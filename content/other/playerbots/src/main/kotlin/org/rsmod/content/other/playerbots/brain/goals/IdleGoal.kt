package org.rsmod.content.other.playerbots.brain.goals

import org.rsmod.content.other.playerbots.brain.BotBrain
import org.rsmod.content.other.playerbots.brain.BotGoal
import org.rsmod.content.other.playerbots.brain.GoalState

/** Stands around until [until], like a player who tabbed out. */
class IdleGoal(private val until: Int) : BotGoal {
    override fun tick(brain: BotBrain): GoalState =
        if (brain.now >= until) GoalState.Done else GoalState.Running
}
