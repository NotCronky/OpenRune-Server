package org.rsmod.content.areas.misc.tutorialisland.quest

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

class QuestGuide @Inject constructor(private val tutorial: TutorialIsland) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.newbie_quest_instructor") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            when (player.tutorialStage) {
                TutorialStage.MEET_QUEST_GUIDE -> introduction()
                TutorialStage.OPEN_QUEST_JOURNAL -> journalReminder()
                TutorialStage.QUEST_JOURNAL_OPENED -> explainQuests()
                TutorialStage.LEAVE_QUEST_GUIDE -> movingOn()
                else -> return@startDialogue
            }
        }
        tutorial.refresh(player)
    }

    private suspend fun Dialogue.introduction() {
        journalReminder()
        tutorial.setStage(player, TutorialStage.OPEN_QUEST_JOURNAL)
    }

    private suspend fun Dialogue.journalReminder() {
        chatNpc(
            neutral,
            "Ah. Welcome, adventurer. I'm here to tell you all about quests. Let's start by " +
                "opening your quest list.",
        )
    }

    private suspend fun Dialogue.explainQuests() {
        chatNpc(
            neutral,
            "Now that you have the quest list open, you can see all the quests within it. " +
                "Opening a quest's journal will provide you with more information on it.",
        )
        chatNpc(
            neutral,
            "If you haven't started the quest, it will tell you where to begin and what " +
                "requirements you need. If the quest is in progress, it will remind you what to " +
                "do next.",
        )
        chatNpc(
            neutral,
            "It's very easy to find quest start points. Just look out for the quest icon on your " +
                "minimap. You should see one marking this house.",
        )
        objbox(
            "obj.quest_start_icon_dummy",
            "The minimap in the top right corner of the screen has various icons to show " +
                "different points of interest. Look for the icon to the left to find quest " +
                "start points.",
        )
        chatNpc(
            neutral,
            "The quests themselves can vary greatly from collecting beads to hunting down " +
                "dragons. Completing quests will reward you with all sorts of things, such as " +
                "new areas and better weapons!",
        )
        tutorial.setStage(player, TutorialStage.LEAVE_QUEST_GUIDE)
        movingOn()
    }

    private suspend fun Dialogue.movingOn() {
        chatNpc(
            neutral,
            "There's not a lot more I can tell you about questing. You have to experience the " +
                "thrill of it yourself to fully understand. Let me know if you want a recap, " +
                "otherwise you can move on.",
        )
    }
}
