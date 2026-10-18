package org.rsmod.content.other.playerbots

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.content.other.playerbots.brain.BotBrains
import org.rsmod.game.entity.Player

/** Runs after every death's respawn teleport; tells a bot's brain to go back for its items. */
class BotDeathHook @Inject constructor(private val brains: BotBrains) : PlayerDeathCleanupHook {
    override fun cleanup(player: Player) {
        brains[player]?.onRespawn()
    }
}
