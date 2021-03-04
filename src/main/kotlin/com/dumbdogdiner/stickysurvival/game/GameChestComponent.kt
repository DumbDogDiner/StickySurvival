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
import com.dumbdogdiner.stickysurvival.task.ChestRefillRunnable
import com.dumbdogdiner.stickysurvival.util.broadcastMessage
import com.dumbdogdiner.stickysurvival.util.game
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.settings
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.DoubleChestInventory

class GameChestComponent(game: Game) : GameComponent(game) {
    var refillCount = 1
    private val filledChests = mutableMapOf<Location, Int>()
    private val currentlyOpenChests = mutableSetOf<Location>()
    private val chestRefill = ChestRefillRunnable(this)

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as Player
        if (player.world.game == game) {
            if (game.phase != Game.Phase.WAITING && player in game.tributesComponent) {
                val location = event.inventory.location ?: return
                val type = game.world.getBlockAt(location).type
                if (type == Material.CHEST || type in settings.bonusContainers) {
                    currentlyOpenChests += location
                    maybeFill(location)
                }
            }
        }
    }

    @EventHandler
    fun onChestClose(event: InventoryCloseEvent) {
        if (event.player.world == game.world) {
            event.inventory.location?.let {
                currentlyOpenChests -= it
            }
        }
    }

    private fun maybeFill(location: Location) {
        if (filledChests[location]?.let { it >= refillCount } == true) return // chest needs no refills

        val chest = game.world.getBlockAt(location).state as Container

        val loot = when (chest.type) {
            in settings.bonusContainers -> settings.bonusLoot
            else -> settings.basicLoot
        }

        val inventories = if (chest.inventory is DoubleChestInventory) {
            val inv = chest.inventory as DoubleChestInventory
            listOf(inv.leftSide, inv.rightSide)
        } else {
            listOf(chest.inventory)
        }

        for (inventory in inventories) {
            val loc = inventory.location ?: location
            var i = filledChests[loc] ?: 0
            while (i < refillCount) {
                loot.insertItems(inventory)
                i += 1
            }
            filledChests[loc] = refillCount
        }
    }

    fun refill() {
        refillCount += 1
        currentlyOpenChests.forEach { maybeFill(it) }
        game.world.broadcastMessage(messages.chat.refill)
    }

    @EventHandler
    fun startTask(event: GameEnableDamageEvent) {
        if (game.config.chestRefill > 0) {
            chestRefill.maybeRunTaskTimer(game.config.chestRefill, game.config.chestRefill)
        }
    }

    @EventHandler
    fun stopTask(event: GameCloseEvent) {
        if (event.game == game) {
            chestRefill.safelyCancel()
        }
    }
}
