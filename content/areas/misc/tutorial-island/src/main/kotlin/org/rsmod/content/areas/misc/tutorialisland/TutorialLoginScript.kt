package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.ui.IfOverlayButtonPressed
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TutorialLoginScript
@Inject
constructor(private val tutorial: TutorialIsland, private val config: ServerConfig) :
    PluginScript() {
    private val Player.newAccount by boolVarBit("varbit.new_player_account")

    override fun ScriptContext.startup() {
        onEvent<SessionStateEvent.PrepareLogin> { player.startTutorial() }
        onPlayerLogin { player.resumeTutorial() }
        onEvent<IfOverlayButtonPressed> { player.sideButtonPressed(this) }

        onCommand("tutorial") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Set your Tutorial Island stage (::tutorial <stage>)"
            cheat {
                val stage = args.firstOrNull()?.toIntOrNull()
                if (stage == null) {
                    player.mes("Tutorial stage: ${player.tutorialStage}")
                    return@cheat
                }
                tutorial.setStage(player, stage)
                tutorial.refresh(player)
            }
        }
    }

    private fun Player.startTutorial() {
        if (!config.gameplay.tutorialIsland || worldType != WorldType.MAIN) {
            return
        }
        if (!newAccount || tutorialStage != TutorialStage.NOT_STARTED) {
            return
        }
        coords = ISLAND_START
        tutorialStage = TutorialStage.GETTING_STARTED
    }

    private fun Player.resumeTutorial() {
        if (!inTutorial) {
            return
        }
        tutorial.closeLockedSideTabs(this)
        tutorial.refresh(this)
    }

    private fun Player.sideButtonPressed(event: IfOverlayButtonPressed) {
        if (inTutorial && event.op == IfButtonOp.Op1) {
            tutorial.onOverlayButton(this, event.component.packed)
        }
    }
}
