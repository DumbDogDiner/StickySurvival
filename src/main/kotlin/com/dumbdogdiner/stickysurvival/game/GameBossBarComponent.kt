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
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.settings
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerChangedWorldEvent

class GameBossBarComponent(game: Game) : GameComponent(game) {
    private val bossBar = Bukkit.createBossBar(null, BarColor.WHITE, BarStyle.SOLID)

    private var title
        get() = bossBar.title
        set(value) = bossBar.setTitle(value)

    private var progress
        get() = bossBar.progress
        set(value) { bossBar.progress = value }

    private var color
        get() = bossBar.color
        set(value) { bossBar.color = value }

    private fun <T : Number, U : Number> divideClamp(t: T, u: U): Double {
        return (t.toDouble() / u.toDouble()).coerceIn(0.0..1.0)
    }

    @EventHandler
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        if (event.player.world == game.world) {
            bossBar.addPlayer(event.player)
        } else if (event.from == game.world) {
            bossBar.removePlayer(event.player)
        }
    }

    @EventHandler
    fun cleanup(event: GameCloseEvent) {
        if (event.game == game) {
            bossBar.removeAll()
        }
    }

    @EventHandler
    fun update(event: BossBarNeedsUpdatingEvent) {
        val countdown = game.countdownComponent.countdown
        if (event.game == game) {
            when (game.phase) {
                Game.Phase.WAITING -> {
                    val tributesLeft = game.tributesComponent.size
                    if (tributesLeft >= game.config.minPlayers) {
                        title = messages.bossBar.countdown.safeFormat(countdown)
                        progress = divideClamp(countdown, settings.countdown)
                        color = BarColor.YELLOW
                    } else {
                        title = messages.bossBar.waiting.safeFormat(game.config.minPlayers - tributesLeft)
                        progress = divideClamp(tributesLeft, game.config.minPlayers)
                        color = BarColor.RED
                    }
                }
                Game.Phase.ACTIVE -> {
                    if (game.noDamage) {
                        title = messages.bossBar.noDamage.safeFormat(countdown)
                        progress = divideClamp(countdown, game.noDamageTime)
                        color = BarColor.PURPLE
                    } else {
                        title = messages.bossBar.active.safeFormat(countdown / 60, countdown % 60)
                        progress = divideClamp(countdown, game.config.time)
                        color = BarColor.GREEN
                    }
                }
                Game.Phase.COMPLETE -> {
                    progress = 1.0
                    game.winner?.name?.let {
                        title = messages.bossBar.winner.safeFormat(it)
                        color = BarColor.BLUE
                    } ?: run {
                        title = messages.bossBar.draw
                        color = BarColor.WHITE
                    }
                }
            }
        }
    }
}
