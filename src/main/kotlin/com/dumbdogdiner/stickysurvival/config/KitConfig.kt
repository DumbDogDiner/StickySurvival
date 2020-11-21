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

import com.dumbdogdiner.stickysurvival.util.clearPotionEffects
import com.dumbdogdiner.stickysurvival.util.effectFromConfig
import com.dumbdogdiner.stickysurvival.util.itemFromConfig
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect

class KitConfig(
    val name: String,
    private val helmet: ItemStack?,
    private val chestplate: ItemStack?,
    private val leggings: ItemStack?,
    private val boots: ItemStack?,
    private val items: List<ItemStack>,
    private val effects: List<PotionEffect>,
) {
    constructor(name: String, cfg: ConfigHelper) : this(
        name,
        cfg["helmet"].maybe { itemFromConfig(it) },
        cfg["chestplate"].maybe { itemFromConfig(it) },
        cfg["leggings"].maybe { itemFromConfig(it) },
        cfg["boots"].maybe { itemFromConfig(it) },
        cfg["items"].maybeGet()?.map { itemFromConfig(it) } ?: emptyList(),
        cfg["effects"].maybeGet()?.map { effectFromConfig(it) } ?: emptyList(),
    )

    fun giveTo(player: Player) {
        player.inventory.let {
            it.clear()
            it.helmet = helmet
            it.chestplate = chestplate
            it.leggings = leggings
            it.boots = boots
            it.addItem(*items.toTypedArray())
        }

        player.clearPotionEffects() // necessary?
        for (effect in effects) {
            player.addPotionEffect(effect)
        }
    }
}
