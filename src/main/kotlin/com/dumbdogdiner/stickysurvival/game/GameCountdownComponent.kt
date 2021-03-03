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

package com.dumbdogdiner.stickysurvival.game

import com.dumbdogdiner.stickysurvival.Game
import com.dumbdogdiner.stickysurvival.event.GameCloseEvent
import com.dumbdogdiner.stickysurvival.event.StartCountdownEvent
import com.dumbdogdiner.stickysurvival.event.StopCountdownEvent
import com.dumbdogdiner.stickysurvival.event.TributeWinEvent
import com.dumbdogdiner.stickysurvival.task.TimerRunnable
import com.dumbdogdiner.stickysurvival.util.broadcastMessage
import com.dumbdogdiner.stickysurvival.util.game
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.settings
import org.bukkit.event.EventHandler

class GameCountdownComponent(game: Game) : GameComponent(game) {
    private val task = TimerRunnable(game)

    @EventHandler
    fun onStartCountdown(event: StartCountdownEvent) {
        if (event.game == game) {
            // start the timer
            task.maybeRunTaskTimer(1, 1)

            // broadcast the message
            game.world.broadcastMessage(messages.chat.countdown.safeFormat(settings.countdown))
        }
    }

    @EventHandler
    fun onStopCountdown(event: StopCountdownEvent) {
        if (event.game == game) {
            // cancel the timer
            task.safelyCancel()

            // broadcast a message
            game.world.broadcastMessage(messages.chat.countdownCancelled)
        }
    }

    @EventHandler
    fun onTributeWin(event: TributeWinEvent) {
        if (event.player?.world?.game == game) {
            // cancel the timer
            task.safelyCancel()
        }
    }

    @EventHandler
    fun onGameClose(event: GameCloseEvent) {
        if (event.game == game) {
            // cancel the timer
            task.safelyCancel()
        }
    }
}
