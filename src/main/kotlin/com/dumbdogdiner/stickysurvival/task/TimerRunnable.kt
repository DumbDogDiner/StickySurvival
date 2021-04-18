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
import com.dumbdogdiner.stickysurvival.util.broadcastSound
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.warn
import org.bukkit.Sound

class TimerRunnable(val game: Game) : SafeRunnable() {
    override fun run() {
        game.countdownComponent.countdown -= 1
        val countdown = game.countdownComponent.countdown

        when (game.phase) {
            Game.Phase.WAITING -> {
                when (countdown) {
                    3 -> game.world.broadcastSound(Sound.ENTITY_ARROW_HIT_PLAYER, 1F, 0.7F)
                    2 -> game.world.broadcastSound(Sound.ENTITY_ARROW_HIT_PLAYER, 1F, 0.8F)
                    1 -> game.world.broadcastSound(Sound.ENTITY_ARROW_HIT_PLAYER, 1F, 0.9F)
                    0 -> {
                        game.world.broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F)
                        game.startGame()
                    }
                    else -> game.playCountdownClick()
                }
            }

            Game.Phase.ACTIVE -> {
                if (countdown == game.config.borderStart) {
                    val shrinkTime = game.config.borderStart - game.config.borderEnd
                    game.world.worldBorder.setSize(game.config.borderFinalSize, shrinkTime.toLong())
                    game.world.broadcastMessage(messages.chat.border.safeFormat(shrinkTime))
                }
                if (countdown == 0) {
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
}
