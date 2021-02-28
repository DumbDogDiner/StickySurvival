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
import com.dumbdogdiner.stickysurvival.util.settings
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.inventory.DoubleChestInventory
import kotlin.random.Random

class GameChestRemovalComponent(val game: Game) {
    private val visitedChunks = mutableSetOf<Long>()

    fun process(chunk: Chunk) {
        val ratio = game.config.chestRatio
        if (ratio >= 1.0) return // don't run this method if we aren't going to remove chests
        val chests = chunk.tileEntities.filter { it.type == Material.CHEST || it.type in settings.bonusContainers }
        if (chests.isEmpty()) return // ignore chunks with no chests
        if (visitedChunks.add(chunk.chunkKey)) {
            val cornucopia = game.config.cornucopia
            chests.asSequence()
                .filterNot { it is Chest && it.inventory is DoubleChestInventory } // ignore double chests
                .filter { cornucopia?.contains(it.location) != true } // ignore chests in cornucopia
                .forEach {
                    // randomly remove
                    if (Random.nextDouble() >= ratio) chunk.world.getBlockAt(it.location).type = Material.AIR
                }
        }
    }
}
