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

package com.dumbdogdiner.stickysurvival.task

import com.dumbdogdiner.stickysurvival.StickySurvival
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

abstract class SafeRunnable : BukkitRunnable() {
    fun maybeRunTask() = ifNotScheduled {
        runTask(StickySurvival.instance)
    }

    fun maybeRunTaskAsync() = ifNotScheduled {
        runTaskAsynchronously(StickySurvival.instance)
    }

    fun maybeRunTaskLater(delay: Long) = ifNotScheduled {
        runTaskLater(StickySurvival.instance, delay * 20)
    }

    fun maybeRunTaskTimer(delay: Long, period: Long) = ifNotScheduled {
        runTaskTimer(StickySurvival.instance, delay * 20, period * 20)
    }

    fun maybeRunTaskEveryTick() = ifNotScheduled {
        runTaskTimer(StickySurvival.instance, 0, 1)
    }

    fun safelyCancel() = ifScheduled { id ->
        Bukkit.getScheduler().cancelTask(id)

        // this doesn't feel right, but there's no other way to do it from what i can tell
        val taskField = BukkitRunnable::class.java.getDeclaredField("task")
        taskField.isAccessible = true
        taskField.set(this, null)
    }

    private fun maybeTaskId() = try { taskId } catch (_: IllegalStateException) { null }

    private fun ifScheduled(f: (Int) -> Unit) {
        f(maybeTaskId() ?: return) // run if taskId is not null
    }

    private fun ifNotScheduled(f: () -> Unit) {
        maybeTaskId() ?: f() // run if taskId is null
    }
}
