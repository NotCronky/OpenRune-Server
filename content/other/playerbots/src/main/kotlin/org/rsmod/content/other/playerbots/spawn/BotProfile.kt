package org.rsmod.content.other.playerbots.spawn

import org.rsmod.api.player.stat.PlayerSkillXP
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.game.type.getInvObj

/** What a newly created bot account starts with. Applied once, before its login events. */
class BotProfile(
    val archetype: BotArchetype,
    val levels: Map<String, Int>,
    val worn: List<InvObj>,
    val inv: List<InvObj>,
    val bank: List<InvObj>,
) {
    fun apply(player: Player) {
        for ((stat, level) in levels) {
            player.statMap.setFineXP(stat, PlayerSkillXPTable.getFineXPFromLevel(level))
            player.statMap.setBaseLevel(stat, level.toByte())
            player.statMap.setCurrentLevel(stat, level.toByte())
        }
        player.appearance.combatLevel = PlayerSkillXP.calculateCombatLevel(player)

        for (obj in worn) {
            player.worn[getInvObj(obj).wearpos1] = obj
        }
        inv.forEachIndexed { slot, obj -> player.inv[slot] = obj }
        val bankInv = player.invMap.getOrPut(BANK_INV)
        bank.forEachIndexed { slot, obj -> bankInv[slot] = obj }
        repairBankTabs(player)
    }

    companion object {
        private const val BANK_INV = "inv.bank"
        private const val MAIN_TAB = "varbit.bank_tab_main"
        private val NUMBERED_TABS = (1..9).map { "varbit.bank_tab_$it" }

        /**
         * The bank keeps each tab's size in a varbit and assumes it covers every occupied slot.
         * Items put straight into the bank inventory need the main tab grown to match.
         */
        fun repairBankTabs(player: Player) {
            val bank = player.invMap[BANK_INV] ?: return
            val tabbed = NUMBERED_TABS.sumOf { player.vars[it] }
            val main = (bank.lastOccupiedSlot() - tabbed).coerceAtLeast(0)
            if (player.vars[MAIN_TAB] < main) {
                VarPlayerIntMapSetter.set(player, MAIN_TAB, main)
            }
        }
    }
}
