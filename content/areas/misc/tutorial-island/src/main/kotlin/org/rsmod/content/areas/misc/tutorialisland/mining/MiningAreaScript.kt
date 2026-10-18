package org.rsmod.content.areas.misc.tutorialisland.mining

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.areas.misc.tutorialisland.TutorialIsland
import org.rsmod.content.areas.misc.tutorialisland.TutorialStage
import org.rsmod.content.areas.misc.tutorialisland.acrossEdge
import org.rsmod.content.areas.misc.tutorialisland.tutorialStage
import org.rsmod.content.skills.smithing.AnvilSmithed
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MiningAreaScript
@Inject
constructor(private val tutorial: TutorialIsland, private val locRepo: LocRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.newbieladdertop1") { climbDown() }
        onOpLoc1("loc.newbietinrock") {
            mine("obj.tin_ore", "tin", TutorialStage.MINE_TIN, TutorialStage.MINE_COPPER)
        }
        onOpLoc1("loc.newbiecopperrock") {
            mine("obj.copper_ore", "copper", TutorialStage.MINE_COPPER, TutorialStage.SMELT_BAR)
        }
        onOpLoc1("loc.newbiefurnace") { smelt() }
        onIfOpen("interface.smithing") { player.anvilOpened() }
        onEvent<AnvilSmithed> { player.smithed(product.internalName) }
        onOpLoc1(GATE_LEFT) { passGate(it.loc) }
        onOpLoc1(GATE_RIGHT) { passGate(it.loc) }
    }

    private suspend fun ProtectedAccess.climbDown() {
        if (player.tutorialStage < TutorialStage.LEAVE_QUEST_GUIDE) {
            tutorial.refresh(player)
            return
        }
        arriveDelay()
        anim("seq.human_pickupfloor")
        delay(1)
        telejump(MINE_LANDING, TeleportType.Exempt)
        if (player.tutorialStage == TutorialStage.LEAVE_QUEST_GUIDE) {
            tutorial.advance(player, TutorialStage.MEET_MINING_INSTRUCTOR)
        }
    }

    private suspend fun ProtectedAccess.mine(ore: String, name: String, stage: Int, next: Int) {
        if (player.tutorialStage < TutorialStage.MINE_TIN) {
            tutorial.refresh(player)
            return
        }
        if (!inv.contains(PICKAXE) && !worn.contains(PICKAXE)) {
            mes("You need a pickaxe to mine this rock.")
            return
        }
        tutorial.mesOverlay(
            player,
            "Your character is now attempting to mine the rock.<br>This should only take a few " +
                "seconds.",
        )
        anim("seq.human_mining_bronze_pickaxe")
        delay(MINING_TICKS)
        resetAnim()
        soundSynth(MINING_SYNTH)
        invAdd(inv, ore)
        statAdvance("stat.mining", MINING_XP)
        mes("You manage to mine some $name.")
        if (player.tutorialStage == stage) {
            tutorial.advance(player, next)
        } else {
            tutorial.refresh(player)
        }
        objbox(ore, "You manage to mine some $name.")
    }

    private suspend fun ProtectedAccess.smelt() {
        if (player.tutorialStage < TutorialStage.SMELT_BAR) {
            tutorial.refresh(player)
            return
        }
        if (!inv.contains("obj.tin_ore") || !inv.contains("obj.copper_ore")) {
            mes("You need one copper ore and one tin ore to make bronze.")
            return
        }
        tutorial.mesOverlay(player, "You smelt the copper and tin together in the furnace.")
        anim("seq.human_furnace")
        soundSynth("synth.furnace")
        delay(SMELTING_TICKS)
        if (!smeltBronze()) {
            resetAnim()
            return
        }
        statAdvance("stat.smithing", SMELTING_XP)
        tutorial.mesOverlay(player, "You retrieve a bar of bronze.")
        if (player.tutorialStage == TutorialStage.SMELT_BAR) {
            tutorial.setStage(player, TutorialStage.BAR_SMELTED)
        }
        delay(SMELTING_TICKS)
        tutorial.refresh(player)
    }

    private fun ProtectedAccess.smeltBronze(): Boolean {
        val result =
            player.invTransaction(inv) {
                val backpack = select(inv)
                delete {
                    from = backpack
                    obj = "obj.tin_ore".asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                delete {
                    from = backpack
                    obj = "obj.copper_ore".asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                insert {
                    into = backpack
                    obj = "obj.bronze_bar".asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        return result.success
    }

    private fun Player.anvilOpened() {
        if (tutorialStage == TutorialStage.OPEN_ANVIL) {
            tutorial.advance(this, TutorialStage.SMITH_DAGGER)
        }
    }

    private fun Player.smithed(product: String) {
        val stage = tutorialStage
        val smithing = stage == TutorialStage.OPEN_ANVIL || stage == TutorialStage.SMITH_DAGGER
        if (smithing && product == "obj.bronze_dagger") {
            tutorial.advance(this, TutorialStage.LEAVE_MINING)
        }
    }

    private suspend fun ProtectedAccess.passGate(clicked: BoundLocInfo) {
        arriveDelay()
        if (player.tutorialStage < TutorialStage.LEAVE_MINING) {
            tutorial.refresh(player)
            return
        }
        if (coords != clicked.coords) {
            return
        }
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
                "loc.inacmetalgateopenl",
                OPEN_TICKS,
                panel.angle.turn(if (right) 1 else 3),
                panel.shape,
            )
        }
        soundSynth(GATE_SYNTH)
        playerWalk(clicked.coords.acrossEdge(clicked.angle))
        if (player.tutorialStage == TutorialStage.LEAVE_MINING) {
            tutorial.advance(player, TutorialStage.MEET_COMBAT_INSTRUCTOR)
        }
    }

    private companion object {
        const val PICKAXE = "obj.bronze_pickaxe"
        const val GATE_LEFT = "loc.newbiedoor4l"
        const val GATE_RIGHT = "loc.newbiedoor4r"

        const val MINING_TICKS = 5
        const val SMELTING_TICKS = 3
        const val OPEN_TICKS = 2
        const val MINING_XP = 17.5
        const val SMELTING_XP = 6.25

        const val MINING_SYNTH = 3600
        const val GATE_SYNTH = 71

        val MINE_LANDING = CoordGrid(3088, 9520, 0)
    }
}
