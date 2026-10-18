package org.rsmod.content.other.playerbots.brain

import jakarta.inject.Singleton
import org.rsmod.api.random.GameRandom

/** Canned public chat lines, read from `playerbots/chat/idle.txt`. */
@Singleton
class BotChat {
    private val lines by lazy {
        val stream =
            checkNotNull(BotChat::class.java.getResourceAsStream(PATH)) { "Missing $PATH" }
        stream.bufferedReader().readLines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun line(random: GameRandom): String = random.pick(lines)

    private companion object {
        private const val PATH = "/playerbots/chat/idle.txt"
    }
}
