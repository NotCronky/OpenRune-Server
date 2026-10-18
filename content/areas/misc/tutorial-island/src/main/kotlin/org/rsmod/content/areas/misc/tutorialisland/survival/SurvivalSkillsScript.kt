package org.rsmod.content.areas.misc.tutorialisland.survival

import dev.openrune.map.MapSingletons.collision
import jakarta.inject.Inject
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.misc.tutorialisland.TutorialIsland
import org.rsmod.content.areas.misc.tutorialisland.TutorialStage
import org.rsmod.content.areas.misc.tutorialisland.tutorialStage
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.Appearance
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.collision.firstStepDestination
import org.rsmod.game.obj.Obj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SurvivalSkillsScript
@Inject
constructor(
    private val tutorial: TutorialIsland,
    private val objRegistry: ObjRegistry,
    private val locRepo: LocRepository,
    private val worldClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.0_48_48_newbiefishing") { catchShrimp() }
        onOpLoc1("loc.newbietree") { chopTree() }
        onOpHeldU("obj.tinderbox", LOGS) { lightFire() }
        onOpLocU("loc.fire", RAW_SHRIMP) { cookShrimp() }
    }

    private suspend fun ProtectedAccess.catchShrimp() {
        if (player.tutorialStage < TutorialStage.CATCH_SHRIMP) {
            tutorial.refresh(player)
            return
        }
        if (!inv.contains("obj.net")) {
            mes("You need a net to catch these fish.")
            return
        }
        tutorial.mesOverlay(
            player,
            "Your character is now attempting to catch some shrimp. ${player.hardWork()}",
        )
        anim("seq.human_smallnet")
        delay(FISHING_TICKS)
        resetAnim()
        invAdd(inv, RAW_SHRIMP)
        statAdvance("stat.fishing", FISHING_XP)
        mes("You manage to catch some shrimp.")
        if (player.tutorialStage == TutorialStage.CATCH_SHRIMP) {
            tutorial.advance(player, TutorialStage.OPEN_SKILLS)
            ClientScripts.invHighlightItemOn(player, RAW_SHRIMP, HIGHLIGHT_ALERT, lifetime = 100)
        } else {
            tutorial.refresh(player)
        }
        objbox(RAW_SHRIMP, "You manage to catch some shrimp.")
    }

    private suspend fun ProtectedAccess.chopTree() {
        if (player.tutorialStage < TutorialStage.CHOP_TREE) {
            tutorial.refresh(player)
            return
        }
        if (!inv.contains("obj.bronze_axe") && !worn.contains("obj.bronze_axe")) {
            mes("You do not have an axe which you have the Woodcutting level to use.")
            return
        }
        tutorial.mesOverlay(
            player,
            "Your character is now attempting to cut down the tree. ${player.hardWork()}",
        )
        anim("seq.human_woodcutting_bronze_axe")
        delay(WOODCUTTING_TICKS)
        resetAnim()
        invAdd(inv, LOGS)
        statAdvance("stat.woodcutting", WOODCUTTING_XP)
        mes("You manage to cut some logs.")
        if (player.tutorialStage == TutorialStage.CHOP_TREE) {
            tutorial.advance(player, TutorialStage.LIGHT_FIRE)
            ClientScripts.invHighlightItemOn(player, LOGS, HIGHLIGHT_CALL_TO_ACTION)
            ClientScripts.invHighlightItemOn(player, "obj.tinderbox", HIGHLIGHT_CALL_TO_ACTION)
        } else {
            tutorial.refresh(player)
        }
        objbox(LOGS, "You manage to cut some logs.")
    }

    private suspend fun ProtectedAccess.lightFire() {
        if (player.tutorialStage < TutorialStage.LIGHT_FIRE) {
            tutorial.refresh(player)
            return
        }
        val fireCoords = coords
        if (locRepo.findExact(fireCoords, LocShape.CentrepieceStraight) != null) {
            mes("You can't light a fire here.")
            return
        }
        invDel(inv, LOGS)
        val logs = Obj.fromServer(worldClock, fireCoords, LOGS, 1)
        objRegistry.add(logs)
        ClientScripts.invHighlightClearAll(player)
        tutorial.mesOverlay(
            player,
            "Your character is now attempting to light a fire. This should only take a few " +
                "seconds.",
        )
        anim("seq.human_createfire")
        delay(FIREMAKING_TICKS)
        if (!objRegistry.isValid(player, logs)) {
            resetAnim()
            return
        }
        objRegistry.del(logs)
        locRepo.add(
            fireCoords,
            "loc.fire",
            (FIRE_MIN_DURATION..FIRE_MAX_DURATION).random(),
            LocAngle.West,
            LocShape.CentrepieceStraight,
            onDespawn = { objRegistry.add(Obj.fromServer(worldClock, fireCoords, "obj.ashes", 1)) },
        )
        resetAnim()
        statAdvance("stat.firemaking", FIREMAKING_XP)
        collision.firstStepDestination(fireCoords, FIRE_STEP_DIRECTIONS)?.let { walk(it) }
        faceSquare(fireCoords)
        if (player.tutorialStage == TutorialStage.LIGHT_FIRE) {
            tutorial.advance(player, TutorialStage.COOK_SHRIMP)
            ClientScripts.invHighlightItemOn(player, RAW_SHRIMP, HIGHLIGHT_CALL_TO_ACTION)
        }
    }

    private suspend fun ProtectedAccess.cookShrimp() {
        if (player.tutorialStage < TutorialStage.COOK_SHRIMP) {
            tutorial.refresh(player)
            return
        }
        tutorial.mesOverlay(
            player,
            "Your character is now attempting to cook some shrimp. This will only take a few " +
                "seconds.",
        )
        anim("seq.human_firecooking")
        delay(COOKING_TICKS)
        val replaced = invReplace(inv, RAW_SHRIMP, 1, "obj.shrimp")
        if (replaced.failure) {
            resetAnim()
            return
        }
        statAdvance("stat.cooking", COOKING_XP)
        mes("You manage to cook some shrimp.")
        ClientScripts.invHighlightClearAll(player)
        if (player.tutorialStage == TutorialStage.COOK_SHRIMP) {
            tutorial.advance(player, TutorialStage.LEAVE_SURVIVAL)
        } else {
            tutorial.refresh(player)
        }
        objbox("obj.shrimp", "You manage to cook some shrimp.")
    }

    private fun Player.hardWork(): String =
        when (appearance.pronoun) {
            Appearance.PRONOUN_HE -> "Sit back for a moment while he does all the hard work."
            Appearance.PRONOUN_SHE -> "Sit back for a moment while she does all the hard work."
            else -> "Sit back for a moment while they do all the hard work."
        }

    private companion object {
        const val RAW_SHRIMP = "obj.newbieraw_shrimp"
        const val LOGS = "obj.newbielogs"

        const val HIGHLIGHT_CALL_TO_ACTION = "dbrow.ui_highlighting_style_tutorial_cta"
        const val HIGHLIGHT_ALERT = "dbrow.ui_highlighting_style_tutorial_alert"

        const val FISHING_TICKS = 6
        const val WOODCUTTING_TICKS = 5
        const val FIREMAKING_TICKS = 7
        const val COOKING_TICKS = 3

        const val FISHING_XP = 10.0
        const val WOODCUTTING_XP = 25.0
        const val FIREMAKING_XP = 40.0
        const val COOKING_XP = 30.0

        const val FIRE_MIN_DURATION = 100
        const val FIRE_MAX_DURATION = 200

        val FIRE_STEP_DIRECTIONS =
            listOf(Direction.West, Direction.East, Direction.South, Direction.North)
    }
}
