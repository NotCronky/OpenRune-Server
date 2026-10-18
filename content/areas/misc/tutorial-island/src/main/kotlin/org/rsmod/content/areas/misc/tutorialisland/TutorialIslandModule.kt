package org.rsmod.content.areas.misc.tutorialisland

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.player.hook.PlayerInvUpdateHook
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.plugin.module.PluginModule

class TutorialIslandModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackValidateHook>(TutorialAttackHook::class.java)
        addSetBinding<NpcDeathKillHook>(TutorialKillHook::class.java)
        addSetBinding<PlayerInvUpdateHook>(TutorialEquipmentHook::class.java)
        addSetBinding<PlayerTeleportValidateHook>(TutorialTeleportHook::class.java)
        addSetBinding<PlayerPostTickHook>(TutorialLeaveHook::class.java)
        addSetBinding<PlayerDeathHook>(TutorialDeathHook::class.java)
        addSetBinding<PlayerRespawnHook>(TutorialRespawnHook::class.java)
    }
}
