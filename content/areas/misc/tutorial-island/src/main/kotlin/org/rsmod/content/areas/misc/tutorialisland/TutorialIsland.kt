package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.invAddAll
import org.rsmod.api.invtx.invClear
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.table.TutorialIslandSideTabsRow
import org.rsmod.api.table.TutorialIslandStarterKitRow
import org.rsmod.api.table.TutorialIslandStepsRow
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

internal var Player.tutorialStage by intVarp("varp.tutorial")
private var Player.tutorialProgress by intVarp("varp.tutorial_progress_overlay")
private var Player.flashSide by intVarBit("varbit.flashside")

internal fun CoordGrid.acrossEdge(angle: LocAngle): CoordGrid =
    when (angle) {
        LocAngle.West -> translate(-1, 0)
        LocAngle.North -> translate(0, 1)
        LocAngle.East -> translate(1, 0)
        LocAngle.South -> translate(0, -1)
    }

internal val Player.inTutorial: Boolean
    get() = tutorialStage in TutorialStage.GETTING_STARTED until TutorialStage.COMPLETE

internal val CoordGrid.onTutorialIsland: Boolean
    get() = x in ISLAND_X && (z in ISLAND_SURFACE_Z || z in ISLAND_UNDERGROUND_Z)

private val ISLAND_X = 3050..3160
private val ISLAND_SURFACE_Z = 3050..3140
private val ISLAND_UNDERGROUND_Z = 9490..9540

@Singleton
class TutorialIsland
@Inject
constructor(private val npcRepo: NpcRepository, private val eventBus: EventBus) {
    private val steps by lazy { TutorialIslandStepsRow.all().associateBy { it.stage } }
    private val sideTabs by lazy { TutorialIslandSideTabsRow.all() }
    private val starterKit by lazy {
        TutorialIslandStarterKitRow.all().map { InvObj(it.obj, it.count) }
    }

    fun setStage(player: Player, stage: Int) {
        val previous = player.tutorialStage
        player.tutorialStage = stage
        for (tab in sideTabs) {
            if (tab.unlockStage in (previous + 1)..stage) {
                openSideTab(player, tab)
            }
        }
    }

    fun advance(player: Player, stage: Int) {
        setStage(player, stage)
        refresh(player)
    }

    fun refresh(player: Player) {
        player.ifOpenOverlay(TUTORIAL_OVERLAY, OVERLAY_TARGET, eventBus)
        MiscOutput.hintArrowReset(player)

        val step = steps[player.tutorialStage]
        if (step == null) {
            player.flashSide = 0
            player.ifCloseOverlay(MES_OVERLAY, eventBus)
            return
        }

        player.tutorialProgress = step.progress
        player.flashSide = step.flashTab?.plus(1) ?: 0
        showHintArrow(player, step)

        val overlay = step.overlay
        if (overlay != null) {
            mesOverlay(player, overlay)
        } else {
            player.ifCloseOverlay(MES_OVERLAY, eventBus)
        }
        step.sideTab?.let { ClientScripts.toplevelSidebuttonSwitch(player, it) }
        step.overlayHint?.let { ClientScripts.tutorialOverlayHint(player, it) }
    }

    fun mesOverlay(player: Player, text: String) {
        player.ifOpenOverlay(MES_OVERLAY, MES_OVERLAY_TARGET, eventBus)
        ClientScripts.mesOverlay(player, text)
    }

    fun complete(player: Player) {
        player.invClear(player.inv)
        player.invClear(player.worn)
        player.invAddAll(player.inv, starterKit)
        setStage(player, TutorialStage.COMPLETE)
        player.flashSide = 0
        MiscOutput.hintArrowReset(player)
        player.ifCloseOverlay(MES_OVERLAY, eventBus)
        player.ifCloseOverlay(TUTORIAL_OVERLAY, eventBus)
        player.runClientScript(TUTORIAL_END.asRSCM(RSCMType.CLIENTSCRIPT))
        player.mes("Welcome to Old School RuneScape.", ChatType.Welcome)
    }

    fun checkEquipment(player: Player) {
        val worn = player.worn
        when (player.tutorialStage) {
            TutorialStage.EQUIP_DAGGER ->
                if (worn.contains("obj.bronze_dagger")) {
                    advance(player, TutorialStage.DAGGER_EQUIPPED)
                }
            TutorialStage.EQUIP_SWORD_AND_SHIELD ->
                if (worn.contains("obj.bronze_sword") && worn.contains("obj.wooden_shield")) {
                    advance(player, TutorialStage.OPEN_COMBAT_TAB)
                }
        }
    }

    fun closeLockedSideTabs(player: Player) {
        val stage = player.tutorialStage
        for (tab in sideTabs) {
            if (stage < tab.unlockStage) {
                player.ifCloseOverlay(tab.sideInterface, eventBus)
            }
        }
    }

    fun onOverlayButton(player: Player, component: Int) {
        val equipmentStats = EQUIPMENT_STATS_BUTTON.asRSCM(RSCMType.COMPONENT)
        if (component == equipmentStats && player.tutorialStage == TutorialStage.WORN_OPENED) {
            advance(player, TutorialStage.EQUIP_DAGGER)
            checkEquipment(player)
            return
        }
        val tab = sideTabs.firstOrNull { stoneComponent(it.tab) == component } ?: return
        val opened = tab.openedStage ?: return
        if (player.tutorialStage == tab.unlockStage) {
            advance(player, opened)
        }
    }

    private fun openSideTab(player: Player, tab: TutorialIslandSideTabsRow) {
        val target = "component.toplevel_osrs_stretch:side${tab.tab}"
        player.ifOpenOverlay(tab.sideInterface, target, eventBus)
    }

    private fun showHintArrow(player: Player, step: TutorialIslandStepsRow) {
        val npcType = step.hintNpc
        if (npcType != null) {
            val npc = findNearest(player, npcType.id) ?: return
            MiscOutput.hintArrowNpc(player, npc.slotId)
            return
        }
        val coord = step.hintCoord ?: return
        MiscOutput.hintArrowTile(player, coord, step.hintHeight ?: 0, step.hintPosition ?: 0)
    }

    private fun findNearest(player: Player, npcType: Int): Npc? =
        npcRepo
            .findAll(ZoneKey.from(player.coords), NPC_SEARCH_ZONE_RADIUS)
            .filter { it.id == npcType }
            .minByOrNull { it.coords.chebyshevDistance(player.coords) }

    private fun stoneComponent(tab: Int): Int =
        "component.toplevel_osrs_stretch:stone$tab".asRSCM(RSCMType.COMPONENT)

    private companion object {
        const val TUTORIAL_OVERLAY = "interface.tutorial_overlay"
        const val OVERLAY_TARGET = "component.toplevel_osrs_stretch:overlay_atmosphere"
        const val MES_OVERLAY = "interface.mesoverlay"
        const val MES_OVERLAY_TARGET = "component.chatbox:chatoverlay"
        const val NPC_SEARCH_ZONE_RADIUS = 4
        const val EQUIPMENT_STATS_BUTTON = "component.wornitems:equipment"
        const val TUTORIAL_END = "clientscript.[clientscript,tutorial_end]"
    }
}
