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

package com.dumbdogdiner.stickysurvival.manager

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.WeakHashMap

object LobbyInventoryManager {
    private val savedInventories = WeakHashMap<Player, Array<ItemStack>>()

    fun saveInventory(player: Player) {
        savedInventories[player] = player.inventory.contents.clone()
    }

    fun restoreInventory(player: Player) {
        val inv = player.inventory
        val savedInv = savedInventories[player] ?: return
        for ((index, item) in savedInv.withIndex()) {
            inv.setItem(index, item)
        }
    }

    fun unloadInventory(player: Player) {
        savedInventories -= player
    }
}
