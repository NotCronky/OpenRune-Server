package org.rsmod.content.other.playerbots

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.rsprot.protocol.message.IncomingGameMessage
import org.rsmod.api.net.rsprot.provider.MessageConsumerProvider
import org.rsmod.game.entity.Player

/** Hands bot messages to the same consumers rsprot uses for a real client's packets. */
@Singleton
class BotInput @Inject constructor(private val provider: MessageConsumerProvider) {
    private val repository by lazy { provider.get().provide() }

    fun consume(player: Player, message: IncomingGameMessage) {
        val consumer =
            checkNotNull(repository.consumers[message.javaClass]) {
                "Consumer for message $message does not exist."
            }
        consumer.consume(player, message)
        for (global in repository.globalConsumers) {
            global.consume(player, message)
        }
    }
}
