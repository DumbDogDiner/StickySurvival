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
import com.dumbdogdiner.stickysurvival.StickySurvival
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

abstract class GameRunnable(protected val game: Game) : BukkitRunnable() {
    fun runTask() {
        runTask(StickySurvival.instance)
    }

    fun runTaskAsync() {
        runTaskAsynchronously(StickySurvival.instance)
    }

    fun runTaskLater(delay: Long) {
        runTaskLater(StickySurvival.instance, delay * 20)
    }

    fun runTaskTimer(delay: Long, period: Long) {
        runTaskTimer(StickySurvival.instance, delay * 20, period * 20)
    }

    fun safelyCancel() {
        val id = try {
            taskId
        } catch (_: IllegalStateException) {
            return
        }

        Bukkit.getScheduler().cancelTask(id)

        // this doesn't feel right, but there's no other way to do it from what i can tell
        val taskField = BukkitRunnable::class.java.getDeclaredField("task")
        taskField.isAccessible = true
        taskField.set(this, null)
    }
}
