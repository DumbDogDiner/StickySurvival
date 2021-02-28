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
import com.dumbdogdiner.stickysurvival.task.RandomDropRunnable
import com.dumbdogdiner.stickysurvival.util.settings
import com.google.common.collect.HashBiMap
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

class GameCarePackageComponent(val game: Game) {
    private val chests = HashBiMap.create<Location, Inventory>()
    private val task = RandomDropRunnable(game)

    fun startTask() {
        task.maybeRunTaskTimer(settings.randomChestInterval, settings.randomChestInterval)
    }

    fun stopTask() {
        task.safelyCancel()
    }

    /**
     * Get or create a random chest inventory at the given location.
     * @param location Where to consider this inventory
     * @return A new or existing inventory, remembered to be at the given location
     */
    operator fun get(location: Location): Inventory {
        return chests[location] ?: run {
            val inv = Bukkit.createInventory(null, InventoryType.CHEST)
            settings.randomChestLoot.insertItems(inv)
            chests[location] = inv
            inv
        }
    }

    /**
     * Remove an inventory.
     * @param inv The inventory to remove
     */
    operator fun minusAssign(inv: Inventory) {
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

    /**
     * Check if this component contains a given inventory
     * @param inv The inventory to search for
     * @return True if this component contains the given inventory
     */
    operator fun contains(inv: Inventory) = chests.containsValue(inv)
}
