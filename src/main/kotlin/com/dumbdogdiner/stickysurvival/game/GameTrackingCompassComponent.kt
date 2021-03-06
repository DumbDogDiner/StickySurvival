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
import com.dumbdogdiner.stickysurvival.StickySurvival
import com.dumbdogdiner.stickysurvival.event.GameCloseEvent
import com.dumbdogdiner.stickysurvival.event.GameStartEvent
import com.dumbdogdiner.stickysurvival.task.TrackingCompassRunnable
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler

class GameTrackingCompassComponent(game: Game) : GameComponent(game) {
    private val task = TrackingCompassRunnable(game)

    init {
        Bukkit.getPluginManager().registerEvents(this, StickySurvival.instance)
    }

    @EventHandler
    fun startTask(event: GameStartEvent) {
        if (event.game == game) {
            task.maybeRunTaskEveryTick()
        }
    }

    @EventHandler
    fun stopTask(event: GameCloseEvent) {
        if (event.game == game) {
            task.safelyCancel()
        }
    }
}
