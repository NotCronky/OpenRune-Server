package org.rsmod.content.other.playerbots.brain

import kotlin.math.abs
import kotlin.math.max
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.random.GameRandom
import org.rsmod.content.other.playerbots.brain.goals.RecoverGoal
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionLoc
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid

/**
 * One bot's decision loop. It only re-decides every few ticks, with a human-ish reaction delay
 * and the odd longer distraction, and works on one [BotGoal] at a time.
 */
class BotBrain
internal constructor(
    val player: Player,
    val world: BotWorld,
    val random: GameRandom,
    private val clock: MapClock,
    private val chat: BotChat,
) {
    val now: Int
        get() = clock.cycle

    var goal: BotGoal? = null
        private set

    /** How the previous goal ended, e.g. "fishing at draynor: Failed", for `::bot status`. */
    var lastEnded: String? = null
        private set

    /** Where the bot logged in; idle wandering stays around it. */
    val home: CoordGrid = player.coords

    private var nextThink = 0
    private var nextFaceRefresh = now + random.of(FACE_REFRESH_TICKS)
    private val chattiness = random.of(0, MAX_CHATTINESS)
    private val eatPercent = random.of(EAT_PERCENT_MIN, EAT_PERCENT_MAX)
    private var lastTickCoords: CoordGrid = player.coords

    /**
     * Whether the bot is mid-action: walking, interacting, delayed, or running a skill loop on a
     * weak queue (fishing between catches; any new action would cancel it).
     */
    val busy: Boolean
        get() = player.isBusy2 || player.isDelayed || player.weakQueueList.isNotEmpty

    internal fun think(pickGoal: (BotBrain) -> BotGoal) {
        if (now < nextThink || player.manualLogout) {
            return
        }
        nextThink = now + reactionDelay()
        if (eatIfHurt()) {
            return
        }
        val current = goal ?: pickGoal(this).also { goal = it }
        if (player.ui.modals.isNotEmpty() && !current.ownsModal(this)) {
            BotActions.closeModal(player)
            return
        }
        val state = current.tick(this)
        if (state != GoalState.Running) {
            lastEnded = "${current.name}: $state"
            goal = null
        }
        if (random.of(CHAT_ODDS) < chattiness) {
            BotActions.say(player, chat.line(random))
        }
    }

    internal fun force(goal: BotGoal) {
        this.goal = goal
    }

    fun hasFood(): Boolean = foodSlot() != null

    /** Called every tick; the bot stands still while dying, so this ends up on the death tile. */
    internal fun trackPosition() {
        lastTickCoords = player.coords
    }

    /** After the respawn teleport: drop whatever the bot was doing and go back for its items. */
    internal fun onRespawn() {
        goal = RecoverGoal(lastTickCoords, now + RECOVER_TICKS)
    }

    private fun eatIfHurt(): Boolean {
        val hp = player.hitpoints
        if (hp == 0 || hp * 100 >= player.baseHitpointsLvl * eatPercent) {
            return false
        }
        val slot = foodSlot() ?: return false
        return BotActions.heldOp(player, slot, EAT)
    }

    private fun foodSlot(): Int? =
        player.inv.indices.firstOrNull { slot ->
            player.inv[slot]?.let { obj ->
                getInvObj(obj).interfaceOptions.any { it.equals(EAT, ignoreCase = true) }
            } == true
        }

    /**
     * Turns the bot to the loc it's working on again every few ticks. rsprot only sends a stored
     * face angle to observers that have seen the player before, so a client that first sees a bot
     * mid-action would otherwise show it facing south.
     */
    internal fun refreshFacing() {
        if (now < nextFaceRefresh) {
            return
        }
        nextFaceRefresh = now + FACE_REFRESH_TICKS
        val interaction = player.interaction as? InteractionLoc ?: return
        if (player.routeDestination.isEmpty()) {
            player.faceLoc(interaction.target)
        }
    }

    /**
     * Walks towards [dest] in hops of at most [MAX_HOP] tiles, so long walks stay inside the
     * route finder's search area. Returns false when no walkable tile is found for the hop.
     */
    fun walkToward(dest: CoordGrid): Boolean {
        val from = player.coords
        val dx = dest.x - from.x
        val dz = dest.z - from.z
        val distance = max(abs(dx), abs(dz))
        val hop =
            if (distance <= MAX_HOP) {
                dest
            } else {
                from.translate(dx * MAX_HOP / distance, dz * MAX_HOP / distance)
            }
        val tile =
            if (world.isWalkable(hop)) hop else world.walkableNear(hop, HOP_SLACK, random)
        if (tile == null) {
            return false
        }
        BotActions.walk(player, tile)
        return true
    }

    private fun reactionDelay(): Int =
        if (random.of(100) < DISTRACTED_PERCENT) {
            random.of(DISTRACTED_MIN, DISTRACTED_MAX)
        } else {
            random.of(REACTION_MIN, REACTION_MAX)
        }

    private companion object {
        private const val REACTION_MIN = 1
        private const val REACTION_MAX = 3
        private const val DISTRACTED_PERCENT = 4
        private const val DISTRACTED_MIN = 5
        private const val DISTRACTED_MAX = 15
        private const val MAX_HOP = 14
        private const val HOP_SLACK = 3
        private const val MAX_CHATTINESS = 4
        private const val CHAT_ODDS = 2_500
        private const val FACE_REFRESH_TICKS = 8
        private const val RECOVER_TICKS = 1_000
        private const val EAT = "Eat"
        private const val EAT_PERCENT_MIN = 40
        private const val EAT_PERCENT_MAX = 60
    }
}
