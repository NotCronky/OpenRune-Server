package org.rsmod.content.other.playerbots.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

/** Where new bot accounts start, weighted towards the busy banks real players log out at. */
object PlayerbotSpawnsTable {
    const val COORDS = 0
    const val WEIGHT = 1

    fun spawns() =
        dbTable("dbtable.playerbot_spawns", serverOnly = true) {
            column("coords", COORDS, VarType.COORDGRID)
            column("weight", WEIGHT, VarType.INT)

            fun spawn(key: String, coords: CoordGrid, weight: Int) {
                row("dbrow.playerbot_spawn_$key") {
                    columnCoord(COORDS, coords)
                    column(WEIGHT, weight)
                }
            }

            spawn("lumbridge_castle", CoordGrid(3222, 3218, 0), 10)
            spawn("varrock_west_bank", CoordGrid(3185, 3436, 0), 12)
            spawn("varrock_east_bank", CoordGrid(3253, 3420, 0), 10)
            spawn("grand_exchange", CoordGrid(3164, 3485, 0), 16)
            spawn("edgeville_bank", CoordGrid(3094, 3491, 0), 12)
            spawn("falador_east_bank", CoordGrid(3013, 3355, 0), 8)
            spawn("falador_west_bank", CoordGrid(2946, 3368, 0), 6)
            spawn("draynor_bank", CoordGrid(3093, 3244, 0), 8)
            spawn("al_kharid_bank", CoordGrid(3269, 3167, 0), 8)
        }
}
