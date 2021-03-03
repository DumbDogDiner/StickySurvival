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
import com.dumbdogdiner.stickysurvival.event.BossBarNeedsUpdatingEvent
import com.dumbdogdiner.stickysurvival.event.GameCloseEvent
import com.dumbdogdiner.stickysurvival.event.GameEnableDamageEvent
import com.dumbdogdiner.stickysurvival.event.GameStartEvent
import com.dumbdogdiner.stickysurvival.event.StartCountdownEvent
import com.dumbdogdiner.stickysurvival.event.StopCountdownEvent
import com.dumbdogdiner.stickysurvival.event.TributeWinEvent
import com.dumbdogdiner.stickysurvival.task.TimerRunnable
import com.dumbdogdiner.stickysurvival.util.broadcastMessage
import com.dumbdogdiner.stickysurvival.util.callSafe
import com.dumbdogdiner.stickysurvival.util.game
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.settings
import org.bukkit.event.EventHandler

class GameCountdownComponent(game: Game) : GameComponent(game) {
    private val task = TimerRunnable(game)
    var countdown = -1
        set(value) {
            field = value
            BossBarNeedsUpdatingEvent(game).callSafe()
        }

    private fun stopTimer() {
        task.safelyCancel()
        countdown = -1
    }

    @EventHandler
    fun onStartCountdown(event: StartCountdownEvent) {
        if (event.game == game) {
            // set the countdown
            countdown = settings.countdown
            // start the timer
            task.maybeRunTaskTimer(1, 1)
            // broadcast the message
            game.world.broadcastMessage(messages.chat.countdown.safeFormat(settings.countdown))
            // play the sound
            game.playCountdownClick()
        }
    }

    @EventHandler
    fun onGameStart(event: GameStartEvent) {
        if (event.game == game) {
            // set timer to no damage time
            countdown = game.noDamageTime
        }
    }

    @EventHandler
    fun onGameEnableDamage(event: GameEnableDamageEvent) {
        if (event.game == game) {
            // set timer to game time
            countdown = game.config.time
        }
    }

    @EventHandler
    fun onStopCountdown(event: StopCountdownEvent) {
        if (event.game == game) {
            // cancel the timer
            stopTimer()

            // broadcast a message
            game.world.broadcastMessage(messages.chat.countdownCancelled)
        }
    }

    @EventHandler
    fun onTributeWin(event: TributeWinEvent) {
        if (event.player?.world?.game == game) {
            // cancel the timer
            stopTimer()
        }
    }

    @EventHandler
    fun onGameClose(event: GameCloseEvent) {
        if (event.game == game) {
            // cancel the timer
            stopTimer()
        }
    }
}
