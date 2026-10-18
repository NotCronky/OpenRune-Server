package org.rsmod.content.areas.misc.tutorialisland.combat

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.areas.misc.tutorialisland.TutorialIsland
import org.rsmod.content.areas.misc.tutorialisland.TutorialStage
import org.rsmod.content.areas.misc.tutorialisland.acrossEdge
import org.rsmod.content.areas.misc.tutorialisland.tutorialStage
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CombatAreaScript
@Inject
constructor(private val tutorial: TutorialIsland, private val locRepo: LocRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(GATE_LEFT) { ratPitGate(it.loc) }
        onOpLoc1(GATE_RIGHT) { ratPitGate(it.loc) }
        onOpLoc1("loc.newbieladder2") { climbUp() }
    }

    private suspend fun ProtectedAccess.ratPitGate(clicked: BoundLocInfo) {
        arriveDelay()
        val stage = player.tutorialStage
        val outside = coords == clicked.coords
        when {
            stage < TutorialStage.ENTER_RAT_PIT -> tutorial.refresh(player)
            outside && stage >= TutorialStage.RANGE_RAT -> keepOutOfPit()
            outside -> {
                swingGate(clicked)
                playerWalk(clicked.coords.acrossEdge(clicked.angle))
                if (stage == TutorialStage.ENTER_RAT_PIT) {
                    tutorial.advance(player, TutorialStage.ATTACK_RAT)
                }
            }
            stage >= TutorialStage.RAT_KILLED -> {
                swingGate(clicked)
                playerWalk(clicked.coords)
            }
        }
    }

    private suspend fun ProtectedAccess.keepOutOfPit() {
        startDialogue {
            chatNpcSpecific(
                "Combat Instructor",
                "npc.newbie_combat_instructor",
                neutral,
                "No, don't enter the pit. Range the rats from outside the cage.",
            )
        }
    }

    private fun ProtectedAccess.swingGate(clicked: BoundLocInfo) {
        val gateIds = setOf(GATE_LEFT.asRSCM(RSCMType.LOC), GATE_RIGHT.asRSCM(RSCMType.LOC))
        val panels =
            listOf(clicked.coords, clicked.coords.translate(0, 1), clicked.coords.translate(0, -1))
                .mapNotNull { locRepo.findExact(it, clicked.shape) }
                .filter { it.id in gateIds }
        for (panel in panels) {
            val right = panel.id == GATE_RIGHT.asRSCM(RSCMType.LOC)
            locRepo.del(panel, OPEN_TICKS)
            locRepo.add(
                panel.coords.acrossEdge(panel.angle),
                if (right) "loc.inacmetalgateopenr" else "loc.inacmetalgateopenl",
                OPEN_TICKS,
                panel.angle.turn(if (right) 1 else 3),
                panel.shape,
            )
        }
        soundSynth(GATE_SYNTH)
    }

    private suspend fun ProtectedAccess.climbUp() {
        if (player.tutorialStage < TutorialStage.LEAVE_COMBAT) {
            tutorial.refresh(player)
            return
        }
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(1)
        telejump(BANK_LANDING, TeleportType.Exempt)
        if (player.tutorialStage == TutorialStage.LEAVE_COMBAT) {
            tutorial.advance(player, TutorialStage.OPEN_BANK)
        }
    }

    private companion object {
        const val GATE_LEFT = "loc.newbiedoor5_l"
        const val GATE_RIGHT = "loc.newbiedoor5_r"
        const val OPEN_TICKS = 2
        const val GATE_SYNTH = 71

        val BANK_LANDING = CoordGrid(3111, 3125, 0)
    }
}
