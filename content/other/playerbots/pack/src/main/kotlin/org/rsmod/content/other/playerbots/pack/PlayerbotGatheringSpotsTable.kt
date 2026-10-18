package org.rsmod.content.other.playerbots.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

/**
 * Places bots gather resources ([SKILL]: woodcutting, mining or fishing). Resources and bank booths
 * are found by scanning [RADIUS] tiles around [CENTRE]; a spot without a ground-floor bank in
 * range ([BANKS] false) drops what it gathers instead.
 */
object PlayerbotGatheringSpotsTable {
    const val KEY = 0
    const val CENTRE = 1
    const val RADIUS = 2
    const val BANKS = 3
    const val SKILL = 4

    fun spots() =
        dbTable("dbtable.playerbot_gathering_spots", serverOnly = true) {
            column("key", KEY, VarType.STRING)
            column("centre", CENTRE, VarType.COORDGRID)
            column("radius", RADIUS, VarType.INT)
            column("banks", BANKS, VarType.BOOLEAN)
            column("skill", SKILL, VarType.STRING)

            fun spot(skill: String, key: String, centre: CoordGrid, radius: Int, banks: Boolean) {
                row("dbrow.playerbot_${skill}_$key") {
                    column(KEY, key)
                    columnCoord(CENTRE, centre)
                    column(RADIUS, radius)
                    column(BANKS, banks)
                    column(SKILL, skill)
                }
            }
            spot("woodcutting", "draynor", CoordGrid(3093, 3244, 0), 30, banks = true)
            spot("woodcutting", "varrock_west", CoordGrid(3185, 3436, 0), 30, banks = true)
            spot("woodcutting", "edgeville", CoordGrid(3094, 3491, 0), 30, banks = true)
            spot("woodcutting", "lumbridge", CoordGrid(3222, 3218, 0), 25, banks = false)

            spot("mining", "varrock_east", CoordGrid(3270, 3393, 0), 36, banks = true)
            spot("mining", "varrock_west", CoordGrid(3180, 3371, 0), 10, banks = false)
            spot("mining", "lumbridge_swamp", CoordGrid(3228, 3147, 0), 8, banks = false)
            spot("mining", "rimmington", CoordGrid(2977, 3239, 0), 10, banks = false)

            spot("fishing", "draynor", CoordGrid(3090, 3236, 0), 14, banks = true)
            spot("fishing", "lumbridge_river", CoordGrid(3239, 3247, 0), 8, banks = false)
            spot("fishing", "lumbridge_swamp", CoordGrid(3243, 3150, 0), 8, banks = false)
            spot("fishing", "barbarian_village", CoordGrid(3107, 3429, 0), 8, banks = false)
            spot("fishing", "musa_point", CoordGrid(2924, 3180, 0), 6, banks = false)
        }
}
