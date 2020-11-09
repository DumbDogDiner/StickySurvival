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

import com.dumbdogdiner.stickysurvival.manager.HiddenPlayerManager
import com.dumbdogdiner.stickysurvival.manager.LobbyInventoryManager
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

fun Player.reset() {
    // There must be a better way to do this (besides multiple servers)
    // it's not perfect, and i tried to focus on only resetting things that could
    // change during a game.

    fallDistance = 0F
    isGlowing = false
    isInvulnerable = false

    absorptionAmount = 0.0

    remainingAir = maximumAir
    noDamageTicks = 0
    arrowsStuck = 0
    shieldBlockingDelay = 0
    isCollidable = true
    canPickupItems = true

    inventory.clear()
    setCooldown(Material.ENDER_PEARL, 0)
    setCooldown(Material.CHORUS_FRUIT, 0)
    setCooldown(Material.SHIELD, 0)

    allowFlight = gameMode == GameMode.CREATIVE

    walkSpeed = 0.2F
    flySpeed = 0.1F

    killer = null

    halt()
    extinguish()
    resetExperience()
    heal()
    feed()
    resetCooldown()
    clearPotionEffects()
}

fun Player.reset(newGameMode: GameMode) {
    gameMode = newGameMode
    reset()
}

fun Player.halt() {
    velocity = Vector()
}

fun Player.heal() {
    health = getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
}

fun Player.extinguish() {
    fireTicks = 0
}

fun Player.feed() {
    exhaustion = 0F
    saturation = 5F
    foodLevel = 20
}

fun Player.resetExperience() {
    exp = 0F
    level = 0
}

fun Player.clearPotionEffects() {
    for (effect in activePotionEffects) {
        removePotionEffect(effect.type)
    }
}

fun Player.freeze() {
    reset(GameMode.ADVENTURE)
    isInvulnerable = true
    saturation = Float.POSITIVE_INFINITY
    foodLevel = 1
    walkSpeed = 0.00001F
    addPotionEffect(PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, 129, false, false, false))
}

fun Player.spectate() {
    inventory.clear()
    isCollidable = false
    isInvulnerable = true
    saturation = Float.POSITIVE_INFINITY
    gameMode = GameMode.ADVENTURE
    allowFlight = true
    canPickupItems = false
    HiddenPlayerManager.add(this)
}

fun Player.goToLobby(): Boolean {
    reset(GameMode.ADVENTURE)
    HiddenPlayerManager.remove(this)
    return if (teleport(settings.lobbySpawn)) {
        LobbyInventoryManager.restoreInventory(this)
        true
    } else {
        false
    }
}
