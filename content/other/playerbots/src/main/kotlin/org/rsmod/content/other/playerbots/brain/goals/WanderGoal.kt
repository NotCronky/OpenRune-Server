package org.rsmod.content.other.playerbots.brain.goals

import org.rsmod.content.other.playerbots.brain.BotActions
import org.rsmod.content.other.playerbots.brain.BotBrain
import org.rsmod.content.other.playerbots.brain.BotGoal
import org.rsmod.content.other.playerbots.brain.GoalState
import org.rsmod.map.CoordGrid

/** Strolls to a few random tiles around [home], pausing between them. */
class WanderGoal(private val home: CoordGrid, private var walksLeft: Int) : BotGoal {
    private var pauseUntil = 0

    override fun tick(brain: BotBrain): GoalState {
        if (brain.busy || brain.now < pauseUntil) {
            return GoalState.Running
        }
        if (walksLeft <= 0) {
            return GoalState.Done
        }
        val dest = brain.world.walkableNear(home, RADIUS, brain.random) ?: return GoalState.Failed
        BotActions.walk(brain.player, dest)
        walksLeft--
        pauseUntil = brain.now + brain.random.of(PAUSE_MIN, PAUSE_MAX)
        return GoalState.Running
    }

    private companion object {
        private const val RADIUS = 10
        private const val PAUSE_MIN = 5
        private const val PAUSE_MAX = 40
    }
}
