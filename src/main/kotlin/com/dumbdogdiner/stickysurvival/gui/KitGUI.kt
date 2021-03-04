/*
 * StickySurvival - an implementation of the Survival Games minigame
 * Copyright (C) 2021 Dumb Dog Diner <dumbdogdiner.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dumbdogdiner.stickysurvival.gui

import com.dumbdogdiner.stickyapi.bukkit.gui.GUI
import com.dumbdogdiner.stickyapi.bukkit.util.SoundUtil
import com.dumbdogdiner.stickysurvival.StickySurvival
import com.dumbdogdiner.stickysurvival.util.game
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.settings
import com.dumbdogdiner.stickysurvival.util.substituteAmpersand
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryOpenEvent
import kotlin.math.ceil

class KitGUI : GUI(
    // rows
    ceil(settings.kits.size.toFloat() / 9).toInt(),
    // name
    messages.misc.kitGuiTitle,
    // plugin
    StickySurvival.instance,
) {
    init {
        // for each kit
        for ((i, kit) in settings.kits.withIndex()) {
            // get the icon
            val item = kit.icon
            // if no custom name set, set the name to the kit's name
            if (!item.itemMeta.hasDisplayName()) {
                item.itemMeta = item.itemMeta.also {
                    it.displayName(Component.text(kit.name.substituteAmpersand()))
                }
            }
            // add slot to the GUI
            addSlot(i % 9, i / 9, item) { event, _ ->
                // get player or return
                val player = event.whoClicked as? Player ?: return@addSlot
                // get game or return
                val game = player.world.game ?: return@addSlot
                // set the kit
                game.tributesComponent.setKit(player, kit)
                // send a message
                player.sendMessage(messages.chat.kitSelect.safeFormat(kit.name))
                // close this GUI
                player.closeInventory()
                // ooh gui make sound
                SoundUtil.sendSuccess(player)
            }
        }
    }

    override fun onInventoryOpen(event: InventoryOpenEvent) {
        // sounds are fun!
        SoundUtil.sendQuiet(event.player as Player)
    }
}
