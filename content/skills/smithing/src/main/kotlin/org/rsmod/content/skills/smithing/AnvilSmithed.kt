package org.rsmod.content.skills.smithing

import dev.openrune.types.ItemServerType
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

/** Published each time an anvil produces [product], after the bars are taken and XP is given. */
data class AnvilSmithed(val player: Player, val product: ItemServerType) : UnboundEvent
