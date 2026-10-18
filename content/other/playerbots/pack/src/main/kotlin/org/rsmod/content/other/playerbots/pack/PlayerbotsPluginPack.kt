package org.rsmod.content.other.playerbots.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class PlayerbotsPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            PlayerbotKitTable.kit(),
            PlayerbotSpawnsTable.spawns(),
            PlayerbotGatheringSpotsTable.spots(),
            PlayerbotCombatSpotsTable.spots(),
            PlayerbotCombatLootTable.loot(),
        )
}
