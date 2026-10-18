package org.rsmod.content.other.playerbots.brain.gathering

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.table.PlayerbotGatheringSpotsRow
import org.rsmod.content.other.playerbots.brain.BotActions
import org.rsmod.content.other.playerbots.brain.BotBrain
import org.rsmod.content.skills.mining.configs.MiningRocks
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeLevelReq
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj

object MiningSkill : GatheringSkill {
    override val key: String = "mining"

    private const val MINE_OP = 1
    private val ROCK by lazy { "content.rock".asRSCM(RSCMType.CONTENT) }

    override fun targets(brain: BotBrain, spot: PlayerbotGatheringSpotsRow): List<GatherTarget> {
        val level = brain.player.miningLvl
        return brain.world
            .locsNear(spot.centre, spot.radius) { _, type ->
                type.contentGroup == ROCK &&
                    (MiningRocks.forLoc(type)?.level ?: Int.MAX_VALUE) <= level
            }
            .map { loc ->
                val tier = MiningRocks.forLoc(loc.id)?.level ?: 0
                GatherTarget(loc.coords, loc.coords, tier) { BotActions.opLoc(it, loc, MINE_OP) }
            }
    }

    override fun equipped(brain: BotBrain, spot: PlayerbotGatheringSpotsRow): Boolean =
        Mining.findPickaxe(brain.player) != null

    override fun isSupply(player: Player, obj: InvObj): Boolean = isPickaxe(obj)

    override fun nextWithdrawal(
        brain: BotBrain,
        spot: PlayerbotGatheringSpotsRow,
        bank: Inventory,
    ): Withdrawal? {
        val player = brain.player
        if (Mining.findPickaxe(player) != null) {
            return null
        }
        return bank.indices
            .filter { slot -> bank[slot]?.let { usable(player, it) } == true }
            .maxByOrNull { slot -> getInvObj(checkNotNull(bank[slot])).pickaxeLevelReq }
            ?.let { Withdrawal(it, all = false) }
    }

    fun isPickaxe(obj: InvObj): Boolean = getInvObj(obj).isContentType("content.mining_pickaxe")

    private fun usable(player: Player, obj: InvObj): Boolean =
        isPickaxe(obj) && getInvObj(obj).pickaxeLevelReq <= player.miningLvl
}
