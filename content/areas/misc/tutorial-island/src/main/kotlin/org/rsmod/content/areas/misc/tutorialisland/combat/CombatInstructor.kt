package org.rsmod.content.areas.misc.tutorialisland.combat

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

class CombatInstructor @Inject constructor(private val tutorial: TutorialIsland) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.newbie_combat_instructor") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            when (player.tutorialStage) {
                TutorialStage.MEET_COMBAT_INSTRUCTOR -> introduction()
                in TutorialStage.OPEN_WORN..TutorialStage.EQUIP_DAGGER -> wieldReminder()
                TutorialStage.DAGGER_EQUIPPED -> giveSwordAndShield()
                TutorialStage.EQUIP_SWORD_AND_SHIELD -> replaceSwordAndShield()
                TutorialStage.RAT_KILLED -> rangedIntroduction()
                TutorialStage.RANGE_RAT,
                TutorialStage.RANGING_RAT -> replaceRangedGear()
                else -> return@startDialogue
            }
        }
        tutorial.refresh(player)
        tutorial.checkEquipment(player)
    }

    private suspend fun Dialogue.introduction() {
        chatPlayer(happy, "Hi! My name's ${player.displayName}.")
        chatNpc(
            angry,
            "Do I look like I care? To me you're just another newcomer who thinks they're ready " +
                "to fight.",
        )
        chatNpc(
            neutral,
            "I am Vannaka, the greatest swordsman alive, and I'm here to teach you the basics of " +
                "combat.",
        )
        wieldReminder()
        tutorial.setStage(player, TutorialStage.OPEN_WORN)
    }

    private suspend fun Dialogue.wieldReminder() {
        chatNpc(neutral, "Let's get started by teaching you to wield a weapon.")
    }

    private suspend fun Dialogue.giveSwordAndShield() {
        chatNpc(
            neutral,
            "Very good, but that little butter knife isn't going to protect you much. Let's get " +
                "you something a bit better. Once you're properly equipped we can move on to " +
                "some actual combat.",
        )
        swordAndShield()
        tutorial.setStage(player, TutorialStage.EQUIP_SWORD_AND_SHIELD)
    }

    private suspend fun Dialogue.replaceSwordAndShield() {
        if (!playerContainsObj(SWORD) || !playerContainsObj(SHIELD)) {
            swordAndShield()
        }
    }

    private suspend fun Dialogue.swordAndShield() {
        if (!playerContainsObj(SWORD)) {
            access.invAdd(access.inv, SWORD)
        }
        if (!playerContainsObj(SHIELD)) {
            access.invAdd(access.inv, SHIELD)
        }
        doubleobjbox(
            SWORD,
            SHIELD,
            "The combat instructor gives you a <col=000080>bronze sword<col=000000> and a " +
                "<col=000000><col=000080>wooden shield<col=000000>.",
        )
    }

    private suspend fun Dialogue.rangedIntroduction() {
        chatPlayer(happy, "I did it! I killed a giant rat!")
        chatNpc(
            neutral,
            "I saw. You seem better at this than I thought. Now that you have grasped basic " +
                "swordplay, let's move on.",
        )
        chatNpc(
            neutral,
            "Next up we have ranged combat. With this you can kill foes from a distance. With " +
                "ranged, you'll be able to attack the rats without entering the pit meaning " +
                "they won't be able to fight back.",
        )
        bowAndArrows()
        tutorial.setStage(player, TutorialStage.RANGE_RAT)
    }

    private suspend fun Dialogue.replaceRangedGear() {
        if (!playerContainsObj(BOW) || !playerContainsObj(ARROWS)) {
            bowAndArrows()
        }
    }

    private suspend fun Dialogue.bowAndArrows() {
        if (!playerContainsObj(BOW)) {
            access.invAdd(access.inv, BOW)
        }
        if (!playerContainsObj(ARROWS)) {
            access.invAdd(access.inv, ARROWS, ARROW_COUNT)
        }
        doubleobjbox(
            BOW,
            ARROWS,
            "The combat instructor gives you a <col=000080>shortbow<col=000000> and some " +
                "<col=000000><col=000080>bronze arrows<col=000000>.",
        )
    }

    private companion object {
        const val SWORD = "obj.bronze_sword"
        const val SHIELD = "obj.wooden_shield"
        const val BOW = "obj.shortbow"
        const val ARROWS = "obj.bronze_arrow"
        const val ARROW_COUNT = 50
    }
}
