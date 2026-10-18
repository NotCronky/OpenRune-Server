package org.rsmod.content.other.playerbots.brain.goals

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.table.PlayerbotGatheringSpotsRow
import org.rsmod.content.other.playerbots.brain.BotActions
import org.rsmod.content.other.playerbots.brain.BotBrain
import org.rsmod.content.other.playerbots.brain.BotGoal
import org.rsmod.content.other.playerbots.brain.GoalState
import org.rsmod.content.other.playerbots.brain.gathering.GatherTarget
import org.rsmod.content.other.playerbots.brain.gathering.GatheringSkill
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * Works [skill]'s resources around [spot] until [endsAt], then finishes the current load. Full
 * inventories are banked at the nearest booth, or dropped where the spot has no bank. A bot that's
 * missing its tool or bait fetches it from the bank. Anything the bot didn't carry when the goal
 * started (other than tools and bait) counts as gathered: that's what gets dropped.
 */
class GatheringGoal(
    private val skill: GatheringSkill,
    private val spot: PlayerbotGatheringSpotsRow,
    private val endsAt: Int,
) : BotGoal {
    private val badTargets = mutableSetOf<Any>()
    private var carriedAtStart: Set<Int>? = null
    private var target: Any? = null
    private var targetAttempts = 0
    private var progressAtAttempt = 0
    private var bankAttempts = 0
    private var idleThinks = 0
    private var lastCoords: CoordGrid? = null
    private var dropping = false

    override val name: String
        get() = "${skill.key} at ${spot.key}"

    override fun ownsModal(brain: BotBrain): Boolean = brain.player.ui.containsModal(BANK_MAIN)

    override fun tick(brain: BotBrain): GoalState {
        val player = brain.player
        if (carriedAtStart == null) {
            carriedAtStart = player.inv.objs.mapNotNull { it?.id }.toSet()
        }
        if (ownsModal(brain)) {
            return useBank(brain)
        }
        if (brain.busy) {
            idleThinks = 0
            return GoalState.Running
        }
        if (stuck(player)) {
            return GoalState.Failed
        }
        if (dropping) {
            return dropGathered(brain)
        }
        val equipped = skill.equipped(brain, spot)
        if (player.inv.isFull() || !equipped) {
            return when {
                spot.banks -> openBank(brain)
                equipped -> dropGathered(brain)
                else -> GoalState.Failed
            }
        }
        if (brain.now >= endsAt) {
            return GoalState.Done
        }
        return gather(brain)
    }

    private fun gather(brain: BotBrain): GoalState {
        val player = brain.player
        if (player.coords.chebyshevDistance(spot.centre) > spot.radius) {
            return if (brain.walkToward(spot.centre)) GoalState.Running else GoalState.Failed
        }
        val targets = skill.targets(brain, spot).filter { it.key !in badTargets }
        if (targets.isEmpty()) {
            return GoalState.Failed
        }
        val picked = pickTarget(brain, targets)
        val progress = gatheredCount(player)
        if (picked.key == target && progress == progressAtAttempt) {
            if (++targetAttempts > MAX_TARGET_ATTEMPTS) {
                badTargets += picked.key
                target = null
                return GoalState.Running
            }
        } else {
            target = picked.key
            targetAttempts = 0
        }
        progressAtAttempt = progress
        picked.act(player)
        return GoalState.Running
    }

    /**
     * Usually one of the highest-tier targets the bot can work, now and then any; one of the few
     * nearest, so bots at the same spot spread out instead of sharing one.
     */
    private fun pickTarget(brain: BotBrain, targets: List<GatherTarget>): GatherTarget {
        targets.firstOrNull { it.key == target }?.let { return it }
        val bestTier = targets.maxOf { it.tier }
        val pool =
            if (brain.random.of(100) < BEST_TIER_PERCENT) targets.filter { it.tier == bestTier }
            else targets
        val nearest = pool.sortedBy { it.coords.chebyshevDistance(brain.player.coords) }
        return brain.random.pick(nearest.take(NEAREST_TARGETS))
    }

    private fun openBank(brain: BotBrain): GoalState {
        val player = brain.player
        if (++bankAttempts > MAX_BANK_ATTEMPTS) {
            return GoalState.Failed
        }
        val booth =
            brain.world
                .locsNear(spot.centre, spot.radius) { _, type -> type.contentGroup == BANK_BOOTH }
                .minByOrNull { it.coords.chebyshevDistance(player.coords) }
                ?: return GoalState.Failed
        if (player.coords.chebyshevDistance(booth.coords) > BOOTH_RANGE) {
            return if (brain.walkToward(booth.coords)) GoalState.Running else GoalState.Failed
        }
        BotActions.opLoc(player, booth, BANK_OP)
        return GoalState.Running
    }

    private fun useBank(brain: BotBrain): GoalState {
        val player = brain.player
        bankAttempts = 0
        if (player.inv.objs.any { it != null && !skill.isSupply(player, it) }) {
            BotActions.button(player, DEPOSIT_INVENTORY)
            return GoalState.Running
        }
        val bank = player.invMap[BANK_INV]
        val withdrawal = bank?.let { skill.nextWithdrawal(brain, spot, it) }
        if (withdrawal != null) {
            val obj = checkNotNull(bank[withdrawal.slot])
            val op = if (withdrawal.all) WITHDRAW_ALL_OP else WITHDRAW_OP
            BotActions.button(player, BANK_ITEMS, sub = withdrawal.slot, obj = obj.id, op = op)
            return GoalState.Running
        }
        BotActions.closeModal(player)
        return if (skill.equipped(brain, spot)) GoalState.Running else GoalState.Failed
    }

    private fun dropGathered(brain: BotBrain): GoalState {
        val player = brain.player
        val slot = player.inv.indices.firstOrNull { isGathered(player, it) }
        if (slot == null) {
            val wasDropping = dropping
            dropping = false
            return if (wasDropping) GoalState.Running else GoalState.Failed
        }
        dropping = true
        BotActions.heldOp(player, slot, DROP)
        return GoalState.Running
    }

    private fun isGathered(player: Player, slot: Int): Boolean {
        val obj = player.inv[slot] ?: return false
        return obj.id !in carriedAtStart.orEmpty() && !skill.isSupply(player, obj)
    }

    private fun gatheredCount(player: Player): Int =
        player.inv.indices.count { isGathered(player, it) }

    private fun stuck(player: Player): Boolean {
        idleThinks = if (player.coords == lastCoords) idleThinks + 1 else 0
        lastCoords = player.coords
        return idleThinks > MAX_IDLE_THINKS
    }

    private companion object {
        private const val BANK_OP = 2
        private const val WITHDRAW_OP = 1
        private const val WITHDRAW_ALL_OP = 7
        private const val DROP = "Drop"
        private const val BOOTH_RANGE = 10
        private const val BEST_TIER_PERCENT = 80
        private const val NEAREST_TARGETS = 4
        private const val MAX_TARGET_ATTEMPTS = 4
        private const val MAX_BANK_ATTEMPTS = 15
        private const val MAX_IDLE_THINKS = 40

        private const val BANK_MAIN = "interface.bankmain"
        private const val BANK_INV = "inv.bank"
        private const val BANK_ITEMS = "component.bankmain:items"
        private const val DEPOSIT_INVENTORY = "component.bankmain:depositinv"

        private val BANK_BOOTH by lazy { "content.bank_booth".asRSCM(RSCMType.CONTENT) }
    }
}
