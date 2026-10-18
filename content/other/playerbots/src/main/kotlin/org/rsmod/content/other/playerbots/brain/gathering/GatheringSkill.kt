package org.rsmod.content.other.playerbots.brain.gathering

import org.rsmod.api.table.PlayerbotGatheringSpotsRow
import org.rsmod.content.other.playerbots.brain.BotBrain
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid

/** What [org.rsmod.content.other.playerbots.brain.goals.GatheringGoal] needs to know per skill. */
interface GatheringSkill {
    /** Matches `skill` in `dbtable.playerbot_gathering_spots`, and names the goal. */
    val key: String

    /** Resources the bot can work at [spot] right now. */
    fun targets(brain: BotBrain, spot: PlayerbotGatheringSpotsRow): List<GatherTarget>

    /** Carries everything it needs to gather at [spot] (tool, and bait where the method uses it). */
    fun equipped(brain: BotBrain, spot: PlayerbotGatheringSpotsRow): Boolean

    /** Tools and bait: kept through banking and dropping. */
    fun isSupply(player: Player, obj: InvObj): Boolean

    /**
     * The next thing to take out of [bank] before gathering at [spot], or null when nothing is
     * missing or the bank doesn't have it.
     */
    fun nextWithdrawal(
        brain: BotBrain,
        spot: PlayerbotGatheringSpotsRow,
        bank: Inventory,
    ): Withdrawal?
}

/** A loc or npc the bot can work; [key] identifies it across thinks, [tier] ranks it. */
class GatherTarget(
    val key: Any,
    val coords: CoordGrid,
    val tier: Int,
    val act: (Player) -> Unit,
)

class Withdrawal(val slot: Int, val all: Boolean)
