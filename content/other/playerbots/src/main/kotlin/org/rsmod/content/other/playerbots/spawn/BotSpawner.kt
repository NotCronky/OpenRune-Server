package org.rsmod.content.other.playerbots.spawn

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.FacialHairStylesRow
import org.rsmod.api.table.HairStylesRow
import org.rsmod.api.table.HandStylesRow
import org.rsmod.api.table.LeggingStylesRow
import org.rsmod.api.table.PlayerbotKitRow
import org.rsmod.api.table.PlayerbotSpawnsRow
import org.rsmod.api.table.ShoeStylesRow
import org.rsmod.api.table.SleeveStylesRow
import org.rsmod.api.table.TorsoStylesRow
import org.rsmod.api.table.fishing.FishingMethodRow
import org.rsmod.api.table.fishing.FishingSpotRow
import org.rsmod.api.utils.skills.CombatLevel
import org.rsmod.content.other.playerbots.spawn.BotArchetype.Companion.ATTACK
import org.rsmod.content.other.playerbots.spawn.BotArchetype.Companion.DEFENCE
import org.rsmod.content.other.playerbots.spawn.BotArchetype.Companion.HITPOINTS
import org.rsmod.content.other.playerbots.spawn.BotArchetype.Companion.MAGIC
import org.rsmod.content.other.playerbots.spawn.BotArchetype.Companion.PRAYER
import org.rsmod.content.other.playerbots.spawn.BotArchetype.Companion.RANGED
import org.rsmod.content.other.playerbots.spawn.BotArchetype.Companion.STRENGTH
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeLevelReq
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingReq
import org.rsmod.game.entity.player.Appearance
import org.rsmod.game.inv.InvObj
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

