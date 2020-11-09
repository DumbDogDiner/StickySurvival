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

package com.dumbdogdiner.stickysurvival.task

import com.dumbdogdiner.stickysurvival.Game
import com.dumbdogdiner.stickysurvival.util.broadcastMessage
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.random
import com.dumbdogdiner.stickysurvival.util.safeFormat
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import kotlin.math.roundToInt

class RandomDropRunnable(game: Game) : GameRunnable(game) {
    override fun run() {
        val x = game.config.xBounds.random().roundToInt()
        val y = game.config.yBounds.endInclusive
        val z = game.config.zBounds.random().roundToInt()
        val location = Location(game.world, x.toDouble() + 0.5, y, z.toDouble() + 0.5)
        game.world.broadcastMessage(messages.chat.randomChestDrop.safeFormat(x, z))
        game.world.spawnFallingBlock(location, Bukkit.getServer().createBlockData(Material.DRIED_KELP_BLOCK))
    }
}
