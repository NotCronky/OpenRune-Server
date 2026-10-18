package org.rsmod.content.areas.misc.tutorialisland.prayer

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

class BrotherBrace @Inject constructor(private val tutorial: TutorialIsland) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.brother_noob") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            when (player.tutorialStage) {
                TutorialStage.MEET_BROTHER_BRACE -> introduction()
                TutorialStage.OPEN_PRAYER -> greeting()
                TutorialStage.PRAYER_OPENED -> explainPrayer()
                TutorialStage.LEAVE_CHAPEL -> reminder()
                else -> return@startDialogue
            }
        }
        tutorial.refresh(player)
    }

    private suspend fun Dialogue.introduction() {
        chatPlayer(neutral, "Good day, brother.")
        greeting()
        tutorial.setStage(player, TutorialStage.OPEN_PRAYER)
    }

    private suspend fun Dialogue.greeting() {
        chatNpc(neutral, "Hello, I'm Brother Brace. I'm here to tell you all about Prayer.")
    }

    private suspend fun Dialogue.explainPrayer() {
        chatNpc(
            neutral,
            "This is your Prayer list. Prayers can help a lot in combat. Click on the prayer you " +
                "wish to use to activate it, and click it again to deactivate it.",
        )
        chatNpc(
            neutral,
            "Active prayers will drain your Prayer Points, which you can recharge by finding an " +
                "altar or other holy spot and praying there.",
        )
        chatNpc(
            neutral,
            "As you noticed, most enemies will drop bones when defeated. Burying bones, by " +
                "clicking them in your inventory, will gain you Prayer experience.",
        )
        tutorial.setStage(player, TutorialStage.LEAVE_CHAPEL)
        reminder()
    }

    private suspend fun Dialogue.reminder() {
        chatNpc(
            neutral,
            "That's all I have to teach you for now. If you need a reminder about your prayers, " +
                "just speak to me again.",
        )
    }
}
