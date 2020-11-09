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

import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.util.getKeyed
import org.bukkit.Location
import org.bukkit.Material

class WorldConfig(
    val friendlyName: String,
    val icon: Material,
    val minPlayers: Int,
    val maxPlayers: Int,
    val noDamageTime: Int?,
    val chestRefill: Long,
    val time: Int,
    val hologram: Location,
    val xBounds: ClosedFloatingPointRange<Double>,
    val yBounds: ClosedFloatingPointRange<Double>,
    val zBounds: ClosedFloatingPointRange<Double>,
    val center: Location,
    val borderStart: Int,
    val borderEnd: Int,
    val borderFinalSize: Double,
    val spawnPoints: List<Location>,
) {
    constructor(worldName: String, cfg: ConfigHelper) : this(
        cfg["friendly name"].asStringOr(worldName),
        getKeyed(cfg["icon"].asString()),
        cfg["min players"].asInt(),
        cfg["max players"].maybe { it.asInt() } ?: cfg["spawn points"].map { it }.size,
        cfg["no damage time"].maybe { it.asInt() },
        cfg["chest refill"].asLongOr(-1),
        cfg["time"].asInt(),
        cfg["hologram"].let {
            Location(WorldManager.lobbyWorld, it["x"].asDouble(), it["y"].asDouble(), it["z"].asDouble())
        },
        cfg["bounds"].let { it["min"]["x"].asDouble()..it["max"]["x"].asDouble() },
        cfg["bounds"].let { it["min"]["y"].asDouble()..it["max"]["y"].asDouble() },
        cfg["bounds"].let { it["min"]["z"].asDouble()..it["max"]["z"].asDouble() },
        cfg["center"].let {
            Location(
                null,
                it["x"].asDouble(),
                it["y"].asDouble(),
                it["z"].asDouble(),
            )
        },
        cfg["border"]["starts shrinking"].asInt(),
        cfg["border"]["stops shrinking"].asInt(),
        cfg["border"]["final size"].asDouble(),
        cfg["spawn points"].map {
            Location(
                null,
                it["x"].asDouble(),
                it["y"].asDouble(),
                it["z"].asDouble(),
                it["yaw"].asFloatOr(0f),
                it["pitch"].asFloatOr(0f),
            )
        }
    )
}
