package org.rsmod.content.other.playerbots.brain.gathering

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.table.PlayerbotGatheringSpotsRow
import org.rsmod.content.other.playerbots.brain.BotActions
import org.rsmod.content.other.playerbots.brain.BotBrain
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingReq
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLevelReq
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj

object WoodcuttingSkill : GatheringSkill {
    override val key: String = "woodcutting"

    private const val CHOP_OP = 1
    private val TREE by lazy { "content.tree".asRSCM(RSCMType.CONTENT) }

    override fun targets(brain: BotBrain, spot: PlayerbotGatheringSpotsRow): List<GatherTarget> {
        val level = brain.player.woodcuttingLvl
        return brain.world
            .locsNear(spot.centre, spot.radius) { _, type ->
                type.contentGroup == TREE && type.treeLevelReq <= level
            }
            .map { loc ->
                val tier = brain.world.locType(loc)?.treeLevelReq ?: 0
                GatherTarget(loc.coords, loc.coords, tier) { BotActions.opLoc(it, loc, CHOP_OP) }
            }
    }

    override fun equipped(brain: BotBrain, spot: PlayerbotGatheringSpotsRow): Boolean =
        hasUsableAxe(brain.player)

    override fun isSupply(player: Player, obj: InvObj): Boolean = isAxe(obj)

    override fun nextWithdrawal(
        brain: BotBrain,
        spot: PlayerbotGatheringSpotsRow,
        bank: Inventory,
    ): Withdrawal? {
        val player = brain.player
        if (hasUsableAxe(player)) {
            return null
        }
        return bestAxeSlot(player, bank)?.let { Withdrawal(it, all = false) }
    }

    fun isAxe(obj: InvObj): Boolean = getInvObj(obj).isContentType("content.woodcutting_axe")

    fun hasUsableAxe(player: Player): Boolean =
        sequenceOf(player.inv, player.worn).any { inv ->
            inv.objs.any { it != null && usable(player, it) }
        }

    private fun bestAxeSlot(player: Player, bank: Inventory): Int? =
        bank.indices
            .filter { slot -> bank[slot]?.let { usable(player, it) } == true }
            .maxByOrNull { slot -> getInvObj(checkNotNull(bank[slot])).axeWoodcuttingReq }

    private fun usable(player: Player, obj: InvObj): Boolean =
        isAxe(obj) && getInvObj(obj).axeWoodcuttingReq <= player.woodcuttingLvl
}
