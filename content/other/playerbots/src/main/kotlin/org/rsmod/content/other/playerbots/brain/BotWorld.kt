package org.rsmod.content.other.playerbots.brain

import dev.openrune.ServerCacheManager
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.obj.Obj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap

/** Read-only world queries for bot decisions. */
@Singleton
class BotWorld
@Inject
constructor(
    private val locs: LocRegistry,
    private val npcs: NpcRegistry,
    private val objs: ObjRegistry,
    private val collision: CollisionFlagMap,
) {
    fun locsNear(
        centre: CoordGrid,
        radius: Int,
        predicate: (LocInfo, ObjectServerType) -> Boolean,
    ): List<LocInfo> =
        inZones(centre, radius) { zone ->
            locs.findAll(zone).filter { loc ->
                loc.coords.chebyshevDistance(centre) <= radius &&
                    ServerCacheManager.getObject(loc.id)?.let { predicate(loc, it) } == true
            }
        }

    fun npcsNear(centre: CoordGrid, radius: Int, predicate: (Npc) -> Boolean): List<Npc> =
        inZones(centre, radius) { zone ->
            npcs.findAll(zone).filter { it.coords.chebyshevDistance(centre) <= radius && predicate(it) }
        }

    fun objsNear(centre: CoordGrid, radius: Int, predicate: (Obj) -> Boolean): List<Obj> =
        inZones(centre, radius) { zone ->
            objs.findAll(zone).filter { it.coords.chebyshevDistance(centre) <= radius && predicate(it) }
        }

    private fun <T> inZones(
        centre: CoordGrid,
        radius: Int,
        find: (ZoneKey) -> Sequence<T>,
    ): List<T> {
        val minZone = ZoneKey.from(centre.translate(-radius, -radius))
        val maxZone = ZoneKey.from(centre.translate(radius, radius))
        val found = mutableListOf<T>()
        for (zoneX in minZone.x..maxZone.x) {
            for (zoneZ in minZone.z..maxZone.z) {
                found += find(ZoneKey(zoneX, zoneZ, centre.level))
            }
        }
        return found
    }

    fun locType(loc: LocInfo): ObjectServerType? = ServerCacheManager.getObject(loc.id)

    fun isWalkable(coords: CoordGrid): Boolean = !collision.isWalkBlocked(coords)

    fun walkableNear(centre: CoordGrid, radius: Int, random: GameRandom): CoordGrid? {
        repeat(WALKABLE_TRIES) {
            val coords = centre.translate(random.of(-radius, radius), random.of(-radius, radius))
            if (isWalkable(coords)) {
                return coords
            }
        }
        return null
    }

    private companion object {
        private const val WALKABLE_TRIES = 12
    }
}
