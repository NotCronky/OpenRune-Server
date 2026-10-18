package org.rsmod.content.areas.misc.tutorialisland.kitchen

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.areas.misc.tutorialisland.TutorialIsland
import org.rsmod.content.areas.misc.tutorialisland.TutorialStage
import org.rsmod.content.areas.misc.tutorialisland.tutorialStage
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class KitchenScript @Inject constructor(private val tutorial: TutorialIsland) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(FLOUR, WATER) { makeDough() }
        onOpLoc1("loc.newbierange") { bakeBread() }
    }

    private suspend fun ProtectedAccess.makeDough() {
        if (player.tutorialStage < TutorialStage.MAKE_DOUGH) {
            tutorial.refresh(player)
            return
        }
        if (!mixDough()) {
            mes("You don't have enough inventory space to do that.")
            return
        }
        mes("You make some dough.")
        ClientScripts.invHighlightClearAll(player)
        if (player.tutorialStage == TutorialStage.MAKE_DOUGH) {
            tutorial.advance(player, TutorialStage.BAKE_BREAD)
        }
        objbox(DOUGH, "You make some dough.")
    }

    private fun ProtectedAccess.mixDough(): Boolean {
        val result =
            player.invTransaction(inv) {
                val backpack = select(inv)
                delete {
                    from = backpack
                    obj = FLOUR.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                delete {
                    from = backpack
                    obj = WATER.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                insert {
                    into = backpack
                    obj = DOUGH.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                insert {
                    into = backpack
                    obj = "obj.pot_empty".asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                insert {
                    into = backpack
                    obj = "obj.bucket_empty".asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        return result.success
    }

    private suspend fun ProtectedAccess.bakeBread() {
        if (player.tutorialStage < TutorialStage.BAKE_BREAD || !inv.contains(DOUGH)) {
            tutorial.refresh(player)
            return
        }
        tutorial.mesOverlay(
            player,
            "Your character is now attempting to bake some bread. This will only take a few " +
                "seconds.",
        )
        anim("seq.human_cooking")
        delay(BAKING_TICKS)
        val replaced = invReplace(inv, DOUGH, 1, "obj.bread")
        if (replaced.failure) {
            resetAnim()
            return
        }
        statAdvance("stat.cooking", BREAD_XP)
        mes("You manage to bake some bread.")
        if (player.tutorialStage == TutorialStage.BAKE_BREAD) {
            tutorial.advance(player, TutorialStage.LEAVE_KITCHEN)
        } else {
            tutorial.refresh(player)
        }
        objbox("obj.bread", "You manage to bake some bread.")
    }

    private companion object {
        const val FLOUR = "obj.newbie_pot_flour"
        const val WATER = "obj.bucket_water"
        const val DOUGH = "obj.bread_dough"

        const val BAKING_TICKS = 5
        const val BREAD_XP = 40.0
    }
}
