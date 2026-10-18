package org.rsmod.content.other.playerbots.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

/**
 * Ground items fighting bots pick up. [BURY] items are buried straight away; the rest are kept.
 * A bot also picks up the ammo it has equipped.
 */
object PlayerbotCombatLootTable {
    const val OBJ = 0
    const val BURY = 1

    fun loot() =
        dbTable("dbtable.playerbot_combat_loot", serverOnly = true) {
            column("obj", OBJ, VarType.OBJ)
            column("bury", BURY, VarType.BOOLEAN)

            fun loot(obj: String, bury: Boolean = false) {
                row("dbrow.playerbot_combat_loot_$obj") {
                    columnRSCM(OBJ, "obj.$obj")
                    column(BURY, bury)
                }
            }
            loot("bones", bury = true)
            loot("coins")
            loot("feather")
        }
}
