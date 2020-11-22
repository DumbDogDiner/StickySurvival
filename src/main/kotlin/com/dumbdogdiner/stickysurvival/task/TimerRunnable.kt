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

package com.dumbdogdiner.stickysurvival.task

import com.dumbdogdiner.stickysurvival.Game
import com.dumbdogdiner.stickysurvival.util.broadcastMessage
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.warn
import kotlin.math.absoluteValue
import kotlin.math.max

class TimerRunnable(game: Game) : GameRunnable(game) {
    override fun run() {
        game.countdown -= 1

        when (game.phase) {
            Game.Phase.WAITING -> {
                if (game.countdown == 0) {
                    game.startGame()
                } else {
                    game.playCountdownClick()
                }
            }

            Game.Phase.ACTIVE -> {
                if (game.countdown == game.config.borderStart) {
                    val shrinkTime = game.config.borderStart - game.config.borderEnd
                    val radius = radiusForBounds(
                        centerX = game.config.center.x,
                        centerZ = game.config.center.z,
                        xBounds = game.config.xBounds,
                        zBounds = game.config.zBounds,
                    )
                    game.world.worldBorder.size = radius * 2.0
                    game.world.worldBorder.setSize(game.config.borderFinalSize, shrinkTime.toLong())
                    game.world.broadcastMessage(messages.chat.border.safeFormat(shrinkTime))
                }
                if (game.countdown == 0) {
                    if (game.noDamage) {
                        game.enableDamage()
                    } else {
                        game.checkForWinner()
                        if (game.winner == null) game.endAsDraw()
                        safelyCancel()
                    }
                }
            }

            Game.Phase.COMPLETE -> {
                warn(
                    "Countdown was still running after the game was completed, which shouldn't happen, but won't" +
                        "cause any issues. Cancelling the countdown now."
                )
                safelyCancel()
            }
        }
    }

    companion object {
        private fun radiusForBounds(
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
    }
}
