package org.rsmod.content.areas.misc.tutorialisland.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object TutorialIslandStarterKitTable {

    const val OBJ = 0
    const val COUNT = 1

    fun starterKit() =
        dbTable("dbtable.tutorial_island_starter_kit", serverOnly = true) {
            column("obj", OBJ, VarType.OBJ)
            column("count", COUNT, VarType.INT)

            item(0, "obj.bronze_axe", 1)
            item(1, "obj.bronze_pickaxe", 1)
            item(2, "obj.tinderbox", 1)
            item(3, "obj.net", 1)
            item(4, "obj.shrimp", 1)
            item(5, "obj.bronze_dagger", 1)
            item(6, "obj.bronze_sword", 1)
            item(7, "obj.wooden_shield", 1)
            item(8, "obj.shortbow", 1)
            item(9, "obj.bronze_arrow", 25)
            item(10, "obj.airrune", 25)
            item(11, "obj.mindrune", 15)
            item(12, "obj.bucket_empty", 1)
            item(13, "obj.pot_empty", 1)
            item(14, "obj.bread", 1)
            item(15, "obj.waterrune", 6)
            item(16, "obj.earthrune", 4)
            item(17, "obj.bodyrune", 2)
        }

    private fun DBTableBuilder.item(index: Int, obj: String, count: Int) {
        row("dbrow.tutorial_island_starter_kit_$index") {
            columnRSCM(OBJ, obj)
            column(COUNT, count)
        }
    }
}
