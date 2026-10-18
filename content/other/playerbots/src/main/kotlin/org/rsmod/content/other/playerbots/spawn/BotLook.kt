package org.rsmod.content.other.playerbots.spawn

import org.rsmod.game.entity.Player

/**
 * A bot's appearance. Account saves don't store appearance, so the look is derived from the
 * account name ([BotSpawner.look]) and applied again on every login.
 */
class BotLook(
    val bodyType: Int,
    val pronoun: Int,
    val identKit: List<Int>,
    val colours: List<Int>,
) {
    fun apply(player: Player) {
        player.appearance.bodyType = bodyType
        player.appearance.pronoun = pronoun
        identKit.forEachIndexed { slot, kit -> player.appearance.setIdentKit(slot, kit) }
        colours.forEachIndexed { slot, colour -> player.appearance.setColour(slot, colour) }
    }
}
