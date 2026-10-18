package org.rsmod.content.areas.misc.tutorialisland.survival

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.misc.tutorialisland.TutorialIsland
import org.rsmod.content.areas.misc.tutorialisland.TutorialStage
import org.rsmod.content.areas.misc.tutorialisland.tutorialStage
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SurvivalExpert @Inject constructor(private val tutorial: TutorialIsland) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.newbie_survival_instructor") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            when (player.tutorialStage) {
                TutorialStage.MEET_SURVIVAL_EXPERT -> introduction()
                TutorialStage.OPEN_INVENTORY,
                TutorialStage.CATCH_SHRIMP -> fishingReminder()
                TutorialStage.OPEN_SKILLS -> {
                    chatNpc(
                        neutral,
                        "Hello again. You should take a look at that menu before we continue.",
                    )
                }
                TutorialStage.SKILLS_OPENED -> firemakingIntroduction()
                in TutorialStage.CHOP_TREE until TutorialStage.LEAVE_SURVIVAL -> replaceTools()
                else -> return@startDialogue
            }
        }
        tutorial.refresh(player)
    }

    private suspend fun Dialogue.introduction() {
        chatNpc(
            neutral,
            "Hello there, newcomer. My name is Brynna. My job is to teach you about the skills " +
                "you can use to survive in this world.",
        )
        fishingReminder()
        tutorial.setStage(player, TutorialStage.OPEN_INVENTORY)
    }

    private suspend fun Dialogue.fishingReminder() {
        chatNpc(
            neutral,
            "The first skill we're going to look at is Fishing. There's some shrimp in this pond " +
                "here. Let's try and catch some.",
        )
        if (!playerContainsObj(NET)) {
            giveNet()
        }
    }

    private suspend fun Dialogue.firemakingIntroduction() {
        chatPlayer(neutral, "I've managed to catch some shrimp.")
        chatNpc(
            happy,
            "Excellent work. Next you'll want to cook those shrimp. To do that, you'll need a " +
                "fire. This brings us on to the Woodcutting and Firemaking skills.",
        )
        giveTools()
        tutorial.setStage(player, TutorialStage.CHOP_TREE)
    }

    private suspend fun Dialogue.replaceTools() {
        if (playerContainsObj(AXE) && playerContainsObj(TINDERBOX) && playerContainsObj(NET)) {
            return
        }
        if (!playerContainsObj(NET)) {
            giveNet()
        }
        if (!playerContainsObj(AXE) || !playerContainsObj(TINDERBOX)) {
            giveTools()
        }
    }

    private suspend fun Dialogue.giveNet() {
        access.invAdd(access.inv, NET)
        objbox(NET, "The survival expert gives you a <col=000080>small fishing net<col=000000>.")
    }

    private suspend fun Dialogue.giveTools() {
        if (!playerContainsObj(AXE)) {
            access.invAdd(access.inv, AXE)
        }
        if (!playerContainsObj(TINDERBOX)) {
            access.invAdd(access.inv, TINDERBOX)
        }
        doubleobjbox(
            AXE,
            TINDERBOX,
            "The survival expert gives you a <col=000080>bronze axe<col=000000> and a " +
                "<col=000000><col=000080>tinderbox<col=000000>.",
        )
    }

    private companion object {
        const val NET = "obj.net"
        const val AXE = "obj.bronze_axe"
        const val TINDERBOX = "obj.tinderbox"
    }
}
