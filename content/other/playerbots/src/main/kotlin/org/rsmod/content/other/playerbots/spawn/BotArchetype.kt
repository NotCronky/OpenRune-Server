package org.rsmod.content.other.playerbots.spawn

import org.rsmod.api.random.GameRandom

/**
 * The kinds of account a bot can be generated as. [anchor] is the level the account's skills
 * cluster around; [gearChance] is the percent chance each worn slot is filled.
 */
enum class BotArchetype(val weight: Int, val anchor: IntRange, val gearChance: Int) {
    Fresh(weight = 12, anchor = 1..1, gearChance = 25),
    Low(weight = 28, anchor = 5..40, gearChance = 70),
    Mid(weight = 28, anchor = 40..75, gearChance = 85),
    Pure(weight = 10, anchor = 1..60, gearChance = 80),
    Skiller(weight = 10, anchor = 30..95, gearChance = 25),
    Maxed(weight = 12, anchor = 85..99, gearChance = 95);

    val fightsInCombat: Boolean
        get() = this != Skiller

    fun level(stat: String, anchor: Int, random: GameRandom): Int =
        when (this) {
            Fresh -> if (random.of(100) < 15) random.of(2, 6) else 1
            Skiller -> if (stat in COMBAT_STATS || stat == PRAYER) 1 else spread(stat, anchor, random)
            Pure -> pureLevel(stat, anchor, random)
            Maxed -> spread(stat, anchor, random).let { if (it >= 95 && random.randomBoolean()) 99 else it }
            Low,
            Mid -> spread(stat, anchor, random)
        }

    private fun pureLevel(stat: String, anchor: Int, random: GameRandom): Int =
        when (stat) {
            DEFENCE -> 1
            PRAYER -> random.pick(PURE_PRAYER_LEVELS)
            ATTACK -> if (random.randomBoolean()) 60 else random.of(40, 75)
            STRENGTH -> random.of(60, 99)
            RANGED -> random.of(50, 99)
            MAGIC -> random.of(50, 94)
            else -> spread(stat, anchor, random)
        }

    private fun spread(stat: String, anchor: Int, random: GameRandom): Int {
        val bias =
            when (stat) {
                in COMBAT_STATS -> COMBAT_BIAS
                PRAYER,
                in SLOW_STATS -> SLOW_BIAS
                else -> 0
            }
        return (anchor + bias + random.of(-NOISE, NOISE)).coerceIn(1, 99)
    }

    companion object {
        const val ATTACK = "stat.attack"
        const val STRENGTH = "stat.strength"
        const val DEFENCE = "stat.defence"
        const val HITPOINTS = "stat.hitpoints"
        const val RANGED = "stat.ranged"
        const val MAGIC = "stat.magic"
        const val PRAYER = "stat.prayer"

        private const val COMBAT_BIAS = 5
        private const val SLOW_BIAS = -10
        private const val NOISE = 8

        private val COMBAT_STATS = setOf(ATTACK, STRENGTH, DEFENCE, RANGED, MAGIC)
        private val SLOW_STATS =
            setOf(
                "stat.agility",
                "stat.runecrafting",
                "stat.hunter",
                "stat.construction",
                "stat.slayer",
                "stat.farming",
                "stat.herblore",
                "stat.sailing",
            )
        private val PURE_PRAYER_LEVELS = listOf(1, 13, 31, 31, 44, 52)

        fun pick(random: GameRandom): BotArchetype {
            var roll = random.of(entries.sumOf { it.weight })
            for (archetype in entries) {
                roll -= archetype.weight
                if (roll < 0) {
                    return archetype
                }
            }
            return Low
        }
    }
}
