package org.rsmod.content.other.playerbots.brain

/** Something a bot works on until it is done or gives up. [tick] runs on each think. */
interface BotGoal {
    val name: String
        get() = javaClass.simpleName

    fun tick(brain: BotBrain): GoalState

    /** Whether the goal is using the open modal (e.g. a bank), so the brain must not close it. */
    fun ownsModal(brain: BotBrain): Boolean = false
}

enum class GoalState {
    Running,
    Done,
    Failed,
}
