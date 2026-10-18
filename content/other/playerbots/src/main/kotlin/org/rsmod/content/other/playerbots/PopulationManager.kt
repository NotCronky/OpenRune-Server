package org.rsmod.content.other.playerbots

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.random.GameRandom
import org.rsmod.content.other.playerbots.spawn.BotSpawner
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid

/**
 * Keeps the number of online bots near [target] without login bursts: at most [LOGINS_PER_TICK]
 * logins start per tick and [MAX_IN_FLIGHT] can be loading at once. Offline bot accounts are
 * logged back in before new ones are rolled, and every bot it logs in gets a session length after
 * which it logs out and is replaced, so the online set keeps changing.
 */
@Singleton
class PopulationManager
@Inject
constructor(
    private val botLogin: BotLogin,
    private val accounts: BotAccounts,
    private val spawner: BotSpawner,
    private val playerList: PlayerList,
    private val mapClock: MapClock,
    private val random: GameRandom,
) {
    private val logger = InlineLogger()

    var target: Int? = null
        private set

    /** New accounts to roll, each with the tile to start on (null: a random spawn). */
    private val pendingNew = ArrayDeque<CoordGrid?>()
    private var inFlight = 0
    private val sessionEnds = mutableMapOf<String, Int>()

    fun setTarget(count: Int?) {
        target = count
    }

    fun spawn(count: Int, at: CoordGrid? = null) {
        repeat(count) { pendingNew.addLast(at) }
    }

    fun pendingSpawns(): Int = pendingNew.size

    fun tick() {
        val online = onlineBots()
        val wanted = target
        if (wanted != null) {
            expireSessions(online)
            logOutExtra(online, wanted)
        }
        repeat(LOGINS_PER_TICK) { startLogin(online.size, wanted) }
    }

    private fun startLogin(online: Int, wanted: Int?) {
        if (inFlight >= MAX_IN_FLIGHT) {
            return
        }
        if (pendingNew.isNotEmpty()) {
            loginNew(pendingNew.removeFirst())
            return
        }
        if (wanted == null || online + inFlight >= wanted) {
            return
        }
        val offline = offlineAccount()
        if (offline != null) {
            loginExisting(offline)
        } else {
            loginNew()
        }
    }

    private fun loginNew(at: CoordGrid? = null) {
        val name = spawner.name()
        inFlight++
        botLogin.login(name, at ?: spawner.spawnCoord(), spawner.profile()) { result ->
            inFlight--
            when (result) {
                is BotLogin.Result.Ok -> startSession(result.player)
                is BotLogin.Result.Failed ->
                    if (result.reason == BotLogin.NAME_TAKEN) {
                        pendingNew.addFirst(at)
                    } else {
                        logger.warn { "New bot '$name' did not log in: ${result.reason}" }
                    }
            }
        }
    }

    private fun loginExisting(name: String) {
        inFlight++
        botLogin.login(name, spawner.spawnCoord()) { result ->
            inFlight--
            when (result) {
                is BotLogin.Result.Ok -> startSession(result.player)
                is BotLogin.Result.Failed ->
                    logger.warn { "Bot '$name' did not log back in: ${result.reason}" }
            }
        }
    }

    private fun startSession(player: Player) {
        sessionEnds[player.username.lowercase()] =
            mapClock.cycle + random.of(MIN_SESSION_TICKS, MAX_SESSION_TICKS)
    }

    private fun expireSessions(online: List<Player>) {
        var loggedOut = 0
        for (bot in online) {
            if (loggedOut >= LOGOUTS_PER_TICK) {
                return
            }
            val name = bot.username.lowercase()
            val end = sessionEnds[name] ?: continue
            if (mapClock.cycle >= end) {
                sessionEnds -= name
                bot.manualLogout = true
                loggedOut++
            }
        }
    }

    private fun logOutExtra(online: List<Player>, wanted: Int) {
        val staying = online.filterNot { it.manualLogout }
        val extra = (staying.size - wanted).coerceAtMost(LOGOUTS_PER_TICK)
        if (extra <= 0) {
            return
        }
        val leaving = staying.sortedBy { sessionEnds[it.username.lowercase()] ?: 0 }.take(extra)
        for (bot in leaving) {
            sessionEnds -= bot.username.lowercase()
            bot.manualLogout = true
        }
    }

    private fun offlineAccount(): String? {
        val online = playerList.mapTo(mutableSetOf()) { it.username.lowercase() }
        val offline = accounts.all().filter { it !in online && !botLogin.isLoggingIn(it) }
        return random.pickOrNull(offline)
    }

    private fun onlineBots(): List<Player> = playerList.filter { it.isBot }

    private companion object {
        private const val LOGINS_PER_TICK = 1
        private const val LOGOUTS_PER_TICK = 2
        private const val MAX_IN_FLIGHT = 4
        private const val MIN_SESSION_TICKS = 3_000
        private const val MAX_SESSION_TICKS = 18_000
    }
}
