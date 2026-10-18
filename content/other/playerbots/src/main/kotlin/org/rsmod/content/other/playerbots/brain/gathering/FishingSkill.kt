package org.rsmod.content.other.playerbots.brain.gathering

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.table.PlayerbotGatheringSpotsRow
import org.rsmod.api.table.fishing.FishingMethodRow
import org.rsmod.api.table.fishing.FishingSpotDefRow
import org.rsmod.api.table.fishing.FishingSpotRow
import org.rsmod.content.other.playerbots.brain.BotActions
import org.rsmod.content.other.playerbots.brain.BotBrain
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj

/**
 * Fishing spots are npcs; each spot type (`dbtable.fishing_spot_def`) offers a method on op1 and
 * op3, and each method names its tool and bait (`dbtable.fishing_method`). A bot uses whichever
 * op it has the gear and level for, preferring the higher-level catch.
 */
object FishingSkill : GatheringSkill {
    override val key: String = "fishing"

    private class SpotOp(val op: Int, val method: FishingMethodRow, val level: Int)

    private val methods by lazy { FishingMethodRow.all().associateBy { it.methodId } }

    /** Spot content group → the ops it offers, each with the lowest level that catches anything. */
    private val spotOps by lazy {
        FishingSpotDefRow.all().associate { def ->
            val ops =
                listOf(1 to def.op1, 3 to def.op3).mapNotNull { (op, methodId) ->
                    val method = methods[methodId] ?: return@mapNotNull null
                    val level =
                        FishingSpotRow.all()
                            .filter { it.spot == def.spotId && it.method == methodId }
                            .minOfOrNull { it.level } ?: return@mapNotNull null
                    SpotOp(op, method, level)
                }
            def.content.asRSCM(RSCMType.CONTENT) to ops
        }
    }

    private val supplyNames by lazy {
        spotOps.values.flatten().flatMap { listOfNotNull(it.method.tool, it.method.altTool, it.method.bait) }.toSet()
    }

    override fun targets(brain: BotBrain, spot: PlayerbotGatheringSpotsRow): List<GatherTarget> {
        val player = brain.player
        return brain.world
            .npcsNear(spot.centre, spot.radius) { npc -> npc.visType.contentGroup in spotOps }
            .mapNotNull { npc ->
                val best =
                    spotOps.getValue(npc.visType.contentGroup)
                        .filter { canUse(player, it) }
                        .maxByOrNull { it.level } ?: return@mapNotNull null
                GatherTarget(npc.slotId, npc.coords, best.level) {
                    BotActions.opNpc(it, npc, best.op)
                }
            }
    }

    override fun equipped(brain: BotBrain, spot: PlayerbotGatheringSpotsRow): Boolean =
        opsAt(brain, spot).any { canUse(brain.player, it) }

    override fun isSupply(player: Player, obj: InvObj): Boolean =
        getInvObj(obj).internalName in supplyNames

    override fun nextWithdrawal(
        brain: BotBrain,
        spot: PlayerbotGatheringSpotsRow,
        bank: Inventory,
    ): Withdrawal? {
        val player = brain.player
        if (equipped(brain, spot)) {
            return null
        }
        val wanted =
            opsAt(brain, spot)
                .filter { it.level <= player.fishingLvl }
                .sortedByDescending { it.level }
                .firstOrNull { op -> hasOrBanked(player, bank, op.method) } ?: return null
        val method = wanted.method
        if (!carries(player, method.tool, method.altTool)) {
            return bankSlot(bank, method.tool, method.altTool)?.let { Withdrawal(it, all = false) }
        }
        return method.bait?.let { bait -> bankSlot(bank, bait)?.let { Withdrawal(it, all = true) } }
    }

    /** Every op offered by a fishing spot npc at [spot]. */
    private fun opsAt(brain: BotBrain, spot: PlayerbotGatheringSpotsRow): List<SpotOp> =
        brain.world
            .npcsNear(spot.centre, spot.radius) { it.visType.contentGroup in spotOps }
            .map { it.visType.contentGroup }
            .distinct()
            .flatMap { spotOps.getValue(it) }

    private fun canUse(player: Player, op: SpotOp): Boolean =
        op.level <= player.fishingLvl &&
            carries(player, op.method.tool, op.method.altTool) &&
            (op.method.bait == null || carries(player, op.method.bait))

    private fun hasOrBanked(player: Player, bank: Inventory, method: FishingMethodRow): Boolean {
        val tool =
            carries(player, method.tool, method.altTool) ||
                bankSlot(bank, method.tool, method.altTool) != null
        val bait = method.bait?.let { carries(player, it) || bankSlot(bank, it) != null } ?: true
        return tool && bait
    }

    private fun carries(player: Player, vararg names: String?): Boolean {
        val wanted = names.filterNotNull()
        if (wanted.isEmpty()) {
            return true
        }
        return sequenceOf(player.inv, player.worn).any { inv ->
            inv.objs.any { it != null && getInvObj(it).internalName in wanted }
        }
    }

    private fun bankSlot(bank: Inventory, vararg names: String?): Int? {
        val wanted = names.filterNotNull()
        return bank.indices.firstOrNull { slot ->
            bank[slot]?.let { getInvObj(it).internalName in wanted } == true
        }
    }
}
