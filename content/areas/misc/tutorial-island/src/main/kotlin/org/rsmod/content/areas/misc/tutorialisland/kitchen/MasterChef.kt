package org.rsmod.content.areas.misc.tutorialisland.kitchen

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.misc.tutorialisland.TutorialIsland
import org.rsmod.content.areas.misc.tutorialisland.TutorialStage
import org.rsmod.content.areas.misc.tutorialisland.tutorialStage
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MasterChef @Inject constructor(private val tutorial: TutorialIsland) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.newbie_cook_instructor") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            when (player.tutorialStage) {
                TutorialStage.MEET_CHEF -> introduction()
                TutorialStage.MAKE_DOUGH,
                TutorialStage.BAKE_BREAD -> replaceIngredients()
                else -> return@startDialogue
            }
        }
        tutorial.refresh(player)
    }

    private suspend fun Dialogue.introduction() {
        chatNpc(
            neutral,
            "Ah! Welcome, newcomer. I am the Master Chef, Lev. It is here that I will teach you " +
                "how to cook food truly fit for a king.",
        )
        chatPlayer(angry, "I already know how to cook. Brynna taught me just now.")
        chatNpc(
            laugh,
            "Hahahahahaha! You call THAT cooking? Some shrimp on an open log fire? Oh, no, no, " +
                "no. I am going to teach you the fine art of cooking bread.",
        )
        giveIngredients()
        tutorial.setStage(player, TutorialStage.MAKE_DOUGH)
    }

    private suspend fun Dialogue.replaceIngredients() {
        val hasDough = playerContainsObj(DOUGH) || playerContainsObj("obj.bread")
        if (hasDough || (playerContainsObj(FLOUR) && playerContainsObj(WATER))) {
            return
        }
        giveIngredients()
    }

    private suspend fun Dialogue.giveIngredients() {
        if (!playerContainsObj(FLOUR)) {
            access.invAdd(access.inv, FLOUR)
        }
        if (!playerContainsObj(WATER)) {
            access.invAdd(access.inv, WATER)
        }
        doubleobjbox(
            FLOUR,
            WATER,
            "The master chef gives you some <col=000080>flour<col=000000> and some " +
                "<col=000080>water<col=000000>.",
        )
        ClientScripts.invHighlightItemOn(player, FLOUR, HIGHLIGHT_CALL_TO_ACTION)
        ClientScripts.invHighlightItemOn(player, WATER, HIGHLIGHT_CALL_TO_ACTION)
    }

    private companion object {
        const val FLOUR = "obj.newbie_pot_flour"
        const val WATER = "obj.bucket_water"
        const val DOUGH = "obj.bread_dough"
        const val HIGHLIGHT_CALL_TO_ACTION = "dbrow.ui_highlighting_style_tutorial_cta"
    }
}
