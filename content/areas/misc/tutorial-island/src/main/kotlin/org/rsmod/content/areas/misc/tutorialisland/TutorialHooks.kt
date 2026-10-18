package org.rsmod.content.areas.misc.tutorialisland

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.player.hook.PlayerInvUpdateHook
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid

internal class TutorialAttackHook @Inject constructor(private val tutorial: TutorialIsland) :
    NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!player.inTutorial) {
            return NpcAttackValidateResult.Pass
        }
        return when {
            npc.isType(GIANT_RAT) -> attackRat(player)
            npc.isType(CHICKEN) -> attackChicken(player)
            else -> NpcAttackValidateResult.Pass
        }
    }

    private fun attackRat(player: Player): NpcAttackValidateResult =
        when (player.tutorialStage) {
            TutorialStage.ATTACK_RAT -> {
                tutorial.advance(player, TutorialStage.FIGHTING_RAT)
                NpcAttackValidateResult.Pass
            }
            TutorialStage.RANGE_RAT -> {
                tutorial.advance(player, TutorialStage.RANGING_RAT)
                NpcAttackValidateResult.Pass
            }
            TutorialStage.RAT_KILLED ->
                NpcAttackValidateResult.Deny(
                    "You've already done that. Perhaps you should move on to learn about ranged " +
                        "combat."
                )
            in TutorialStage.LEAVE_COMBAT until TutorialStage.COMPLETE -> ALREADY_DONE
            else -> NpcAttackValidateResult.Pass
        }

    private fun attackChicken(player: Player): NpcAttackValidateResult {
        val stage = player.tutorialStage
        if (stage == TutorialStage.CAST_WIND_STRIKE) {
            tutorial.advance(player, TutorialStage.SPELL_CAST)
            player.queue(LEARNING_THE_ROPES_COMPLETE_QUEUE, 1)
            return NpcAttackValidateResult.Pass
        }
        if (stage > TutorialStage.CAST_WIND_STRIKE) {
            return ALREADY_DONE
        }
        return NpcAttackValidateResult.Pass
    }

    private companion object {
        val ALREADY_DONE =
            NpcAttackValidateResult.Deny("You've already done that. Perhaps you should move on.")
    }
}

internal class TutorialKillHook @Inject constructor(private val tutorial: TutorialIsland) :
    NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val player = context.hero
        if (!player.inTutorial || !context.npc.isType(GIANT_RAT)) {
            return
        }
        when (player.tutorialStage) {
            TutorialStage.ATTACK_RAT,
            TutorialStage.FIGHTING_RAT -> tutorial.advance(player, TutorialStage.RAT_KILLED)
            TutorialStage.RANGE_RAT,
            TutorialStage.RANGING_RAT -> tutorial.advance(player, TutorialStage.LEAVE_COMBAT)
        }
    }
}

internal class TutorialEquipmentHook @Inject constructor(private val tutorial: TutorialIsland) :
    PlayerInvUpdateHook {
    override fun onInvUpdated(player: Player, inv: Inventory) {
        if (inv === player.worn && player.inTutorial) {
            tutorial.checkEquipment(player)
        }
    }
}

internal class TutorialTeleportHook @Inject constructor() : PlayerTeleportValidateHook {
    override fun validate(player: Player, type: TeleportType, areaChecker: AreaChecker): String? =
        if (player.inTutorial && player.tutorialStage < TutorialStage.HOME_TELEPORT) {
            "You cannot teleport from Tutorial Island just yet!"
        } else {
            null
        }
}

internal class TutorialLeaveHook @Inject constructor(private val tutorial: TutorialIsland) :
    PlayerPostTickHook {
    override fun onPostTick(player: Player) {
        val leaving = player.tutorialStage == TutorialStage.HOME_TELEPORT
        if (leaving && !player.coords.onTutorialIsland) {
            tutorial.complete(player)
        }
    }
}

internal class TutorialDeathHook @Inject constructor() : PlayerDeathHook {
    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        if (!context.player.inTutorial) {
            return null
        }
        return PlayerDeathHandling(
            keepCount = Int.MAX_VALUE,
            dropReceiver = null,
            dropDuration = 0,
            revealDelay = 0,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.KEEP,
        )
    }
}

internal class TutorialRespawnHook @Inject constructor() : PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? {
        if (!player.inTutorial) {
            return null
        }
        return if (player.coords.z in UNDERGROUND_Z) COMBAT_AREA else ISLAND_START
    }

    private companion object {
        val UNDERGROUND_Z = 9000..10000
        val COMBAT_AREA = CoordGrid(3106, 9508, 0)
    }
}

internal val ISLAND_START = CoordGrid(3094, 3107, 0)

internal const val LEARNING_THE_ROPES_COMPLETE_QUEUE = "queue.tutorial_learning_the_ropes_complete"
private const val GIANT_RAT = "npc.newbiegiantrat"
private const val CHICKEN = "npc.newbiechicken"
