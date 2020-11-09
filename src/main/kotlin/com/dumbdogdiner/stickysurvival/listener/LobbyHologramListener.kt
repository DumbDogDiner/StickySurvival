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

package com.dumbdogdiner.stickysurvival.listener // package com.dumbdogdiner.stickysurvival.listener

import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.spawn
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent

object LobbyHologramListener : Listener {
    @EventHandler
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        if (event.rightClicked.type == EntityType.ARMOR_STAND) {
            val hologram = WorldManager.getHolograms().find { event.rightClicked in it.armorStands }

            if (hologram != null) {
                when {
                    WorldManager.playerJoinCooldownExists(event.player) -> {
                        event.player.sendMessage(messages.chat.cooldown)
                    }
                    WorldManager.isPlayerWaitingToJoin(event.player) -> {
                        // i don't feel that this is important enough for its own message
                    }
                    else -> {
                        event.player.sendMessage(messages.chat.joining.safeFormat(hologram.config.friendlyName))
                        spawn {
                            try {
                                WorldManager.putPlayerInWorldNamed(event.player, hologram.worldName)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }
}
