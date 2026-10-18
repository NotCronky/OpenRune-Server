package org.rsmod.content.areas.misc.tutorialisland.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object TutorialIslandSideTabsTable {

    const val TAB = 0
    const val SIDE_INTERFACE = 1
    const val UNLOCK_STAGE = 2
    const val OPENED_STAGE = 3

    fun sideTabs() =
        dbTable("dbtable.tutorial_island_side_tabs", serverOnly = true) {
            column("tab", TAB, VarType.INT)
            column("side_interface", SIDE_INTERFACE, VarType.STRING)
            column("unlock_stage", UNLOCK_STAGE, VarType.INT)
            column("opened_stage", OPENED_STAGE, VarType.INT)

            tab("combat", 0, "interface.combat_interface", unlock = 430, opened = 440)
            tab("stats", 1, "interface.stats", unlock = 50, opened = 60)
            tab("journal", 2, "interface.side_journal", unlock = 230, opened = 240)
            tab("inventory", 3, "interface.inventory", unlock = 30, opened = 40)
            tab("worn", 4, "interface.wornitems", unlock = 390, opened = 400)
            tab("prayer", 5, "interface.prayerbook", unlock = 560, opened = 570)
            tab("magic", 6, "interface.magic_spellbook", unlock = 630, opened = 640)
            tab("channels", 7, "interface.side_channels", unlock = 1000)
            tab("account", 8, "interface.account", unlock = 531, opened = 532)
            tab("friends", 9, "interface.friends", unlock = 1000)
            tab("settings", 11, "interface.settings_side", unlock = 3, opened = 7)
            tab("emote", 12, "interface.emote", unlock = 1000)
            tab("music", 13, "interface.music", unlock = 1000)
        }

    private fun DBTableBuilder.tab(
        name: String,
        tab: Int,
        interf: String,
        unlock: Int,
        opened: Int? = null,
    ) {
        row("dbrow.tutorial_island_tab_$name") {
            column(TAB, tab)
            column(SIDE_INTERFACE, interf)
            column(UNLOCK_STAGE, unlock)
            if (opened != null) {
                column(OPENED_STAGE, opened)
            }
        }
    }
}
