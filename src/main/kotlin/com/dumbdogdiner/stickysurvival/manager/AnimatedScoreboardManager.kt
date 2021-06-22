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

package com.dumbdogdiner.stickysurvival.manager

import org.bukkit.plugin.java.JavaPlugin
import java.lang.reflect.Method

object AnimatedScoreboardManager {
    var plugin = null as JavaPlugin?
        set(value) {
            field = value
            classAnimatedScoreboard = value?.javaClass
            methodGetScoreboardHandler = classAnimatedScoreboard?.getDeclaredMethod("getScoreboardHandler")
            classPlayerScoreboardHandler = methodGetScoreboardHandler?.returnType
            methodGetWorlds = classPlayerScoreboardHandler?.getDeclaredMethod("getWorlds")
            methodAddTemplate = classPlayerScoreboardHandler?.getDeclaredMethod("addTemplate", String::class.java, String::class.java)
        }

    private var classAnimatedScoreboard = null as Class<*>?
    private var classPlayerScoreboardHandler = null as Class<*>?
    private var methodGetScoreboardHandler = null as Method?
    private var methodGetWorlds = null as Method?
    private var methodAddTemplate = null as Method?

    fun addWorld(name: String) {
        getScoreboardHandler()?.let { methodAddTemplate?.invoke(it, name, "sgscoreboard") }
    }

    fun removeWorld(name: String) {
        getScoreboardHandler()?.let { (methodGetWorlds?.invoke(it) as? MutableMap<*, *>)?.remove(name) }
    }

    private fun getScoreboardHandler() = plugin?.let { methodGetScoreboardHandler?.invoke(it) }
}
