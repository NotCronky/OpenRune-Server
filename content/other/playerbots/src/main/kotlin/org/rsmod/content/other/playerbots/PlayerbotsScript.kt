package org.rsmod.content.other.playerbots

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import net.rsprot.protocol.api.NetworkService
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.queueDeath
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.content.other.playerbots.brain.BotBrains
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PlayerbotsScript
@Inject
constructor(
    private val botLogin: BotLogin,
    private val accounts: BotAccounts,
    private val population: PopulationManager,
    private val brains: BotBrains,
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
            population.tick()
            brains.tick()
        }
        onEvent<GameLifecycle.EndCycle> {
            tickTimes[(tickCount++ % TICK_SAMPLES).toInt()] = System.nanoTime() - tickStart
        }
        onEvent<SessionStateEvent.Login> {
            if (player.isBot) {
                brains.add(player)
            }
        }
        onEvent<SessionStateEvent.Delete> {
            brains.remove(player)
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
            "spawn" -> spawn(args.getOrNull(1)?.toIntOrNull(), args.getOrNull(2))
            "population" -> population(args.getOrNull(1))
            "clear" -> clear()
            "tick" -> tick()
            "status" -> status(name)
            "goal" -> goal(args.getOrNull(1), args.getOrNull(2))
            "inv" -> inv(name)
            "kill" -> kill(name)
            "hp" -> hp(args.getOrNull(1), args.getOrNull(2)?.toIntOrNull())
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
        val target = population.target?.toString() ?: "off"
        player.mes("Bots online: ${online.size} (${known.size} bot accounts, target: $target).")
        player.mesList(online.map { "${it.displayName} ${it.coords}" })
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

    private fun Cheat.spawn(count: Int?, where: String?) {
        val here = where.equals("here", ignoreCase = true)
        if (count == null || count !in 1..MAX_FILL || (where != null && !here)) {
            player.mes("Usage: ::bot spawn <1-$MAX_FILL> [here]")
            return
        }
        population.spawn(count, if (here) player.coords else null)
        player.mes("Rolling $count new bot accounts (${population.pendingSpawns()} queued).")
    }

    private fun Cheat.population(arg: String?) {
        if (arg == null) {
            val target = population.target?.toString() ?: "off"
            player.mes("Bot population target: $target, online: ${onlineBots().size}.")
            return
        }
        if (arg.equals("off", ignoreCase = true)) {
            population.setTarget(null)
            player.mes("Bot population management is off. Online bots stay until removed.")
            return
        }
        val count = arg.toIntOrNull()
        if (count == null || count !in 0..MAX_FILL) {
            player.mes("Usage: ::bot population <0-$MAX_FILL|off>")
            return
        }
        population.setTarget(count)
        player.mes("Bot population target set to $count.")
    }

    private fun Cheat.clear() {
        val bots = onlineBots()
        for (bot in bots) {
            bot.manualLogout = true
        }
        player.mes("Logging out ${bots.size} bots.")
    }

    private fun Cheat.status(name: String) {
        val bot = findOnline(name)
        val brain = bot?.let { brains[it] }
        if (bot == null || brain == null) {
            player.mes("No bot named '$name' is online.")
            return
        }
        val goal = brain.goal?.name ?: "none"
        player.mes(
            "${bot.displayName}: goal $goal at ${bot.coords}, busy=${brain.busy}, " +
                "hp ${bot.hitpoints}/${bot.baseHitpointsLvl}, cb ${bot.combatLevel}, " +
                "last: ${brain.lastEnded ?: "none"}."
        )
    }

    private fun Cheat.goal(name: String?, goal: String?) {
        val bot = name?.let(::findOnline)
        val brain = bot?.let { brains[it] }
        if (bot == null || brain == null || goal == null) {
            player.mes("Usage: ::bot goal <name> <${brains.goalNames().joinToString("|")}>")
            return
        }
        val forced = brains.forceGoal(brain, goal)
        if (forced == null) {
            player.mes("${bot.displayName} can't do '$goal' from where it is now.")
            return
        }
        player.mes("${bot.displayName}: goal set to ${forced.name}.")
    }

    private fun Cheat.inv(name: String) {
        val bot = findOnline(name)
        if (bot == null || !bot.isBot) {
            player.mes("No bot named '$name' is online.")
            return
        }
        player.mes("${bot.displayName} inventory:")
        player.mesList(bot.inv.objs.filterNotNull().map { "${getInvObj(it).name} x${it.count}" })
        player.mes("${bot.displayName} worn:")
        player.mesList(bot.worn.objs.filterNotNull().map { "${getInvObj(it).name} x${it.count}" })
    }

    private fun Cheat.kill(name: String) {
        val bot = findOnline(name)
        if (bot == null || !bot.isBot) {
            player.mes("No bot named '$name' is online.")
            return
        }
        bot.queueDeath()
        player.mes("Killing bot '${bot.displayName}'.")
    }

    private fun Cheat.hp(name: String?, hitpoints: Int?) {
        val bot = name?.let(::findOnline)
        if (bot == null || !bot.isBot || hitpoints == null || hitpoints !in 1..bot.hitpoints) {
            player.mes("Usage: ::bot hp <name> <1-current hp> (lowers a bot's hitpoints)")
            return
        }
        bot.statSub(HITPOINTS, bot.hitpoints - hitpoints, 0)
        player.mes("${bot.displayName}: hp ${bot.hitpoints}/${bot.baseHitpointsLvl}.")
    }

    /** Joins [items] into as few game messages as fit the protocol's length limit. */
    private fun Player.mesList(items: List<String>) {
        if (items.isEmpty()) {
            mes("  (none)")
            return
        }
        var line = StringBuilder()
        for (item in items) {
            if (line.isNotEmpty() && line.length + item.length + 2 > MAX_MESSAGE) {
                mes(line.toString())
                line = StringBuilder()
            }
            if (line.isNotEmpty()) {
                line.append(", ")
            }
            line.append(item)
        }
        mes(line.toString())
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
        private const val USAGE =
            "Usage: ::bot add|remove <name>, ::bot list, ::bot fill <n>, ::bot spawn <n> [here], " +
                "::bot population <n|off>, ::bot status|inv|kill <name>, ::bot goal <name> <goal>, ::bot hp <name> <n>, " +
                "::bot clear, ::bot tick"
        private const val FILL_PREFIX = "bot"
        private const val MAX_FILL = 500
        private const val TICK_SAMPLES = 100
        private const val MAX_MESSAGE = 240
        private const val HITPOINTS = "stat.hitpoints"
        private const val NANOS_PER_MS = 1_000_000L
        private val NAME_PATTERN = Regex("[A-Za-z0-9 _-]{1,12}")
    }
}
