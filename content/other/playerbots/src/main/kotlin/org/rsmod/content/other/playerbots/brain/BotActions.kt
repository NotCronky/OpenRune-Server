package org.rsmod.content.other.playerbots.brain

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.incoming.locs.OpLocV2
import net.rsprot.protocol.game.incoming.messaging.MessagePublic
import net.rsprot.protocol.game.incoming.misc.user.CloseModal
import net.rsprot.protocol.game.incoming.misc.user.MoveGameClick
import net.rsprot.protocol.game.incoming.npcs.OpNpcV2
import net.rsprot.protocol.game.incoming.objs.OpObjV2
import net.rsprot.protocol.message.IncomingGameMessage
import net.rsprot.protocol.util.CombinedId
import org.rsmod.content.other.playerbots.BotClient
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid

/**
 * Everything a bot does goes through here as the message a real client would send, so it takes
 * the normal handler path with the normal validation.
 */
internal object BotActions {
    fun walk(player: Player, dest: CoordGrid) {
        player.send(MoveGameClick(dest.x, dest.z, 0))
    }

    fun opLoc(player: Player, loc: LocInfo, op: Int) {
        player.send(OpLocV2(loc.id, loc.coords.x, loc.coords.z, false, op, 0))
    }

    fun opNpc(player: Player, npc: Npc, op: Int) {
        player.send(OpNpcV2(npc.slotId, false, op, 0))
    }

    fun takeObj(player: Player, obj: Obj) {
        player.send(OpObjV2(obj.type, obj.x, obj.z, false, TAKE_OP, 0))
    }

    /**
     * Clicks the inventory option named [option] (e.g. "Eat", "Bury", "Wield") on [slot]. Returns
     * false when the obj there has no such option.
     */
    fun heldOp(player: Player, slot: Int, option: String): Boolean {
        val obj = player.inv[slot] ?: return false
        val options = getInvObj(obj).interfaceOptions
        val index = options.indexOfFirst { it.equals(option, ignoreCase = true) }
        if (index !in HELD_BUTTON_OPS.indices) {
            return false
        }
        button(player, INVENTORY_ITEMS, sub = slot, obj = obj.id, op = HELD_BUTTON_OPS[index])
        return true
    }

    fun button(player: Player, component: String, sub: Int = -1, obj: Int = -1, op: Int = 1) {
        val type = ServerCacheManager.fromComponent(component.asRSCM(RSCMType.COMPONENT))
        player.send(If3Button(CombinedId(type.interfaceId, type.component), sub, obj, op))
    }

    fun closeModal(player: Player) {
        player.send(CloseModal)
    }

    fun say(player: Player, text: String) {
        player.send(MessagePublic(0, 0, 0, text, null, -1))
    }

    private fun Player.send(message: IncomingGameMessage) {
        (client as? BotClient)?.queue(message)
    }

    private const val TAKE_OP = 3
    private const val INVENTORY_ITEMS = "component.inventory:items"

    /** The If3Button op the inventory sends for each held op (iop1..iop5). */
    private val HELD_BUTTON_OPS = intArrayOf(2, 3, 4, 6, 7)
}
