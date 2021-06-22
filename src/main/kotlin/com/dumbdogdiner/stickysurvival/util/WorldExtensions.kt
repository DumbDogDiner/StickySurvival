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

package com.dumbdogdiner.stickysurvival.util

import com.dumbdogdiner.stickysurvival.manager.WorldManager
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.util.Vector

fun World.broadcastMessage(message: String) {
    for (player in players) {
        player.sendMessage(message)
    }
    Bukkit.getConsoleSender().sendMessage("[Broadcast to $name] $message")
}

fun World.broadcastSound(sound: Sound, volume: Float, pitch: Float) {
    for (player in players) {
        player.playSound(player.location, sound, volume, pitch)
    }
}

fun World.broadcastSound(relativeLocation: Vector, sound: Sound, volume: Float, pitch: Float) {
    for (player in players) {
        player.playSound(player.location.add(relativeLocation), sound, volume, pitch)
    }
}

val World.game get() = WorldManager.getGameForWorld(this)
