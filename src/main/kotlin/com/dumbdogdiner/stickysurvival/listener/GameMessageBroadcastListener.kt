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

package com.dumbdogdiner.stickysurvival.listener

import com.dumbdogdiner.stickysurvival.event.GameEnableDamageEvent
import com.dumbdogdiner.stickysurvival.event.GameStartEvent
import com.dumbdogdiner.stickysurvival.event.StartCountdownEvent
import com.dumbdogdiner.stickysurvival.event.StopCountdownEvent
import com.dumbdogdiner.stickysurvival.event.TributeAddEvent
import com.dumbdogdiner.stickysurvival.event.TributeRemoveEvent
import com.dumbdogdiner.stickysurvival.util.broadcastMessage
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.settings
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * Listener that broadcasts messages for various events in a game. Not complete.
 */
object GameMessageBroadcastListener : Listener {
    @EventHandler
    fun onEnableDamage(event: GameEnableDamageEvent) {
        event.game.world.broadcastMessage(messages.chat.damageEnabled)
    }

    @EventHandler
    fun onTributeAdd(event: TributeAddEvent) {
        val player = event.player
        player.world.broadcastMessage(messages.chat.join.safeFormat(player.name))
    }

    @EventHandler
    fun onTributeRemove(event: TributeRemoveEvent) {
        event.game.world.broadcastMessage(messages.chat.leave.safeFormat(event.player.name))
    }

    @EventHandler
    fun onGameStart(event: GameStartEvent) {
        event.game.world.broadcastMessage(messages.chat.start.safeFormat(event.game.noDamageTime))
    }

    @EventHandler
    fun onStartCountdown(event: StartCountdownEvent) {
        event.game.world.broadcastMessage(messages.chat.countdown.safeFormat(settings.countdown))
    }

    @EventHandler
    fun onStopCountdown(event: StopCountdownEvent) {
        event.game.world.broadcastMessage(messages.chat.countdownCancelled)
    }
}
