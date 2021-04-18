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

package com.dumbdogdiner.stickysurvival.game

import com.dumbdogdiner.stickysurvival.Game
import com.dumbdogdiner.stickysurvival.event.GameCloseEvent
import com.dumbdogdiner.stickysurvival.event.GameEnableDamageEvent
import com.dumbdogdiner.stickysurvival.task.RandomDropRunnable
import com.dumbdogdiner.stickysurvival.util.settings
import com.google.common.collect.HashBiMap
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

class GameCarePackageComponent(game: Game) : GameComponent(game) {
    private val chests = HashBiMap.create<Location, Inventory>()
    private val task = RandomDropRunnable(game)

    @EventHandler
    fun startTask(event: GameEnableDamageEvent) {
        if (event.game == game) {
            task.maybeRunTaskTimer(settings.randomChestInterval, settings.randomChestInterval)
        }
    }

    @EventHandler
    fun stopTask(event: GameCloseEvent) {
        if (event.game == game) {
            task.safelyCancel()
        }
    }

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as Player
        // if event is for this game
        if (player.world == game.world) {
            // if game is not in waiting phase and the player is a tribute
            if (game.phase != Game.Phase.WAITING && player in game.tributesComponent) {
                // if the block is an ender chest
                val location = event.inventory.location ?: return
                if (game.world.getBlockAt(location).type == Material.ENDER_CHEST) {
                    // open new or existing care package inventory
                    event.isCancelled = true
                    if (location !in chests) {
                        val inv = Bukkit.createInventory(null, InventoryType.CHEST)
                        settings.randomChestLoot.insertItems(inv)
                        chests[location] = inv
                    }
                    event.player.openInventory(chests[location]!!)
                }
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.player.world == game.world) {
            val inv = event.inventory
            if (chests.containsValue(inv) && inv.viewers.none { it != event.player }) {
                // Get location or return
                val location = chests.inverse()[inv] ?: return
                // Remove from map
                chests -= location
                // Delete block
                game.world.getBlockAt(location).type = Material.AIR
                // drop items
                inv.filterNotNull().forEach {
                    game.world.dropItemNaturally(location, it)
                }
                // kaboom!
                game.world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f)
                game.world.spawnParticle(Particle.EXPLOSION_NORMAL, location, 10)
            }
        }
    }
}
