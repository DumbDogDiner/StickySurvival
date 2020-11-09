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

package com.dumbdogdiner.stickysurvival.config

import com.dumbdogdiner.stickysurvival.util.itemFromConfig
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class LootConfig(
    val min: Int,
    val max: Int,
    val entries: List<ItemStack>
) {
    constructor(cfg: ConfigHelper) : this(
        cfg["min"].asInt(),
        cfg["max"].asInt(),
        cfg["entries"].map { entry ->
            val stack = itemFromConfig(entry)
            val weight = entry["weight"].asIntOr(1)
            (0 until weight).map { stack }
        }.flatten()
    )

    private fun generateItems(): List<ItemStack> = mutableListOf<ItemStack>().apply {
        repeat((min..max).random()) {
            add(entries.random().clone())
        }
    }

    fun insertItems(inv: Inventory) {
        for (itemStack in generateItems()) {
            val slot = inv.withIndex().filter { it.value == null }.map { it.index }.randomOrNull() ?: break
            inv.setItem(slot, itemStack)
        }
    }
}
