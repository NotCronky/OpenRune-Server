package org.rsmod.content.other.playerbots

import dev.openrune.DirectoryConstants
import jakarta.inject.Singleton
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * The account names that belong to bots, so a bot command never takes over a real player's
 * account. Written from the account loader thread, read from the game thread.
 */
@Singleton
class BotAccounts {
    private val names: MutableSet<String> by lazy { load() }

    @Synchronized fun contains(accountName: String): Boolean = accountName.lowercase() in names

    @Synchronized
    fun add(accountName: String) {
        val name = accountName.lowercase()
        if (!names.add(name)) {
            return
        }
        Files.createDirectories(FILE.parent)
        Files.writeString(
            FILE,
            name + System.lineSeparator(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        )
    }

    @Synchronized fun all(): List<String> = names.sorted()

    private fun load(): MutableSet<String> {
        if (!Files.exists(FILE)) {
            return mutableSetOf()
        }
        return Files.readAllLines(FILE).map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
    }

    private companion object {
        private val FILE: Path = DirectoryConstants.DATA_PATH.resolve("playerbots/accounts.txt")
    }
}
