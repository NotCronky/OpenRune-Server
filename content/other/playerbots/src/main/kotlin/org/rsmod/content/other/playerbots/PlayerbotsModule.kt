package org.rsmod.content.other.playerbots

import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.plugin.module.PluginModule

class PlayerbotsModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathCleanupHook>(BotDeathHook::class.java)
    }
}
