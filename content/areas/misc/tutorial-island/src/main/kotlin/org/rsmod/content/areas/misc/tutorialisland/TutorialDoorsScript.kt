package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TutorialDoorsScript
@Inject
constructor(private val tutorial: TutorialIsland, private val locRepo: LocRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.newbie_door1") {
            passDoor(it.loc, TutorialStage.LEAVE_GUIDE, TutorialStage.MEET_SURVIVAL_EXPERT, true)
        }
        onOpLoc1(GATE_LEFT) { passGate(it.loc) }
        onOpLoc1(GATE_RIGHT) { passGate(it.loc) }
        onOpLoc1("loc.newbie_door2") {
            passDoor(it.loc, TutorialStage.FIND_KITCHEN, TutorialStage.MEET_CHEF, false)
        }
        onOpLoc1("loc.newbie_door3") {
            passDoor(it.loc, TutorialStage.LEAVE_KITCHEN, TutorialStage.RUN_TO_QUEST_GUIDE, true)
        }
        onOpLoc1("loc.newbie_door6") {
            passDoor(it.loc, TutorialStage.LEAVE_BANK, TutorialStage.MEET_ACCOUNT_GUIDE, true)
        }
        onOpLoc1("loc.newbie_door7") {
            val stage = TutorialStage.LEAVE_ACCOUNT_GUIDE
            passDoor(it.loc, stage, TutorialStage.MEET_BROTHER_BRACE, enterFromAcross = true)
        }
        onOpLoc1("loc.newbie_door8") {
            val stage = TutorialStage.LEAVE_CHAPEL
            passDoor(it.loc, stage, TutorialStage.MEET_MAGIC_INSTRUCTOR, enterFromAcross = true)
        }
        onOpLoc1("loc.newbie_door4") {
            val stage = TutorialStage.RUN_TO_QUEST_GUIDE
            passDoor(it.loc, stage, TutorialStage.MEET_QUEST_GUIDE, enterFromAcross = false)
        }
    }

    /**
     * [enterFromAcross] is whether the side the tutorial approaches the door from is the tile
     * across the door's wall edge, rather than the door's own tile.
     */
    private suspend fun ProtectedAccess.passDoor(
        door: BoundLocInfo,
        required: Int,
        next: Int,
        enterFromAcross: Boolean,
    ) {
        arriveDelay()
        if (!canPass(door.coords, required, enterFromAcross)) {
            return
        }
        val across = door.coords.acrossEdge(door.angle)
        locRepo.del(door, OPEN_TICKS)
        locRepo.add(across, "loc.inactivepoordoor", OPEN_TICKS, door.angle.turn(1), door.shape)
        soundSynth("synth.door_open")
        playerWalk(if (coords == door.coords) across else door.coords)
        if (player.tutorialStage == required) {
            tutorial.advance(player, next)
        }
    }

    private suspend fun ProtectedAccess.passGate(clicked: BoundLocInfo) {
        arriveDelay()
        if (!canPass(clicked.coords, TutorialStage.LEAVE_SURVIVAL, enterFromAcross = true)) {
            return
        }
        val clickedPanel = locRepo.findExact(clicked.coords, clicked.shape)
        val panels = listOfNotNull(clickedPanel, gatePartner(clicked))
        val left = panels.firstOrNull { it.id == GATE_LEFT.asRSCM(RSCMType.LOC) } ?: return
        val swungAngle = left.angle.turn(3)
        val leftOpen = left.coords.acrossEdge(left.angle)
        for (panel in panels) {
            locRepo.del(panel, OPEN_TICKS)
        }
        locRepo.add(leftOpen, "loc.inactivefencegate_l", OPEN_TICKS, swungAngle, clicked.shape)
        locRepo.add(
            leftOpen.acrossEdge(left.angle),
            "loc.inactivefencegate_r",
            OPEN_TICKS,
            swungAngle,
            clicked.shape,
        )
        soundSynth("synth.picketgate_open")
        val across = clicked.coords.acrossEdge(clicked.angle)
        playerWalk(if (coords == clicked.coords) across else clicked.coords)
        if (player.tutorialStage == TutorialStage.LEAVE_SURVIVAL) {
            tutorial.advance(player, TutorialStage.FIND_KITCHEN)
        }
    }

    private fun ProtectedAccess.canPass(
        door: CoordGrid,
        required: Int,
        enterFromAcross: Boolean,
    ): Boolean {
        if (player.tutorialStage < required) {
            tutorial.refresh(player)
            return false
        }
        return (coords == door) != enterFromAcross
    }

    private fun gatePartner(clicked: BoundLocInfo): LocInfo? {
        val gateIds = setOf(GATE_LEFT.asRSCM(RSCMType.LOC), GATE_RIGHT.asRSCM(RSCMType.LOC))
        return listOf(clicked.coords.translate(0, 1), clicked.coords.translate(0, -1))
            .mapNotNull { locRepo.findExact(it, clicked.shape) }
            .firstOrNull { it.id in gateIds && it.id != clicked.id }
    }

    private companion object {
        const val GATE_LEFT = "loc.newbiegateclosedl2"
        const val GATE_RIGHT = "loc.newbiegateclosedr2"
        const val OPEN_TICKS = 2
    }
}
