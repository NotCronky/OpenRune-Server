package org.rsmod.content.areas.misc.tutorialisland.mining

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

class MiningInstructor @Inject constructor(private val tutorial: TutorialIsland) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.newbie_mining_instructor") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            when (player.tutorialStage) {
                TutorialStage.MEET_MINING_INSTRUCTOR -> introduction()
                in TutorialStage.MINE_TIN..TutorialStage.SMELT_BAR -> replacePickaxe()
                TutorialStage.BAR_SMELTED -> smithingIntroduction()
                TutorialStage.OPEN_ANVIL,
                TutorialStage.SMITH_DAGGER -> replaceHammer()
                else -> return@startDialogue
            }
        }
        tutorial.refresh(player)
    }

    private suspend fun Dialogue.introduction() {
        chatNpc(
            neutral,
            "Hi there. You must be new around here. So what do I call you? 'Newcomer' seems so " +
                "impersonal, and if we're going to be working together, I'd rather call you by " +
                "name.",
        )
        chatPlayer(neutral, "You can call me ${player.displayName}.")
        chatNpc(
            neutral,
            "Ok then, ${player.displayName}. My name is Dezzick and I'm a miner by trade. Let's " +
                "teach you how to mine.",
        )
        chatNpc(
            happy,
            "Mining is very simple, all you need is a pickaxe. The rocks around here contain tin " +
                "and copper. Why don't you get started by mining some?",
        )
        givePickaxe()
        tutorial.setStage(player, TutorialStage.MINE_TIN)
    }

    private suspend fun Dialogue.replacePickaxe() {
        if (!playerContainsObj(PICKAXE)) {
            givePickaxe()
        }
    }

    private suspend fun Dialogue.givePickaxe() {
        access.invAdd(access.inv, PICKAXE)
        objbox(PICKAXE, "The mining instructor gives you a <col=000080>bronze pickaxe<col=000000>.")
    }

    private suspend fun Dialogue.smithingIntroduction() {
        chatPlayer(quiz, "I have a bronze bar. What now?")
        chatNpc(
            happy,
            "Now that you've got a bar, you can smith it into a weapon. To smith something, you " +
                "need a hammer and an anvil. There's some anvils just here that you can use. " +
                "See if you can make a bronze dagger.",
        )
        giveHammer()
        tutorial.setStage(player, TutorialStage.OPEN_ANVIL)
    }

    private suspend fun Dialogue.replaceHammer() {
        if (!playerContainsObj(HAMMER)) {
            giveHammer()
        }
    }

    private suspend fun Dialogue.giveHammer() {
        access.invAdd(access.inv, HAMMER)
        objbox(HAMMER, "The mining instructor gives you a <col=000080>hammer<col=000000>.")
    }

    private companion object {
        const val PICKAXE = "obj.bronze_pickaxe"
        const val HAMMER = "obj.hammer"
    }
}
