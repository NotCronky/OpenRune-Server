package org.rsmod.content.other.playerbots.spawn

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.random.GameRandom

/** Generates names in the styles real players pick: word pairs, names with numbers, lowercase. */
@Singleton
class BotNames @Inject constructor(private val random: GameRandom) {
    private val adjectives by lazy { words("adjectives") }
    private val nouns by lazy { words("nouns") }
    private val firstNames by lazy { words("first_names") }

    fun next(): String {
        while (true) {
            val name = candidate()
            if (VALID_NAME.matches(name)) {
                return name
            }
        }
    }

    private fun candidate(): String {
        val name =
            when (random.of(100)) {
                in 0 until 35 -> pair(random.pick(adjectives), random.pick(nouns))
                in 35 until 60 -> random.pick(firstNames) + separator() + number()
                in 60 until 80 -> pair(random.pick(firstNames), random.pick(nouns))
                in 80 until 92 -> random.pick(nouns) + number()
                else -> random.pick(adjectives) + random.pick(firstNames)
            }
        return if (random.of(100) < LOWERCASE_PERCENT) name.lowercase() else name
    }

    private fun pair(first: String, second: String): String =
        when (random.of(10)) {
            0,
            1,
            2 -> "$first $second"
            3 -> "${first}_$second"
            else -> first + second
        }

    private fun separator(): String = if (random.of(5) == 0) "_" else ""

    private fun number(): String =
        when (random.of(4)) {
            0 -> random.of(1, 99).toString()
            1 -> random.of(1980, 2012).toString()
            2 -> random.of(100, 999).toString()
            else -> random.of(1, 9).toString()
        }

    private fun words(file: String): List<String> {
        val path = "/playerbots/names/$file.txt"
        val stream =
            checkNotNull(BotNames::class.java.getResourceAsStream(path)) { "Missing $path" }
        return stream.bufferedReader().readLines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    private companion object {
        private const val LOWERCASE_PERCENT = 25
        private val VALID_NAME = Regex("[A-Za-z0-9][A-Za-z0-9 _-]{0,10}[A-Za-z0-9]")
    }
}
