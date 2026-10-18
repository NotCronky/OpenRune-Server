package org.rsmod.content.other.playerbots

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import net.rsprot.protocol.api.NetworkService
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PlayerbotsScript
@Inject
constructor(
    private val botLogin: BotLogin,
    private val accounts: BotAccounts,
    private val playerList: PlayerList,
    private val service: NetworkService<Player>,
) : PluginScript() {
    private val tickTimes = LongArray(TICK_SAMPLES)
    private var tickCount = 0L
    private var tickStart = 0L

    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.StartCycle> {
            tickStart = System.nanoTime()
            botLogin.processPending()
        }
        onEvent<GameLifecycle.EndCycle> {
            tickTimes[(tickCount++ % TICK_SAMPLES).toInt()] = System.nanoTime() - tickStart
        }
        onEvent<SessionStateEvent.Delete> {
            val client = player.client as? BotClient ?: return@onEvent
            client.unregister(service, player)
        }
        onCommand("bot") {
            desc = "Manage player bots"
            requiredRights = Rights.ADMINISTRATOR
            invalidRights = "You need to be an administrator to manage bots."
            invalidArgs = USAGE
            cheat { handle() }
        }
    }

    private fun Cheat.handle() {
        val name = args.drop(1).joinToString(" ")
        when (args.getOrNull(0)?.lowercase()) {
            "add" -> add(name)
            "remove" -> remove(name)
            "list" -> list()
            "fill" -> fill(args.getOrNull(1)?.toIntOrNull())
            "clear" -> clear()
            "tick" -> tick()
            else -> player.mes(USAGE)
        }
    }

    private fun Cheat.add(name: String) {
        if (!NAME_PATTERN.matches(name)) {
            player.mes("Bot names are 1-12 letters, digits, spaces, '-' or '_'.")
            return
        }
        if (findOnline(name) != null) {
            player.mes("'$name' is already online.")
            return
        }
        val admin = player
        botLogin.login(name, admin.coords) { result ->
            when (result) {
                is BotLogin.Result.Ok -> admin.mes("Bot '${result.player.displayName}' logged in.")
                is BotLogin.Result.Failed -> admin.mes("Bot '$name' did not log in: ${result.reason}.")
            }
        }
    }

    private fun Cheat.remove(name: String) {
        val bot = findOnline(name)
        if (bot == null || !bot.isBot) {
            player.mes("No bot named '$name' is online.")
            return
        }
        bot.manualLogout = true
        player.mes("Logging out bot '${bot.displayName}'.")
    }

    private fun Cheat.list() {
        val online = onlineBots()
        val known = accounts.all()
        player.mes("Bots online: ${online.size} (${known.size} bot accounts).")
        if (online.isNotEmpty()) {
            player.mes(online.joinToString(", ") { "${it.displayName} ${it.coords}" })
        }
    }

    private fun Cheat.fill(count: Int?) {
        if (count == null || count !in 1..MAX_FILL) {
            player.mes("Usage: ::bot fill <1-$MAX_FILL>")
            return
        }
        val online = onlineBots().map { it.username.lowercase() }.toSet()
        val names =
            generateSequence(1) { it + 1 }
                .map { "$FILL_PREFIX$it" }
                .filter { it !in online && !botLogin.isLoggingIn(it) }
                .take((count - online.size).coerceAtLeast(0))
                .toList()
        if (names.isEmpty()) {
            player.mes("Already ${online.size} bots online.")
            return
        }
        val admin = player
        var loggedIn = 0
        val failures = mutableMapOf<String, Int>()
        for (name in names) {
            botLogin.login(name, admin.coords) { result ->
                when (result) {
                    is BotLogin.Result.Ok -> loggedIn++
                    is BotLogin.Result.Failed -> failures.merge(result.reason, 1, Int::plus)
                }
                if (loggedIn + failures.values.sum() == names.size) {
                    admin.mes("Bot fill done: $loggedIn logged in, ${failures.values.sum()} failed.")
                    for ((reason, count) in failures) {
                        admin.mes("  $count x $reason")
                    }
                }
            }
        }
        player.mes("Logging in ${names.size} bots...")
    }

    private fun Cheat.clear() {
        val bots = onlineBots()
        for (bot in bots) {
            bot.manualLogout = true
        }
        player.mes("Logging out ${bots.size} bots.")
    }

    private fun Cheat.tick() {
        val samples = minOf(tickCount, TICK_SAMPLES.toLong()).toInt()
        if (samples == 0) {
            player.mes("No tick samples yet.")
            return
        }
        val recent = tickTimes.take(samples)
        val avgMs = recent.average() / NANOS_PER_MS
        val maxMs = recent.max() / NANOS_PER_MS.toDouble()
        player.mes(
            "Last $samples ticks: avg %.2fms, max %.2fms. Bots online: %d, players: %d."
                .format(avgMs, maxMs, onlineBots().size, playerList.count())
        )
    }

    private fun onlineBots(): List<Player> = playerList.filter { it.isBot }

    private fun findOnline(name: String): Player? =
        playerList.firstOrNull { it.username.equals(name, ignoreCase = true) }

    private companion object {
        private const val USAGE = "Usage: ::bot add|remove <name>, ::bot list, ::bot fill <n>, " +
            "::bot clear, ::bot tick"
        private const val FILL_PREFIX = "bot"
        private const val MAX_FILL = 500
        private const val TICK_SAMPLES = 100
        private const val NANOS_PER_MS = 1_000_000L
        private val NAME_PATTERN = Regex("[A-Za-z0-9 _-]{1,12}")
    }
}
