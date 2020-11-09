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
import de.tr7zw.nbtapi.NBTContainer
import de.tr7zw.nbtapi.NBTItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.Collections
import java.util.IllegalFormatException
import java.util.WeakHashMap
import kotlin.random.Random

inline fun <reified T : Keyed> getKeyed(name: String) = NamespacedKey.minecraft(name).let { key ->
    (T::class.java.getDeclaredMethod("values").invoke(null) as Array<*>).first {
        (it as T).key == key
    } as T
}

fun itemFromConfig(cfg: ConfigHelper): ItemStack {
    var stack = ItemStack(getKeyed(cfg["item"].asString()), cfg["amount"].asIntOr(1))
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
            for (effect in effects.map(::effectFromConfig)) {
                itemMeta.addCustomEffect(effect, true)
            }
        }
    }
    stack.itemMeta = itemMeta
    cfg["enchantments"].maybeGet()?.mapEntries { name, level ->
        stack.addUnsafeEnchantment(getKeyed(name), level.asInt())
    }
    cfg["lore"].maybe {
        stack.lore = it.map { line -> line.asString().substituteAmpersand() }
    }
    cfg["data"].maybe { data ->
        val nbt = NBTItem.convertItemtoNBT(stack)
        nbt.mergeCompound(NBTContainer(data.asString()))
        stack = NBTItem.convertNBTtoItem(nbt)
    }
    return stack
}

// some effect names are different between object and minecraft name

val effects = PotionEffectType.values().map {
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

fun effectFromConfig(cfg: ConfigHelper): PotionEffect {
    val name = cfg["type"].asString()
    val type = effects[name] ?: throw IllegalArgumentException("Effect $name does not exist")
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

val trackingStickKey = NamespacedKey(StickySurvival.instance, "tracking_stick")

fun trackingStickName(uses: Int, config: Config = settings) = "${config.trackingStickName}&r Uses: $uses".substituteAmpersand()

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
