package org.rsmod.content.areas.misc.tutorialisland.bank

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.areas.misc.tutorialisland.TutorialIsland
import org.rsmod.content.areas.misc.tutorialisland.TutorialStage
import org.rsmod.content.areas.misc.tutorialisland.tutorialStage
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BankAreaScript @Inject constructor(private val tutorial: TutorialIsland) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.newbiebankbooth") { useBankBooth() }
        onOpLoc1("loc.pollbooth_green") { usePollBooth() }
    }

    private fun ProtectedAccess.useBankBooth() {
        if (player.tutorialStage < TutorialStage.OPEN_BANK) {
            tutorial.refresh(player)
            return
        }
        if (!tryOpenBank()) {
            return
        }
        if (player.tutorialStage == TutorialStage.OPEN_BANK) {
            tutorial.advance(player, TutorialStage.USE_POLL_BOOTH)
        }
    }

    private suspend fun ProtectedAccess.usePollBooth() {
        if (player.tutorialStage < TutorialStage.USE_POLL_BOOTH) {
            tutorial.refresh(player)
            return
        }
        arriveDelay()
        mesbox(
            "Poll booths are found in towns across the world. The Old School RuneScape community " +
                "is invited to vote on future game updates, to decide whether each update should " +
                "or shouldn't be released."
        )
        mesbox(
            "Voting is open to <col=7f0000>members</col> with a <col=7f0000>skill total of " +
                "300</col>. To preserve the old-school character of this game, an update must " +
                "gain <col=7f0000>70%</col> support to be released."
        )
        objbox(
            "obj.cert_furnace_icon_dummy",
            "A flag appears on the booth to let you know when you're invited to vote in a poll.",
        )
        if (player.tutorialStage == TutorialStage.USE_POLL_BOOTH) {
            tutorial.advance(player, TutorialStage.LEAVE_BANK)
        } else {
            tutorial.refresh(player)
        }
    }
}