/** Rolls new bot accounts: archetype, stats, look, gear, food, bank and a starting tile. */
@Singleton
class BotSpawner
@Inject
constructor(
    private val random: GameRandom,
    private val collision: CollisionFlagMap,
    private val names: BotNames,
) {
    private val stats by lazy {
        ServerCacheManager.getStats().values.filterNot { it.unreleased }
    }

    private val kitStyles by lazy {
        listOf(
            HairStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
            FacialHairStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
            TorsoStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
            SleeveStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
            HandStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
            LeggingStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
            ShoeStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
        )
    }

    fun name(): String = names.next()

    fun spawnCoord(): CoordGrid {
        val spawns = PlayerbotSpawnsRow.all()
        var roll = random.of(spawns.sumOf { it.weight })
        val spawn = spawns.first { row -> (roll - row.weight).also { roll = it } < 0 }
        return freeTileNear(spawn.coords) ?: spawn.coords
    }

    fun profile(): BotProfile {
        val archetype = BotArchetype.pick(random)
        val levels = levels(archetype)
        val combat = combatLevel(levels)
        val style = if (archetype.fightsInCombat) style(levels) else null
        val food = if (style != null) food(combat) else emptyList()
        val (carried, banked) = tools(levels).partition { random.of(100) < CARRY_TOOL_PERCENT }
        return BotProfile(
            archetype = archetype,
            levels = levels,
            worn = gear(archetype, style, levels, combat),
            inv = carried.flatten() + food,
            bank = listOf(InvObj(COINS, coins(archetype))) + banked.flatten(),
        )
    }

    /** The same [accountName] always gives the same look, so it survives logouts unsaved. */
    fun look(accountName: String): BotLook {
        val seeded = DefaultGameRandom(accountName.lowercase().hashCode().toLong())
        val typeB = seeded.of(100) < BODY_TYPE_B_PERCENT
        val bodyType = if (typeB) Appearance.BODY_TYPE_B else Appearance.BODY_TYPE_A
        val pronoun =
            when {
                seeded.of(100) < PRONOUN_THEY_PERCENT -> Appearance.PRONOUN_THEY
                bodyType == Appearance.BODY_TYPE_B -> Appearance.PRONOUN_SHE
                else -> Appearance.PRONOUN_HE
            }
        return BotLook(
            bodyType = bodyType,
            pronoun = pronoun,
            identKit = kitStyles.map { styles -> seeded.pick(styles).kitFor(typeB) },
            colours = COLOUR_COUNTS.map { seeded.of(it) },
        )
    }

    private fun Pair<Int, Int>.kitFor(typeB: Boolean): Int = if (typeB) second else first

    private fun levels(archetype: BotArchetype): Map<String, Int> {
        val anchor = random.of(archetype.anchor)
        val levels = mutableMapOf<String, Int>()
        for (stat in stats) {
            val name = RSCM.getReverseMapping(RSCMType.STAT, stat.id)
            levels[name] = archetype.level(name, anchor, random).coerceIn(stat.minLevel, stat.maxLevel)
        }
        levels[HITPOINTS] = hitpoints(archetype, levels)
        return levels
    }

    private fun hitpoints(archetype: BotArchetype, levels: Map<String, Int>): Int {
        if (archetype == BotArchetype.Skiller || archetype == BotArchetype.Fresh) {
            return MIN_HITPOINTS
        }
        val highest = listOf(ATTACK, STRENGTH, DEFENCE, RANGED, MAGIC).maxOf { levels[it] ?: 1 }
        return (highest + random.of(-HITPOINTS_NOISE, HITPOINTS_NOISE)).coerceIn(MIN_HITPOINTS, 99)
    }

    private fun combatLevel(levels: Map<String, Int>): Int =
        CombatLevel.calculate(
            attack = levels[ATTACK] ?: 1,
            defence = levels[DEFENCE] ?: 1,
            strength = levels[STRENGTH] ?: 1,
            hitpoints = levels[HITPOINTS] ?: MIN_HITPOINTS,
            ranged = levels[RANGED] ?: 1,
            magic = levels[MAGIC] ?: 1,
            prayer = levels[PRAYER] ?: 1,
        )

    private fun style(levels: Map<String, Int>): String {
        val melee = maxOf(levels[ATTACK] ?: 1, levels[STRENGTH] ?: 1)
        val ranged = levels[RANGED] ?: 1
        val magic = levels[MAGIC] ?: 1
        return when {
            melee >= ranged && melee >= magic -> ROLE_MELEE
            ranged >= magic -> ROLE_RANGED
            else -> ROLE_MAGIC
        }
    }

    /**
     * Fills each worn slot by chance with the best or second-best tier the bot can wear, where an
     * obj's tier is its highest `statreq` level. A two-handed weapon leaves the shield slot empty.
     */
    private fun gear(
        archetype: BotArchetype,
        style: String?,
        levels: Map<String, Int>,
        combat: Int,
    ): List<InvObj> {
        val roles = if (style == null) setOf(ROLE_ANY) else setOf(style, ROLE_ANY)
        val bySlot =
            PlayerbotKitRow.all()
                .filter { it.role in roles && it.minCombat <= combat && it.obj.meets(levels) }
                .groupBy { it.obj.wearpos1 }
        val worn = mutableListOf<InvObj>()
        val blocked = mutableSetOf<Int>()
        for ((slot, rows) in bySlot.entries.sortedBy { it.key }) {
            if (slot in blocked || random.of(100) >= archetype.gearChance) {
                continue
            }
            val tiers = rows.groupBy { it.obj.tier() }.toSortedMap(reverseOrder()).values.toList()
            val tier = if (tiers.size > 1 && random.of(100) < LOWER_TIER_PERCENT) tiers[1] else tiers[0]
            val row = random.pick(tier)
            worn += InvObj(row.obj, row.count)
            row.obj.wearpos2.takeIf { it >= 0 }?.let(blocked::add)
            row.obj.wearpos3.takeIf { it >= 0 }?.let(blocked::add)
        }
        return worn
    }

    /**
     * Gathering kits: the best axe, pickaxe and fishing tool (with its bait) the bot can use, each
     * owned by chance so some accounts never bought one.
     */
    private fun tools(levels: Map<String, Int>): List<List<InvObj>> =
        listOfNotNull(
            bestTool(ROLE_AXE, levels[WOODCUTTING] ?: 1) { it.axeWoodcuttingReq }?.let(::listOf),
            bestTool(ROLE_PICKAXE, levels[MINING] ?: 1) { it.pickaxeLevelReq }?.let(::listOf),
            bestTool(ROLE_FISHING, levels[FISHING] ?: 1, ::fishingToolLevel)?.let(::withBait),
        ).filter { random.of(100) < OWNS_TOOL_PERCENT }

    private fun bestTool(role: String, level: Int, req: (ItemServerType) -> Int): InvObj? =
        PlayerbotKitRow.all()
            .filter { it.role == role && req(it.obj) <= level }
            .maxByOrNull { req(it.obj) }
            ?.let { InvObj(it.obj) }

    /** The lowest level that catches anything with [tool]; unusable when nothing does. */
    private fun fishingToolLevel(tool: ItemServerType): Int {
        val methods =
            FishingMethodRow.all().filter { it.tool == tool.internalName }.map { it.methodId }
        return FishingSpotRow.all().filter { it.method in methods }.minOfOrNull { it.level }
            ?: Int.MAX_VALUE
    }

    private fun withBait(tool: InvObj): List<InvObj> {
        val name = getInvObj(tool).internalName
        val bait = FishingMethodRow.all().firstOrNull { it.tool == name }?.bait ?: return listOf(tool)
        val row =
            PlayerbotKitRow.all().firstOrNull { it.role == ROLE_BAIT && it.obj.internalName == bait }
                ?: return listOf(tool)
        return listOf(tool, InvObj(row.obj, row.count))
    }

    private fun food(combat: Int): List<InvObj> {
        val options = PlayerbotKitRow.all().filter { it.role == ROLE_FOOD && it.minCombat <= combat }
        val best = options.maxByOrNull { it.minCombat } ?: return emptyList()
        return List(random.of(MIN_FOOD, best.count)) { InvObj(best.obj) }
    }

    private fun coins(archetype: BotArchetype): Int =
        when (archetype) {
            BotArchetype.Fresh -> random.of(0, 500)
            BotArchetype.Low -> random.of(1_000, 50_000)
            BotArchetype.Mid,
            BotArchetype.Pure,
            BotArchetype.Skiller -> random.of(50_000, 5_000_000)
            BotArchetype.Maxed -> random.of(5_000_000, 200_000_000)
        }

    private fun ItemServerType.meets(levels: Map<String, Int>): Boolean =
        requirements().all { (stat, level) -> (levels[stat] ?: 1) >= level }

    private fun ItemServerType.tier(): Int = requirements().maxOfOrNull { it.second } ?: 0

    private fun ItemServerType.requirements(): List<Pair<String, Int>> =
        listOfNotNull(
            paramOrNull(BaseParams.statreq1_skill)?.let {
                RSCM.getReverseMapping(RSCMType.STAT, it.id) to (paramOrNull(BaseParams.statreq1_level) ?: 0)
            },
            paramOrNull(BaseParams.statreq2_skill)?.let {
                RSCM.getReverseMapping(RSCMType.STAT, it.id) to (paramOrNull(BaseParams.statreq2_level) ?: 0)
            },
        )

    private fun freeTileNear(center: CoordGrid): CoordGrid? {
        val free = mutableListOf<CoordGrid>()
        for (dx in -SPAWN_RADIUS..SPAWN_RADIUS) {
            for (dz in -SPAWN_RADIUS..SPAWN_RADIUS) {
                val coord = center.translate(dx, dz)
                if (!collision.isWalkBlocked(coord)) {
                    free += coord
                }
            }
        }
        return random.pickOrNull(free)
    }

    private companion object {
        private const val ROLE_MELEE = "melee"
        private const val ROLE_RANGED = "ranged"
        private const val ROLE_MAGIC = "magic"
        private const val ROLE_ANY = "any"
        private const val ROLE_FOOD = "food"
        private const val ROLE_AXE = "axe"
        private const val ROLE_PICKAXE = "pickaxe"
        private const val ROLE_FISHING = "fishing"
        private const val ROLE_BAIT = "fishing_bait"
        private const val WOODCUTTING = "stat.woodcutting"
        private const val MINING = "stat.mining"
        private const val FISHING = "stat.fishing"
        private const val OWNS_TOOL_PERCENT = 75
        private const val CARRY_TOOL_PERCENT = 40
        private const val COINS = "obj.coins"

        private const val MIN_HITPOINTS = 10
        private const val HITPOINTS_NOISE = 4
        private const val LOWER_TIER_PERCENT = 30
        private const val BODY_TYPE_B_PERCENT = 40
        private const val PRONOUN_THEY_PERCENT = 3
        private const val MIN_FOOD = 3
        private const val SPAWN_RADIUS = 4

        /** Client palette sizes for hair, torso, legs, feet and skin, in appearance slot order. */
        private val COLOUR_COUNTS = listOf(25, 29, 29, 6, 8)
    }
}
