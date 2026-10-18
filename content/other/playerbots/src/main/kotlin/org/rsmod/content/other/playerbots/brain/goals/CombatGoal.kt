package org.rsmod.content.other.playerbots.brain.goals

import dev.openrune.util.WeaponCategory
import dev.openrune.util.Wearpos
import kotlin.math.abs
import kotlin.math.max
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.attackLvl
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.defenceLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.strengthLvl
import org.rsmod.api.table.combat.PlayerbotCombatLootRow
import org.rsmod.api.table.combat.PlayerbotCombatSpotsRow
import org.rsmod.content.other.playerbots.brain.BotActions
import org.rsmod.content.other.playerbots.brain.BotBrain
import org.rsmod.content.other.playerbots.brain.BotGear
import org.rsmod.content.other.playerbots.brain.BotGoal
import org.rsmod.content.other.playerbots.brain.GoalState
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.getOrNull
import org.rsmod.map.CoordGrid

/**
 * Fights the npcs at [spot] until [endsAt]: picks a style for its weakest combat stat, attacks
 * npcs nobody else is fighting, picks up and buries the loot, and runs off when it's low with no
 * food left (eating itself is the brain's job, whatever the goal).
 */
class CombatGoal(private val spot: PlayerbotCombatSpotsRow, private val endsAt: Int) : BotGoal {
    private val tried = mutableSetOf<Int>()
    private val skippedNpcs = mutableSetOf<Int>()
    private val skippedObjs = mutableSetOf<Obj>()
    private var styleChosen = false
    private var target: Npc? = null
    private var attackedAt = 0
    private var lootTarget: Obj? = null
    private var lootAttempts = 0
    private var idleThinks = 0
    private var lastCoords: CoordGrid? = null

    override fun tick(brain: BotBrain): GoalState {
        val player = brain.player
        if (player.hitpoints == 0) {
            return GoalState.Running
        }
        if (hpPercent(player) <= FLEE_PERCENT && !brain.hasFood()) {
            return flee(brain)
        }
        if (brain.busy) {
            idleThinks = 0
            return GoalState.Running
        }
        if (stuck(player)) {
            return GoalState.Failed
        }
        if (BotGear.wieldSpare(player, tried)) {
            return GoalState.Running
        }
        if (outOfAmmo(player)) {
            return GoalState.Done
        }
        if (!styleChosen) {
            styleChosen = true
            if (chooseStyle(brain)) {
                return GoalState.Running
            }
        }
        if (restockAmmo(player) || bury(player) || loot(brain)) {
            return GoalState.Running
        }
        if (brain.now >= endsAt) {
            return GoalState.Done
        }
        return attack(brain)
    }

    private fun attack(brain: BotBrain): GoalState {
        val player = brain.player
        if (player.coords.chebyshevDistance(spot.centre) > spot.radius) {
            return if (brain.walkToward(spot.centre)) GoalState.Running else GoalState.Failed
        }
        val current = target
        if (current != null && current.isValidTarget() && isFree(brain, current)) {
            if (brain.now - attackedAt > ATTACK_TIMEOUT && !engaged(current)) {
                skippedNpcs += current.slotId
                target = null
                return GoalState.Running
            }
            return opAttack(brain, current)
        }
        val npc = pickTarget(brain)
        if (npc == null) {
            brain.walkToward(spot.centre)
            return GoalState.Running
        }
        target = npc
        attackedAt = brain.now
        return opAttack(brain, npc)
    }

    private fun opAttack(brain: BotBrain, npc: Npc): GoalState {
        val op = attackOp(npc) ?: return GoalState.Failed
        BotActions.opNpc(brain.player, npc, op)
        return GoalState.Running
    }

    private fun pickTarget(brain: BotBrain): Npc? {
        val player = brain.player
        val npcs =
            brain.world.npcsNear(spot.centre, spot.radius) { npc ->
                npc.name.equals(spot.npc, ignoreCase = true) &&
                    npc.slotId !in skippedNpcs &&
                    npc.isValidTarget() &&
                    attackOp(npc) != null &&
                    isFree(brain, npc)
            }
        if (npcs.isEmpty()) {
            skippedNpcs.clear()
            return null
        }
        val nearest = npcs.sortedBy { it.coords.chebyshevDistance(player.coords) }
        return brain.random.pick(nearest.take(NEAREST_TARGETS))
    }

    /** Not fighting someone else; roughly the single-combat check in `PvNCombatScript`. */
    private fun isFree(brain: BotBrain, npc: Npc): Boolean =
        npc.vars[NPC_LAST_COMBAT] + ACTIVE_COMBAT_DELAY < brain.now ||
            (npc == target && engaged(npc))

    private fun engaged(npc: Npc): Boolean = npc.vars[NPC_LAST_COMBAT] >= attackedAt

    private fun attackOp(npc: Npc): Int? =
        (0 until NPC_OPS).firstOrNull {
            npc.visType.actions.getOpOrNull(it).equals(ATTACK, ignoreCase = true)
        }?.plus(1)

    /** Walks away from the fight; the goal ends and the brain rests until it has healed. */
    private fun flee(brain: BotBrain): GoalState {
        val player = brain.player
        val from = target?.coords ?: spot.centre
        val dx = (player.coords.x - from.x).let { if (it == 0) brain.random.of(-1, 1) else it }
        val dz = (player.coords.z - from.z).let { if (it == 0) brain.random.of(-1, 1) else it }
        val scale = max(max(abs(dx), abs(dz)), 1)
        val dest =
            player.coords.translate(dx * FLEE_DISTANCE / scale, dz * FLEE_DISTANCE / scale)
        brain.walkToward(dest)
        return GoalState.Done
    }

