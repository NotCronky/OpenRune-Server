package org.rsmod.content.areas.misc.tutorialisland.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class TutorialIslandPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            TutorialIslandStepsTable.steps(),
            TutorialIslandSideTabsTable.sideTabs(),
            TutorialIslandStarterKitTable.starterKit(),
        )
}
