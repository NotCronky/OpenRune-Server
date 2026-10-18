package org.rsmod.content.other.playerbots

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import net.rsprot.protocol.api.NetworkService
import net.rsprot.protocol.common.client.OldSchoolClientType
import org.rsmod.api.account.AccountManager
import org.rsmod.api.account.loader.request.AccountLoadAuth
import org.rsmod.api.account.loader.request.AccountLoadResponse
import org.rsmod.api.account.loader.request.isNewAccount
import org.rsmod.api.net.central.OpenRuneCentralWorldLink
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.realm.Realm
import org.rsmod.api.registry.account.AccountRegistry
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.player.isSuccess
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.map.CoordGrid

/**
 * Logs a bot in the way `AccountLoadResponseHook` logs in a real player, minus the socket, login
 * block, two-factor, trusted devices and central auth.
 *
 * The load runs on the account loader thread; everything that touches the world runs on the game
 * thread through [AccountRegistry.queueLogin], and failures come back through [processPending].
 */
@Singleton
class BotLogin
@Inject
constructor(
    private val accountManager: AccountManager,
    private val accountRegistry: AccountRegistry,
    private val playerRegistry: PlayerRegistry,
    private val eventBus: EventBus,
    private val realm: Realm,
    private val service: NetworkService<Player>,
    private val regions: RegionRegistry,
    private val input: BotInput,
    private val openRuneCentral: OpenRuneCentralWorldLink,
    private val accounts: BotAccounts,
) {
    private val logger = InlineLogger()

    private val inFlight: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val pendingFailures = ConcurrentLinkedQueue<() -> Unit>()

    /**
     * Bots never log in through a client, so the password only has to be unguessable. One hash per
     * run keeps the cost out of the loader's database transaction, which the game thread waits on.
     */
    private val passwordHash by lazy {
        openRuneCentral.passwordHasher().hash(UUID.randomUUID().toString())
    }

    private var Player.newAccount by boolVarBit("varbit.new_player_account")

    sealed class Result {
        data class Ok(val player: Player) : Result()

        data class Failed(val reason: String) : Result()
    }

    fun isLoggingIn(accountName: String): Boolean = accountName.lowercase() in inFlight

    /** [callback] always runs on the game thread. New accounts start at [spawn]. */
    fun login(accountName: String, spawn: CoordGrid, callback: (Result) -> Unit) {
        val name = accountName.lowercase()
        if (!inFlight.add(name)) {
            callback(Result.Failed("already logging in"))
            return
        }
        val queued =
            accountManager.loadOrCreate(
                AccountLoadAuth.UnknownDevice,
                name,
                { claimNewAccount(name) },
            ) { response ->
                onLoaded(name, spawn, response, callback)
            }
        if (!queued) {
            inFlight.remove(name)
            callback(Result.Failed("account loader rejected the request"))
        }
    }

    fun processPending() {
        while (true) {
            val failure = pendingFailures.poll() ?: break
            failure()
        }
    }

    /** Loader thread. */
    private fun onLoaded(
        name: String,
        spawn: CoordGrid,
        response: AccountLoadResponse,
        callback: (Result) -> Unit,
    ) {
        val failure =
            when {
                response !is AccountLoadResponse.Ok -> "account load failed ($response)"
                response is AccountLoadResponse.Ok.LoadAccount && !accounts.contains(name) ->
                    "'$name' is a real player's account"
                else -> null
            }
        if (failure != null || response !is AccountLoadResponse.Ok) {
            fail(name, failure ?: "account load failed", callback)
            return
        }
        try {
            val player = createPlayer(response, spawn)
            accountRegistry.queueLogin(player, response) { queued, ok ->
                enterWorld(queued, ok, callback)
            }
        } catch (e: Exception) {
            logger.error(e) { "Could not queue bot login: $name" }
            fail(name, "could not queue login (${e.message})", callback)
        }
    }

    private fun createPlayer(response: AccountLoadResponse.Ok, spawn: CoordGrid): Player {
        val player = Player()
        for (transform in response.transforms) {
            transform.apply(player)
        }
        player.newAccount = response.isNewAccount()
        player.worldType = response.worldType
        player.applyConfigTransforms(spawn, response.firstVisitToWorldType)
        return player
    }

    /** Mirrors `AccountLoadResponseHook.applyConfigTransforms`, except bots never get dev rights. */
    private fun Player.applyConfigTransforms(spawn: CoordGrid, firstVisitToWorldType: Boolean) {
        if (displayName.isBlank()) {
            displayName = username.toDisplayName()
        }
        if (!newAccount && !firstVisitToWorldType) {
            val inInstance =
                coords in RegionRegistry.workingAreaSmall || coords in RegionRegistry.workingAreaLarge
            if (inInstance) {
                coords = realm.config.spawnCoord
            }
            return
        }
        coords = spawn
        xpRate = realm.config.baseXpRate
        newAccount = true
    }

    /** Game thread. */
    private fun enterWorld(
        player: Player,
        response: AccountLoadResponse.Ok,
        callback: (Result) -> Unit,
    ) {
        val name = response.account.accountName.lowercase()
        try {
            val result = register(player)
            if (result is Result.Failed) {
                logger.warn { "Bot login failed: $name (${result.reason})" }
            }
            callback(result)
        } catch (e: Exception) {
            logger.error(e) { "Error logging in bot: $player" }
            player.forceDisconnect = true
            callback(Result.Failed("error during login (${e.message})"))
        } finally {
            inFlight.remove(name)
        }
    }

    private fun register(player: Player): Result {
        if (playerRegistry.isOnline(player.userId)) {
            return Result.Failed("already online")
        }
        val slot = playerRegistry.nextFreeSlot() ?: return Result.Failed("world is full")

        eventBus.publish(SessionStateEvent.PrepareLogin(player))
        player.slotId = slot

        val infos = service.infoProtocols.alloc(slot, OldSchoolClientType.DESKTOP)
        val client = BotClient(infos, input)
        val cycle = BotCycle.create(client, regions)
        player.client = client
        player.clientCycle = cycle
        cycle.init(player)

        val register = playerRegistry.add(player)
        if (!register.isSuccess()) {
            service.infoProtocols.dealloc(infos)
            return Result.Failed("could not register player ($register)")
        }
        eventBus.publish(SessionStateEvent.Login(player))
        eventBus.publish(SessionStateEvent.EngineLogin(player))
        return Result.Ok(player)
    }

    private fun fail(name: String, reason: String, callback: (Result) -> Unit) {
        logger.warn { "Bot login failed: $name ($reason)" }
        pendingFailures +=
            {
                inFlight.remove(name)
                callback(Result.Failed(reason))
            }
    }

    /**
     * The loader only asks for a password when it is about to create the account, so the name is
     * claimed here; a load that fails after the row is written still leaves it marked as a bot.
     */
    private fun claimNewAccount(name: String): String {
        accounts.add(name)
        return passwordHash
    }

    private fun String.toDisplayName(): String =
        trim().split(Regex(" +")).joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}
