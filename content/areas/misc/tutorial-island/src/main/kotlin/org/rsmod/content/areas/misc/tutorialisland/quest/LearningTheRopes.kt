package org.rsmod.content.areas.misc.tutorialisland.quest

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.areas.misc.tutorialisland.TutorialStage
import org.rsmod.content.areas.misc.tutorialisland.tutorialStage
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestJournalBuilder
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.plugin.scripts.ScriptContext

class LearningTheRopes :
    QuestScript(
        "quest_learningtheropes",
        "varp.tutorial",
        rewards { extra("Access to the Mainland!") },
        ItemRewardDisplay("obj.blankobject"),
        questVarbit = "varp.tutorial",
    ) {
    override fun ScriptContext.init() {}

    override fun subTitle(): String =
        "talking to the <col=800000>Gielinor Guide</col> on <col=800000>Tutorial Island</col>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) {
            val stage = player.player.tutorialStage
            for ((after, text) in PARAGRAPHS) {
                if (stage > after) {
                    strike(text)
                }
            }
            when (stage) {
                TutorialStage.QUEST_JOURNAL_OPENED -> questGuideReminder()
                TutorialStage.LEAVE_MINING -> miningReminder()
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            for ((_, text) in PARAGRAPHS) {
                line(text)
            }
        }

    private fun QuestJournalBuilder.questGuideReminder() {
        line(
            "The <red>Quest Guide</red> is teaching me about the quest list and quest journals. " +
                "The journals are useful tools for tracking my progress through quests in " +
                "Gielinor, and I can keep reminders here of what I have to do next. Right now I " +
                "should speak to the <red>Quest Guide</red> again!"
        )
    }

    private fun QuestJournalBuilder.miningReminder() {
        line(
            "Now that I've learned how to mine and smith, it's time to move on. I should go " +
                "through the nearby gate to find my next instructor."
        )
    }

    private companion object {
        val PARAGRAPHS =
            listOf(
                TutorialStage.LEAVE_GUIDE to
                    "I arrived on Tutorial Island and met the Gielinor Guide, who welcomed me on " +
                        "my journey to becoming an adventurer.",
                TutorialStage.COOK_SHRIMP to
                    "I met Brynna, the Survival Expert. She taught me how to catch shrimp with a " +
                        "fishing net, and then how to cook those shrimp on a fire made from " +
                        "logs I had cut myself.",
                TutorialStage.BAKE_BREAD to
                    "Lev, the Master Chef, explained proper cooking techniques to me. With his " +
                        "guidance I made a loaf of bread from flour and water.",
                TutorialStage.QUEST_JOURNAL_OPENED to
                    "The Quest Guide taught me about my quest list and quest journals. These are " +
                        "a useful tool for tracking my progress through quests in Gielinor.",
                TutorialStage.SMELT_BAR to
                    "I then met Dezzick, the Mining Instructor. He handed me a pickaxe and " +
                        "tasked me with mining some copper and tin ores in order to smelt a " +
                        "bronze bar in the furnace, which I managed to successfully complete!",
                TutorialStage.SMITH_DAGGER to
                    "Dezzick told me to make a bronze dagger with the bronze bar I had smelted. " +
                        "To that end, he handed me a hammer and pointed me to the anvils.",
            )
    }
}
