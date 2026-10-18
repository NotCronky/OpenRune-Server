package org.rsmod.content.other.playerbots

import io.netty.util.ReferenceCountUtil
import net.rsprot.protocol.api.NetworkService
import net.rsprot.protocol.game.outgoing.info.Infos
import net.rsprot.protocol.message.IncomingGameMessage
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player

class BotClient(val infos: Infos, private val input: BotInput) : Client<Any, Any> {
    private val pending = ArrayDeque<IncomingGameMessage>()

    fun queue(message: IncomingGameMessage) {
        pending.addLast(message)
    }

    override fun read(player: Player) {
        while (true) {
            val message = pending.removeFirstOrNull() ?: break
            input.consume(player, message)
        }
    }

    override fun write(message: Any) {
        ReferenceCountUtil.release(message)
    }

    override fun close() {}

    override fun flush() {}

    override fun flushHighPriority() {}

    @Suppress("UNCHECKED_CAST")
    override fun unregister(service: Any, player: Player) {
        (service as NetworkService<Player>).infoProtocols.dealloc(infos)
    }
}

val Player.isBot: Boolean
    get() = client is BotClient
