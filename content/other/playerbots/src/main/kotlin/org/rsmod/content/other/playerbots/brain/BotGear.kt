package org.rsmod.content.other.playerbots.brain

import org.rsmod.content.other.playerbots.brain.gathering.WoodcuttingSkill.isAxe
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getInvObj

internal object BotGear {
    /**
     * Wields one carried item that fits a free worn slot, or tops up the worn ammo stack. Axes go
     * last so a carried weapon wins the weapon slot. Returns false when there is nothing to wield;
     * every obj tried goes into [tried] so one the bot can't wear isn't clicked forever.
     */
    fun wieldSpare(player: Player, tried: MutableSet<Int>): Boolean {
        val slot =
            player.inv.indices
                .filter { slot -> player.inv[slot]?.let { it.id !in tried && fits(player, it) } == true }
                .minByOrNull { slot -> if (isAxe(checkNotNull(player.inv[slot]))) 1 else 0 }
                ?: return false
        val obj = checkNotNull(player.inv[slot])
        tried += obj.id
        return BotActions.heldOp(player, slot, WIELD) || BotActions.heldOp(player, slot, WEAR)
    }

    private fun fits(player: Player, obj: InvObj): Boolean {
        val type = getInvObj(obj)
        if (!type.isEquipable) {
            return false
        }
        val worn = player.worn[type.wearpos1]
        if (worn != null) {
            return worn.id == obj.id && type.stackable
        }
        val blocked =
            player.worn.objs.filterNotNull().flatMap { listOf(getInvObj(it).wearpos2, getInvObj(it).wearpos3) }
        if (type.wearpos1 in blocked) {
            return false
        }
        return listOf(type.wearpos2, type.wearpos3).none { it >= 0 && player.worn[it] != null }
    }

    private const val WIELD = "Wield"
    private const val WEAR = "Wear"
}
