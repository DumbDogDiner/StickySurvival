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
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.WeakHashMap

class GameSpawnPointComponent(game: Game) : GameComponent(game) {
    private val free = game.config.spawnPoints.map {
        it.apply {
            world = game.world
        }
    }.toMutableSet()

    private val used = WeakHashMap<Player, Location>()

    fun givePlayerSpawnPoint(player: Player): Boolean {
        val selected = free.randomOrNull() ?: return false
        free -= selected
        used += player to selected
        return if (player.teleport(selected)) {
            true
        } else {
            used -= player
            free += selected
            false
        }
    }

    fun takePlayerSpawnPoint(player: Player) {
        val selected = used.remove(player) ?: return
        free += selected
    }

    fun getPlayerSpawnPoint(player: Player): Location? {
        return used[player]
    }

    fun getSpaceLeft() = free.size
}