    /**
     * Picks the style for the lowest melee stat it trains, or rapid for a bow. A pure (Defence 1
     * with trained Attack or Strength) never trains Defence. Returns false when the bot already
     * fights in that style.
     */
    private fun chooseStyle(brain: BotBrain): Boolean {
        val player = brain.player
        val button =
            if (isRanged(player)) {
                if (brain.random.of(100) < RAPID_PERCENT) RAPID_BUTTON else ACCURATE_BUTTON
            } else {
                val trains = buildList {
                    add(ACCURATE_BUTTON to player.attackLvl)
                    add(AGGRESSIVE_BUTTON to player.strengthLvl)
                    val pure = player.defenceLvl <= 1 &&
                        max(player.attackLvl, player.strengthLvl) >= PURE_LEVEL
                    if (!pure) {
                        add(DEFENSIVE_BUTTON to player.defenceLvl)
                    }
                }
                val lowest = trains.minOf { it.second }
                brain.random.pick(trains.filter { it.second == lowest }).first
            }
        if (player.vars[COMBAT_STANCE] == button) {
            return false
        }
        BotActions.button(player, "$STANCE_COMPONENT$button")
        return true
    }

    private fun isRanged(player: Player): Boolean =
        getOrNull(player.righthand)?.weaponCategory in RANGED_CATEGORIES

    private fun outOfAmmo(player: Player): Boolean =
        getOrNull(player.righthand)?.weaponCategory in AMMO_CATEGORIES &&
            player.worn[Wearpos.Quiver.slot] == null

    private fun restockAmmo(player: Player): Boolean {
        val ammo = player.worn[Wearpos.Quiver.slot] ?: return false
        val slot = player.inv.indices.firstOrNull { player.inv[it]?.id == ammo.id } ?: return false
        return BotActions.heldOp(player, slot, WIELD)
    }

    private fun bury(player: Player): Boolean {
        val slot = player.inv.indices.firstOrNull { player.inv[it]?.id in buryIds } ?: return false
        return BotActions.heldOp(player, slot, BURY)
    }

    private fun loot(brain: BotBrain): Boolean {
        val player = brain.player
        val ammo = player.worn[Wearpos.Quiver.slot]?.id
        val obj =
            brain.world
                .objsNear(player.coords, LOOT_RANGE) { obj ->
                    obj !in skippedObjs &&
                        obj.isVisibleTo(player) &&
                        (obj.type in lootIds || obj.type == ammo) &&
                        obj.coords.chebyshevDistance(spot.centre) <= spot.radius + LOOT_RANGE &&
                        canHold(player, obj)
                }
                .minByOrNull { it.coords.chebyshevDistance(player.coords) } ?: return false
        if (obj == lootTarget) {
            if (++lootAttempts > MAX_LOOT_ATTEMPTS) {
                skippedObjs += obj
                lootTarget = null
                return false
            }
        } else {
            lootTarget = obj
            lootAttempts = 0
        }
        BotActions.takeObj(player, obj)
        return true
    }

    private fun canHold(player: Player, obj: Obj): Boolean =
        !player.inv.isFull() || player.inv.objs.any { it?.id == obj.type && getInvObj(it).stackable }

    private fun stuck(player: Player): Boolean {
        idleThinks = if (player.coords == lastCoords) idleThinks + 1 else 0
        lastCoords = player.coords
        return idleThinks > MAX_IDLE_THINKS
    }

    private fun hpPercent(player: Player): Int =
        player.hitpoints * 100 / player.baseHitpointsLvl.coerceAtLeast(1)

    companion object {
        private const val ATTACK = "Attack"
        private const val BURY = "Bury"
        private const val WIELD = "Wield"
        private const val NPC_OPS = 5
        private const val NEAREST_TARGETS = 3
        private const val ATTACK_TIMEOUT = 15
        private const val ACTIVE_COMBAT_DELAY = 8
        private const val FLEE_PERCENT = 25
        private const val FLEE_DISTANCE = 12
        private const val RAPID_PERCENT = 70
        private const val PURE_LEVEL = 20
        private const val LOOT_RANGE = 6
        private const val MAX_LOOT_ATTEMPTS = 3
        private const val MAX_IDLE_THINKS = 40

        private const val READY_PERCENT = 70

        private const val ACCURATE_BUTTON = 0
        private const val AGGRESSIVE_BUTTON = 1
        private const val RAPID_BUTTON = 1
        private const val DEFENSIVE_BUTTON = 3
        private const val STANCE_COMPONENT = "component.combat_interface:"
        private const val COMBAT_STANCE = "varp.com_mode"
        private const val NPC_LAST_COMBAT = "varn.lastcombat"

        private val AMMO_CATEGORIES = setOf(WeaponCategory.Bow, WeaponCategory.Crossbow)
        private val RANGED_CATEGORIES = AMMO_CATEGORIES + WeaponCategory.Thrown

        private val lootIds by lazy { PlayerbotCombatLootRow.all().map { it.obj.id }.toSet() }
        private val buryIds by lazy {
            PlayerbotCombatLootRow.all().filter { it.bury }.map { it.obj.id }.toSet()
        }

        fun readyToFight(player: Player): Boolean =
            player.hitpoints * 100 >= player.baseHitpointsLvl * READY_PERCENT
    }
}
