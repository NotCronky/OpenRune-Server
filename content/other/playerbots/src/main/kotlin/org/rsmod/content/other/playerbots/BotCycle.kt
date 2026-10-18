package org.rsmod.content.other.playerbots

import io.netty.util.ReferenceCountUtil
import net.rsprot.protocol.game.incoming.misc.client.MapBuildComplete
import net.rsprot.protocol.game.outgoing.map.RebuildRegionV2
import net.rsprot.protocol.game.outgoing.map.StaticRebuildMessageV2
import net.rsprot.protocol.message.OutgoingGameMessage
import org.rsmod.api.net.rsprot.RspCycle
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.game.client.ClientCycle
import org.rsmod.game.entity.Player

/**
 * Runs the avatar half of [RspCycle] so other players see the bot, but releases the info packets
 * computed for the bot instead of sending them anywhere. Map rebuilds are answered with the
 * `MapBuildComplete` a real client sends once it has loaded the scene.
 */
class BotCycle private constructor(private val sync: RspCycle) : ClientCycle by sync {
    fun init(player: Player) {
        sync.init(player)
    }

    override fun flush(player: Player) {
        sync.release()
    }

    companion object {
        fun create(client: BotClient, regions: RegionRegistry): BotCycle {
            val sync = RspCycle({ message -> client.onCycleMessage(message) }, client.infos, regions)
            return BotCycle(sync)
        }

        private fun BotClient.onCycleMessage(message: OutgoingGameMessage) {
            if (message is StaticRebuildMessageV2 || message is RebuildRegionV2) {
                queue(MapBuildComplete)
            }
            ReferenceCountUtil.release(message)
        }
    }
}
