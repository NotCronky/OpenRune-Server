package org.rsmod.content.areas.misc.tutorialisland.account

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

class AccountGuide @Inject constructor(private val tutorial: TutorialIsland) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.newbie_account_instructor") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        startDialogue(npc) {
            when (player.tutorialStage) {
                TutorialStage.MEET_ACCOUNT_GUIDE -> introduction()
                TutorialStage.OPEN_ACCOUNT -> menuReminder()
                TutorialStage.ACCOUNT_OPENED -> explainAccount()
                TutorialStage.LEAVE_ACCOUNT_GUIDE -> movingOn()
                else -> return@startDialogue
            }
        }
        tutorial.refresh(player)
    }

    private suspend fun Dialogue.introduction() {
        chatPlayer(happy, "Hello. Who are you?")
        chatNpc(happy, "I'm the Account Guide. I'm here to tell you about your account.")
        chatPlayer(quiz, "What's an account?")
        chatNpc(
            happy,
            "Your character IS your account! It's what you are using to play the game right now. " +
                "Everyone here has one and some people even have multiple!",
        )
        menuReminder()
        tutorial.setStage(player, TutorialStage.OPEN_ACCOUNT)
    }

    private suspend fun Dialogue.menuReminder() {
        chatNpc(
            neutral,
            "There's various things you can do with your account which is where the Account " +
                "Management menu comes into play. Let's take a look at it now.",
        )
    }

    private suspend fun Dialogue.explainAccount() {
        chatNpc(happy, "As you can see, there's a few things you can do with your account.")
        chatNpc(
            happy,
            "First, you have membership. All accounts are free to play by default but if you " +
                "desire, you can buy membership to unlock members' worlds. These worlds give " +
                "you access to extra skills, areas, quests, and more!",
        )
        chatPlayer(quiz, "What's a world?")
        chatNpc(
            happy,
            "A world is what you play the game on. Most worlds can hold up to 2000 players. They " +
                "are split into two main types: free worlds and members' worlds. You can swap " +
                "worlds with the World Switcher in the Logout menu.",
        )
        chatNpc(
            happy,
            "You are currently on a free world. If you want to switch to a members' world and " +
                "enjoy all of the benefits, you'll need to purchase membership. You can do so " +
                "using the store in the Account Management menu.",
        )
        chatNpc(
            happy,
            "Bonds are another thing that you should know about. You can purchase Bonds from the " +
                "store in the Account Management menu. Once you have some Bonds, you can redeem " +
                "them for membership!",
        )
        chatPlayer(quiz, "But why would I use a Bond instead of just buying membership normally?")
        chatNpc(
            happy,
            "Unlike membership, Bonds can be traded with other players. You could purchase a " +
                "Bond and then sell it to another player for some gold. Alternatively, you " +
                "could use your gold to buy a Bond from another player!",
        )
        chatPlayer(
            quiz,
            "So if I used my gold to buy a Bond from another player, I could get membership " +
                "without spending any real money?",
        )
        chatNpc(happy, "Exactly!")
        chatNpc(
            happy,
            "Next up, you have your inbox and name changer. The inbox is where you'll receive " +
                "important information related to your account and the game. Be sure to keep an " +
                "eye on it.",
        )
        chatNpc(
            neutral,
            "The name changer allows you to change your display name if you don't like your " +
                "current one. Be aware though that this is only available to accounts with " +
                "membership.",
        )
        chatPlayer(happy, "Awesome. Is there anything else I should know about accounts?")
        chatNpc(
            neutral,
            "Your account is very valuable and it's important you look after it. Various tools " +
                "can help with this, such as Bank PINs and two-factor authentication options.",
        )
        chatNpc(
            neutral,
            "You'll find more information on account security on the Customer Support webpage. " +
                "You can use the Useful Links section of the Account Management menu to get " +
                "there.",
        )
        tutorial.setStage(player, TutorialStage.LEAVE_ACCOUNT_GUIDE)
        movingOn()
    }

    private suspend fun Dialogue.movingOn() {
        chatNpc(
            happy,
            "Anyway, that's everything I have to tell you about accounts. Let me know if you " +
                "need a recap, otherwise head on to your next instructor.",
        )
    }
}
