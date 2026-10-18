package org.rsmod.content.other.playerbots.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

/**
 * Places bots train combat. Targets are any attackable npc named [NPC] within [RADIUS] tiles of
 * [CENTRE]; a bot only picks a spot while its combat level is in [MIN_COMBAT]..[MAX_COMBAT].
 */
object PlayerbotCombatSpotsTable {
    const val KEY = 0
    const val CENTRE = 1
    const val RADIUS = 2
    const val NPC = 3
    const val MIN_COMBAT = 4
    const val MAX_COMBAT = 5

    fun spots() =
        dbTable("dbtable.playerbot_combat_spots", serverOnly = true) {
            column("key", KEY, VarType.STRING)
            column("centre", CENTRE, VarType.COORDGRID)
            column("radius", RADIUS, VarType.INT)
            column("npc", NPC, VarType.STRING)
            column("min_combat", MIN_COMBAT, VarType.INT)
            column("max_combat", MAX_COMBAT, VarType.INT)

            fun spot(
                key: String,
                centre: CoordGrid,
                radius: Int,
                npc: String,
                minCombat: Int,
                maxCombat: Int,
            ) {
                row("dbrow.playerbot_combat_$key") {
                    column(KEY, key)
                    columnCoord(CENTRE, centre)
                    column(RADIUS, radius)
                    column(NPC, npc)
                    column(MIN_COMBAT, minCombat)
                    column(MAX_COMBAT, maxCombat)
                }
            }
            spot("lumbridge_fred_chickens", CoordGrid(3177, 3297, 0), 10, "Chicken", 3, 20)
            spot("lumbridge_east_chickens", CoordGrid(3229, 3298, 0), 6, "Chicken", 3, 20)
            spot("lumbridge_east_cows", CoordGrid(3255, 3285, 0), 14, "Cow", 5, 35)
            spot("lumbridge_east_goblins", CoordGrid(3252, 3238, 0), 16, "Goblin", 8, 45)
        }
}
