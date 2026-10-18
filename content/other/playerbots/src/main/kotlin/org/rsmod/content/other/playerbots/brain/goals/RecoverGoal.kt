package org.rsmod.content.other.playerbots.brain.goals

import org.rsmod.content.other.playerbots.brain.BotActions
import org.rsmod.content.other.playerbots.brain.BotBrain
import org.rsmod.content.other.playerbots.brain.BotGear
import org.rsmod.content.other.playerbots.brain.BotGoal
import org.rsmod.content.other.playerbots.brain.GoalState
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid

/**
 * After a death: wields what it kept, walks back to [deathCoords], picks its items up and wields
 * them again. Gives up at [giveUpAt] or when the way back is blocked.
 */
class RecoverGoal(private val deathCoords: CoordGrid, private val giveUpAt: Int) : BotGoal {
    private val tried = mutableSetOf<Int>()
    private val skipped = mutableSetOf<Obj>()
    private var pickup: Obj? = null
    private var pickupAttempts = 0
    private var idleThinks = 0
    private var lastCoords: CoordGrid? = null

    override fun tick(brain: BotBrain): GoalState {
        val player = brain.player
        if (brain.busy) {
            idleThinks = 0
            return GoalState.Running
        }
        if (BotGear.wieldSpare(player, tried)) {
            return GoalState.Running
        }
        if (brain.now >= giveUpAt || stuck(player)) {
            return GoalState.Failed
        }
        if (player.coords.chebyshevDistance(deathCoords) > PILE_REACH) {
            return if (brain.walkToward(deathCoords)) GoalState.Running else GoalState.Failed
        }
        val obj =
            brain.world
                .objsNear(deathCoords, PILE_RADIUS) { it !in skipped && it.isVisibleTo(player) }
                .firstOrNull { canHold(player, it) }
        if (obj == null) {
            tried.clear()
            return if (BotGear.wieldSpare(player, tried)) GoalState.Running else GoalState.Done
        }
        if (obj == pickup) {
            if (++pickupAttempts > MAX_PICKUP_ATTEMPTS) {
                skipped += obj
                pickup = null
                return GoalState.Running
            }
        } else {
            pickup = obj
            pickupAttempts = 0
        }
        BotActions.takeObj(player, obj)
        return GoalState.Running
    }

    private fun canHold(player: Player, obj: Obj): Boolean =
        !player.inv.isFull() || player.inv.objs.any { it?.id == obj.type && getInvObj(it).stackable }

    private fun stuck(player: Player): Boolean {
        idleThinks = if (player.coords == lastCoords) idleThinks + 1 else 0
        lastCoords = player.coords
        return idleThinks > MAX_IDLE_THINKS
    }

    private companion object {
        private const val PILE_REACH = 8
        private const val PILE_RADIUS = 1
        private const val MAX_PICKUP_ATTEMPTS = 3
        private const val MAX_IDLE_THINKS = 40
    }
}
