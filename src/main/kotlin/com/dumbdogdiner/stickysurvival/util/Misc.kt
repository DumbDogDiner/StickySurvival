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

package com.dumbdogdiner.stickysurvival.util

import com.dumbdogdiner.stickysurvival.StickySurvival
import com.dumbdogdiner.stickysurvival.config.Config
import com.dumbdogdiner.stickysurvival.config.ConfigHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.Collections
import java.util.IllegalFormatException
import java.util.WeakHashMap
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.random.Random

private fun maybeKey(name: String): NamespacedKey? = try {
    NamespacedKey.minecraft(name)
} catch (_: IllegalArgumentException) {
    null
}

fun getMaterial(name: String): Material {
    return Material.getMaterial(name)
        ?: maybeKey(name)?.let { key -> Material.values().firstOrNull { it.key == key } }
        ?: throw IllegalArgumentException("No such material: $name")
}

fun getEnchantment(name: String): Enchantment {
    return Enchantment.getByName(name)
        ?: maybeKey(name)?.let { Enchantment.getByKey(it) }
        ?: throw IllegalArgumentException("No such enchantment: $name")
}

fun getPotionEffectType(name: String): PotionEffectType {
    return PotionEffectType.getByName(name)
        ?: effectTypes[name]
        ?: throw IllegalArgumentException("No such potion effect: $name")
}

// some effect names are different between object and minecraft name
private val effectTypes = PotionEffectType.values().map {
    it.name.toLowerCase() to it
}.toMap().toMutableMap().also {
    fun fixType(name: String, type: PotionEffectType) {
        it -= type.name.toLowerCase()
        it += name to type
    }
    fixType("slowness", PotionEffectType.SLOW)
    fixType("haste", PotionEffectType.FAST_DIGGING)
    fixType("mining_fatigue", PotionEffectType.SLOW_DIGGING)
    fixType("strength", PotionEffectType.INCREASE_DAMAGE)
    fixType("instant_health", PotionEffectType.HEAL)
    fixType("instant_damage", PotionEffectType.HARM)
    fixType("jump_boost", PotionEffectType.JUMP)
    fixType("nausea", PotionEffectType.CONFUSION)
    fixType("resistance", PotionEffectType.DAMAGE_RESISTANCE)
}

fun itemFromConfig(cfg: ConfigHelper): ItemStack {
    val material = getMaterial(cfg["item"].asString())
    val stack = ItemStack(material, cfg["amount"].asIntOr(1))
    val itemMeta = stack.itemMeta
    cfg["color"].maybe {
        val color = if (it.isA(Number::class)) {
            Color.fromRGB(it.asInt())
        } else {
            DyeColor.valueOf(it.asString().toUpperCase()).color
        }
        when (itemMeta) {
            is LeatherArmorMeta -> itemMeta.setColor(color)
            is PotionMeta -> itemMeta.color = color
        }
    }
    cfg["name"].maybe {
        itemMeta.setDisplayName(it.asString().substituteAmpersand())
    }
    cfg["effects"].maybe { effects ->
        if (itemMeta is PotionMeta) {
            for (effect in effects.map { effectFromConfig(it) }) {
                itemMeta.addCustomEffect(effect, true)
            }
        }
    }
    stack.itemMeta = itemMeta
    cfg["enchantments"].maybeGet()?.mapEntries { name, level ->
        stack.addUnsafeEnchantment(getEnchantment(name), level.asInt())
    }
    cfg["lore"].maybe {
        stack.lore = it.map { line -> line.asString().substituteAmpersand() }
    }
    return stack
}

fun effectFromConfig(cfg: ConfigHelper): PotionEffect {
    val type = getPotionEffectType(cfg["type"].asString())
    val duration = cfg["duration"].let {
        when {
            it.isA(String::class) && it.asString() == "forever" -> Int.MAX_VALUE
            else -> it.asInt()
        }
    }
    val amplifier = cfg["amplifier"].asInt()
    return PotionEffect(type, duration, amplifier)
}

fun spawn(f: () -> Unit): BukkitTask {
    return Bukkit.getScheduler().runTaskAsynchronously(StickySurvival.instance, f)
}

fun schedule(f: () -> Unit): Int {
    return Bukkit.getScheduler().scheduleSyncDelayedTask(StickySurvival.instance, f)
}

val settings
    get() = Config.current
val messages
    get() = settings.messages
val worlds
    get() = settings.worldConfigs

fun <T> newWeakSet(): MutableSet<T> = Collections.newSetFromMap(WeakHashMap<T, Boolean>())

@Suppress("unchecked_cast")
fun <T> waitFor(t: () -> T?) = runBlocking {
    while (true) {
        val value = t()
        if (value != null) return@runBlocking value
        delay(10L)
    }
} as T

// don't crash a method due to bad formatting
fun String.safeFormat(vararg args: Any?) = try {
    this.format(*args)
} catch (e: IllegalFormatException) {
    warn("Couldn't format '$this': ${e.message}")
    this
}

fun String.substituteAmpersand() = this.replace("&", "§")

fun ClosedFloatingPointRange<Double>.random(): Double {
    return Random.nextDouble(start, endInclusive)
}

fun radiusForBounds(
    centerX: Double,
    xBounds: ClosedFloatingPointRange<Double>,
    centerZ: Double,
    zBounds: ClosedFloatingPointRange<Double>
): Double {
    val x1 = (centerX - xBounds.start).absoluteValue
    val x2 = (centerX - xBounds.endInclusive).absoluteValue
    val x = max(x1, x2)
    val z1 = (centerZ - zBounds.start).absoluteValue
    val z2 = (centerZ - zBounds.endInclusive).absoluteValue
    val z = max(z1, z2)
    return max(x, z)
}

/**
 * An extension to allow an event to be called without an exception, regardless of the context.
 */
fun Event.callSafe() {
    // if this is true, we are running synchronously
    val threadIsSync = Thread.holdsLock(this)
    if (isAsynchronous && threadIsSync) {
        // if this event is async and this context is not, spawn an async process and call this event there.
        spawn { callEvent() }
    } else if (!isAsynchronous && !threadIsSync) {
        // if this context is async and this event is not...
        if (StickySurvival.instance.isEnabled) {
            // if the plugin is enabled, schedule a synchronous task.
            schedule { callEvent() }
        } else {
            // if it's not (event called during onDisable), block until its completion
            synchronized(this) { callEvent() }
        }
    } else {
        // thread sync matches event sync, just go ahead
        callEvent()
    }
}
