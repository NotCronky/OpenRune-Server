package org.rsmod.content.other.playerbots.brain

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.stat.PlayerSkillXP
import org.rsmod.api.player.stat.baseWoodcuttingLvl
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.PlayerbotGatheringSpotsRow
import org.rsmod.api.table.combat.PlayerbotCombatSpotsRow
import org.rsmod.content.other.playerbots.brain.gathering.FishingSkill
import org.rsmod.content.other.playerbots.brain.gathering.GatheringSkill
import org.rsmod.content.other.playerbots.brain.gathering.MiningSkill
import org.rsmod.content.other.playerbots.brain.gathering.WoodcuttingSkill
import org.rsmod.content.other.playerbots.brain.goals.CombatGoal
import org.rsmod.content.other.playerbots.brain.goals.GatheringGoal
import org.rsmod.content.other.playerbots.brain.goals.IdleGoal
import org.rsmod.content.other.playerbots.brain.goals.WanderGoal
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player

/** One [BotBrain] per online bot, thought about once per tick, plus the goal picker. */
@Singleton
class BotBrains
@Inject
constructor(
    private val world: BotWorld,
    private val random: GameRandom,
    private val clock: MapClock,
    private val chat: BotChat,
) {
    private val brains = LinkedHashMap<Player, BotBrain>()

    fun add(player: Player) {
        brains[player] = BotBrain(player, world, random, clock, chat)
    }

    fun remove(player: Player) {
        brains.remove(player)
    }

    operator fun get(player: Player): BotBrain? = brains[player]

    fun tick() {
        for (brain in brains.values) {
            brain.trackPosition()
            brain.refreshFacing()
            brain.think(::pickGoal)
        }
    }

    /** Switches [brain] to the goal named [name] if it's available to it now. */
    fun forceGoal(brain: BotBrain, name: String): BotGoal? {
        val option = options(brain)[name.lowercase()] ?: return null
        return option.create().also(brain::force)
    }

    fun goalNames(): Set<String> = GATHERING.keys.map { it.key }.toSet() + setOf(COMBAT, WANDER, IDLE)

    private fun pickGoal(brain: BotBrain): BotGoal {
        val options = options(brain).values
        var roll = random.of(options.sumOf { it.weight })
        for (option in options) {
            roll -= option.weight
            if (roll < 0) {
                return option.create()
            }
        }
        return options.last().create()
    }

    private fun options(brain: BotBrain): Map<String, GoalOption> = buildMap {
        for ((skill, weight) in GATHERING) {
            val spot = gatheringSpot(brain, skill) ?: continue
            put(skill.key, GoalOption(weight) { GatheringGoal(skill, spot, sessionEnd(brain)) })
        }
        val combat = combatSpot(brain.player)
        if (combat != null) {
            put(COMBAT, GoalOption(COMBAT_WEIGHT) { CombatGoal(combat, sessionEnd(brain)) })
        }
        put(WANDER, GoalOption(WANDER_WEIGHT) { WanderGoal(brain.home, random.of(WANDER_MIN, WANDER_MAX)) })
        put(IDLE, GoalOption(IDLE_WEIGHT) { IdleGoal(brain.now + random.of(IDLE_MIN, IDLE_MAX)) })
    }

    private class GoalOption(val weight: Int, val create: () -> BotGoal)

    private fun sessionEnd(brain: BotBrain): Int = brain.now + random.of(SESSION_MIN, SESSION_MAX)

    /**
     * The nearest [skill] spot the bot is close to and has the gear for: carried, or in the bank
     * when the spot has one.
     */
    private fun gatheringSpot(brain: BotBrain, skill: GatheringSkill): PlayerbotGatheringSpotsRow? {
        val player = brain.player
        val bank = player.invMap[BANK_INV]
        return PlayerbotGatheringSpotsRow.all()
            .filter { it.skill == skill.key }
            .filter { player.coords.chebyshevDistance(it.centre) <= it.radius + GATHERING_SPOT_REACH }
            .filter { spot ->
                skill.equipped(brain, spot) ||
                    (spot.banks && bank != null && skill.nextWithdrawal(brain, spot, bank) != null)
            }
            .minByOrNull { player.coords.chebyshevDistance(it.centre) }
    }

    /**
     * The nearest spot for the bot's combat level, once it has healed. Level-3 skillers (no
     * combat stats, but trained skills) keep their combat level and never fight.
     */
    private fun combatSpot(player: Player): PlayerbotCombatSpotsRow? {
        if (!CombatGoal.readyToFight(player) || isSkiller(player)) {
            return null
        }
        val combat = PlayerSkillXP.calculateCombatLevel(player)
        return PlayerbotCombatSpotsRow.all()
            .filter { combat in it.minCombat..it.maxCombat }
            .filter { player.coords.chebyshevDistance(it.centre) <= it.radius + COMBAT_SPOT_REACH }
            .minByOrNull { player.coords.chebyshevDistance(it.centre) }
    }

    private fun isSkiller(player: Player): Boolean {
        val untrained = COMBAT_STATS.all { player.statBase(it) <= 1 }
        return untrained && player.baseWoodcuttingLvl >= SKILLER_LEVEL
    }

    private companion object {
        private const val COMBAT = "combat"
        private const val WANDER = "wander"
        private const val IDLE = "idle"
        private const val COMBAT_WEIGHT = 50
        private const val WANDER_WEIGHT = 25
        private const val IDLE_WEIGHT = 15
        private const val GATHERING_SPOT_REACH = 60
        private const val BANK_INV = "inv.bank"
        private val GATHERING: Map<GatheringSkill, Int> =
            mapOf(WoodcuttingSkill to 60, MiningSkill to 45, FishingSkill to 45)
        private const val COMBAT_SPOT_REACH = 90
        private const val SKILLER_LEVEL = 10
        private val COMBAT_STATS =
            listOf("stat.attack", "stat.strength", "stat.defence", "stat.ranged", "stat.magic")
        private const val SESSION_MIN = 1_000
        private const val SESSION_MAX = 6_000
        private const val WANDER_MIN = 2
        private const val WANDER_MAX = 6
        private const val IDLE_MIN = 20
        private const val IDLE_MAX = 300
    }
}
