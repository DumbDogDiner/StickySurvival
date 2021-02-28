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

package com.dumbdogdiner.stickysurvival.config.language

import com.dumbdogdiner.stickysurvival.config.ConfigHelper
import com.dumbdogdiner.stickysurvival.util.substituteAmpersand
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

private val uppercaseRegex = Regex("[A-Z]")

internal fun <T : Any> loadMessages(clazz: KClass<T>, cfg: ConfigHelper): T {
    val constructor = clazz.primaryConstructor!!
    val parameters = mutableListOf<Any?>()
    for (parameter in constructor.parameters) {
        val name = parameter.name!!.replace(uppercaseRegex) { " " + it.value.toLowerCase() }
        parameters += cfg[name].asString().substituteAmpersand()
    }
    return constructor.call(*parameters.toTypedArray())
}
