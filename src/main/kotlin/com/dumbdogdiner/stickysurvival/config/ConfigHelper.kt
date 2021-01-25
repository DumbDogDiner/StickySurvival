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

import org.bukkit.configuration.Configuration
import org.bukkit.configuration.MemorySection
import kotlin.reflect.KClass

class ConfigHelper private constructor(
    private val default: ConfigHelper?,
    private val obj: Any?,
    private val path: List<Any>
) {
    constructor(default: ConfigHelper?, cfg: Configuration) : this(default, cfgToObject(cfg), emptyList())

    fun isDefault(): Boolean = default == null

    private fun fail(reason: String?): Nothing = throw InvalidConfigException(path.joinToString("."), reason)
    private operator fun plus(p: Any) = path.toMutableList().also { it.add(p) }

    operator fun get(index: Int) = ConfigHelper(default, asA(List::class)[index], this + index)
    operator fun get(key: String) = ConfigHelper(default, asA(Map::class)[key], this + key)

    private fun <T : Any> thisIsA(clazz: KClass<T>) = clazz.isInstance(obj)

    private fun <T : Any> thisAsA(clazz: KClass<T>) = if (isA(clazz)) {
        @Suppress("unchecked_cast")
        obj as T
    } else {
        fail("Type mismatch: expected ${clazz.simpleName}, found ${obj?.let{it::class.simpleName} ?: "null"}")
    }

    fun <T : Any> isA(clazz: KClass<T>) = orDefault().thisIsA(clazz)
    fun <T : Any> asA(clazz: KClass<T>): T = orDefault().thisAsA(clazz)

    fun <T : Any> asOr(clazz: KClass<T>, x: T) = maybe { it.asA(clazz) } ?: x

    fun asInt() = asA(Number::class).toInt()
    fun asLong() = asA(Number::class).toLong()
    fun asFloat() = asA(Number::class).toFloat()
    fun asDouble() = asA(Number::class).toDouble()
    fun asString() = asA(String::class)

    fun asIntOr(x: Int) = asOr(Number::class, x).toInt()
    fun asLongOr(x: Long) = asOr(Number::class, x).toLong()
    fun asFloatOr(x: Float) = asOr(Number::class, x).toFloat()
    fun asDoubleOr(x: Double) = asOr(Number::class, x).toDouble()
    fun asStringOr(x: String) = asOr(String::class, x)

    private fun exists() = obj != null

    fun <T> maybe(f: (ConfigHelper) -> T) = if (exists()) f(this) else null
    fun maybeGet() = if (exists()) this else null

    fun <T> map(f: (ConfigHelper) -> T) = asA(List::class).indices.map { f(this[it]) }
    fun <T> mapEntries(f: (String, ConfigHelper) -> T) = asA(Map::class).keys.map { f(it as String, this[it]) }

    private fun orDefault() = if (default != null) {
        if (exists()) this else path.fold(default) { cfg, pathElement ->
            when (pathElement) {
                is Int -> cfg[pathElement]
                is String -> cfg[pathElement]
                else -> throw IllegalStateException("Bad element in path")
            }
        }
    } else this

    companion object {
        // converts a configuration to a map that more closely represents the configuration
        private fun cfgToObject(cfg: Configuration): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            val cfgValues = cfg.getValues(true)
            for (entry in cfgValues) {
                val path = entry.key.split(".")
                val location = path.dropLast(1).fold(map) { currentLocation, pathPart ->
                    if (currentLocation[pathPart] is Map<*, *>) {
                        @Suppress("unchecked_cast")
                        currentLocation[pathPart] as MutableMap<String, Any>
                    } else {
                        val newLocation = mutableMapOf<String, Any>()
                        currentLocation[pathPart] = newLocation
                        newLocation
                    }
                }
                location[path.last()] = when (val value = entry.value) {
                    is MemorySection -> mutableMapOf<String, Any>()
                    else -> value
                }
            }
            return map
        }
    }
}
