/*
 * StickySurvival - an implementation of the Survival Games minigame
 * Copyright (C) 2020 Dumb Dog Diner <dumbdogdiner.com>
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

package com.dumbdogdiner.stickysurvival.listener

import com.dumbdogdiner.stickysurvival.util.warn
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

object PerWorldChatListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onAsyncChat(event: AsyncChatEvent) {
        if (event.isCancelled) return
        val recipients = event.recipients()
        val playersToHideMessageFrom = recipients.filter { it.world != event.player.world }
        try {
            for (player in playersToHideMessageFrom) {
                recipients -= player
            }
        } catch (e: UnsupportedOperationException) {
            warn("Unable to modify recipients for message: ${event.message()}")
            e.printStackTrace()
        }
    }
}
