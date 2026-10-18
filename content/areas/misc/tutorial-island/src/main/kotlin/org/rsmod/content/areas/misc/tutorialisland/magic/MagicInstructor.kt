package org.rsmod.content.areas.misc.tutorialisland.magic

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.content.areas.misc.tutorialisland.LEARNING_THE_ROPES_COMPLETE_QUEUE
import org.rsmod.content.areas.misc.tutorialisland.TutorialIsland
import org.rsmod.content.areas.misc.tutorialisland.TutorialStage
import org.rsmod.content.areas.misc.tutorialisland.tutorialStage
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MagicInstructor @Inject constructor(private val tutorial: TutorialIsland) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.newbie_magic_instructor") { talk(it.npc) }
        onPlayerQueue(LEARNING_THE_ROPES_COMPLETE_QUEUE) { completeLearningTheRopes() }
    }

    private fun ProtectedAccess.completeLearningTheRopes() {
        if (player.tutorialStage != TutorialStage.SPELL_CAST) {
            return
        }
        Quest.get(LEARNING_THE_ROPES)?.completeQuest(this)
        tutorial.refresh(player)
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            when (player.tutorialStage) {
                TutorialStage.MEET_MAGIC_INSTRUCTOR -> introduction()
                TutorialStage.OPEN_MAGIC -> magicTabReminder()
                TutorialStage.MAGIC_OPENED -> windStrike()
                TutorialStage.CAST_WIND_STRIKE -> replaceRunes()
                TutorialStage.QUEST_COMPLETE -> mainland()
                TutorialStage.HOME_TELEPORT -> homeTeleportReminder()
                else -> return@startDialogue
            }
        }
        tutorial.refresh(player)
    }

    private suspend fun Dialogue.introduction() {
        chatPlayer(neutral, "Hello.")
        magicTabReminder()
        tutorial.setStage(player, TutorialStage.OPEN_MAGIC)
    }

    private suspend fun Dialogue.magicTabReminder() {
        chatNpc(
            neutral,
            "Good day, newcomer. My name is Terrova. I'm here to tell you about Magic. Let's " +
                "start by opening your magic interface.",
        )
    }

    private suspend fun Dialogue.windStrike() {
        windStrikeReminder()
        giveRunes()
        tutorial.setStage(player, TutorialStage.CAST_WIND_STRIKE)
    }

    private suspend fun Dialogue.windStrikeReminder() {
        chatNpc(
            neutral,
            "Currently you can only cast one offensive spell called Wind Strike. Let's try it " +
                "out on one of those chickens.",
        )
    }

    private suspend fun Dialogue.replaceRunes() {
        windStrikeReminder()
        if (invTotal(player.inv, AIR_RUNE) < 1 || invTotal(player.inv, MIND_RUNE) < 1) {
            giveRunes()
        }
    }

    private suspend fun Dialogue.giveRunes() {
        access.invAdd(access.inv, AIR_RUNE, RUNE_COUNT)
        access.invAdd(access.inv, MIND_RUNE, RUNE_COUNT)
        doubleobjbox(
            AIR_RUNE,
            MIND_RUNE,
            "Terrova gives you some <col=000080>air runes<col=000000> and <col=000080>mind " +
                "runes<col=000000>.",
        )
    }

    private suspend fun Dialogue.mainland() {
        chatNpc(
            neutral,
            "Well, you're all finished here now. I'll give you a reasonable number of runes when " +
                "you leave.",
        )
        val title = "Do you want to go to the mainland?"
        val leaving = choice2("Yes.", true, "No.", false, title = title)
        if (!leaving) {
            return
        }
        chatNpc(quiz, "Were you planning to become an Ironman, by the way?")
        val ironman =
            choice3(
                "What's an Ironman?",
                true,
                "Yes, I do want to become an Ironman.",
                true,
                "No, I'm not planning to do that.",
                false,
            )
        if (ironman) {
            access.ifOpenMainModal(IRONMAN_SETUP)
            return
        }
        chatPlayer(bored, "No, I'm not planning to do that.")
        chatNpc(
            neutral,
            "When you get to the mainland you will find yourself in the town of Lumbridge. If " +
                "you want some ideas on where to go next, talk to my friend Adventurer Jon. You " +
                "can't miss him; he's wearing light blue armour with a huge sword. There are " +
                "also many tutors willing to teach you about the many skills you could learn.",
        )
        objbox(
            "obj.ap_guide_icon_dummy",
            "When you get to Lumbridge, look out for Adventurer Jon, he can be found outside of " +
                "the Sheared Ram pub.",
        )
        tutorial.setStage(player, TutorialStage.HOME_TELEPORT)
        homeTeleportReminder()
    }

    private suspend fun Dialogue.homeTeleportReminder() {
        chatNpc(
            happy,
            "Excellent, I think that covers everything. When you're ready to go, use your home " +
                "teleport spell to teleport to Lumbridge!",
        )
    }

    private companion object {
        const val LEARNING_THE_ROPES = "quest_learningtheropes"
        const val AIR_RUNE = "obj.airrune"
        const val MIND_RUNE = "obj.mindrune"
        const val RUNE_COUNT = 5
        const val IRONMAN_SETUP = "interface.ironman_setup"
    }
}
