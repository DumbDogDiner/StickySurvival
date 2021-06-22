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

import com.dumbdogdiner.stickysurvival.Game
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.settings
import net.kyori.adventure.text.Component
import org.bukkit.Material
import kotlin.math.roundToInt

class TrackingCompassRunnable(val game: Game) : SafeRunnable() {
    override fun run() {
        val currentTributes = game.world.players.filter { it in game.tributesComponent }
        for (player in currentTributes) {
            val closestPlayer = currentTributes.asSequence().filter {
                it != player // find all players that are not this player
            }.sortedBy {
                it.location.distanceSquared(player.location) // sort by closest
            }.firstOrNull()
            if (player.inventory.itemInMainHand.type == Material.COMPASS || player.inventory.itemInOffHand.type == Material.COMPASS) {
                if (closestPlayer != null) {
                    val loc = closestPlayer.location
                    player.compassTarget = loc
                    player.sendActionBar(
                        Component.text(
                            settings.trackingCompassMessage.safeFormat(
                                closestPlayer.name,
                                loc.x.roundToInt(),
                                loc.y.roundToInt(),
                                loc.z.roundToInt(),
                            )
                        )
                    )
                }
            }
        }
    }
}
