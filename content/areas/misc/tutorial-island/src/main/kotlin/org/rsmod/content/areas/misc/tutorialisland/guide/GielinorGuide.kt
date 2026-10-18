package org.rsmod.content.areas.misc.tutorialisland.guide

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

class GielinorGuide @Inject constructor(private val tutorial: TutorialIsland) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.newbie_basics_instructor") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            when (player.tutorialStage) {
                TutorialStage.GETTING_STARTED -> welcome()
                TutorialStage.OPEN_SETTINGS -> settingsReminder()
                TutorialStage.SETTINGS_OPENED -> sendToFirstInstructor()
                TutorialStage.LEAVE_GUIDE -> firstInstructorReminder()
                else -> return@startDialogue
            }
        }
        tutorial.refresh(player)
    }

    private suspend fun Dialogue.welcome() {
        chatNpc(
            happy,
            "Greetings! I see you are a new arrival to the world of Gielinor. My job is to " +
                "welcome all new visitors. So welcome!",
        )
        chatNpc(
            happy,
            "You have already learned the first thing needed to succeed in this world: talking " +
                "to other people!",
        )
        chatNpc(
            neutral,
            "You will find many inhabitants of this world have useful things to say to you. By " +
                "clicking on them you can talk to them.",
        )
        tutorial.setStage(player, TutorialStage.OPEN_SETTINGS)
        settingsReminder()
    }

    private suspend fun Dialogue.settingsReminder() {
        chatNpc(happy, "Now then, let's start by looking at your settings menu.")
    }

    private suspend fun Dialogue.sendToFirstInstructor() {
        chatNpc(
            happy,
            "Looks like you're making good progress! The menu you've just opened is one of many. " +
                "You'll learn about the rest as you progress through the tutorial.",
        )
        tutorial.setStage(player, TutorialStage.LEAVE_GUIDE)
        firstInstructorReminder()
    }

    private suspend fun Dialogue.firstInstructorReminder() {
        chatNpc(happy, "Anyway, I'd say it's time for you to go and meet your first instructor!")
    }
}
