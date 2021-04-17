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
import com.dumbdogdiner.stickysurvival.config.KitConfig
import com.dumbdogdiner.stickysurvival.event.BossBarNeedsUpdatingEvent
import com.dumbdogdiner.stickysurvival.event.GameEnableDamageEvent
import com.dumbdogdiner.stickysurvival.event.GameStartEvent
import com.dumbdogdiner.stickysurvival.event.HologramNeedsUpdatingEvent
import com.dumbdogdiner.stickysurvival.event.StartCountdownEvent
import com.dumbdogdiner.stickysurvival.event.StopCountdownEvent
import com.dumbdogdiner.stickysurvival.event.TributeAddEvent
import com.dumbdogdiner.stickysurvival.event.TributeRemoveEvent
import com.dumbdogdiner.stickysurvival.event.UpdateStatsEvent
import com.dumbdogdiner.stickysurvival.manager.HiddenPlayerManager
import com.dumbdogdiner.stickysurvival.manager.LobbyInventoryManager
import com.dumbdogdiner.stickysurvival.manager.StatsManager
import com.dumbdogdiner.stickysurvival.stats.PlayerStats
import com.dumbdogdiner.stickysurvival.util.callSafe
import com.dumbdogdiner.stickysurvival.util.freeze
import com.dumbdogdiner.stickysurvival.util.loadPreGameHotbar
import com.dumbdogdiner.stickysurvival.util.loadSpectatorHotbar
import com.dumbdogdiner.stickysurvival.util.reset
import com.dumbdogdiner.stickysurvival.util.settings
import com.dumbdogdiner.stickysurvival.util.spectate
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import java.util.WeakHashMap

class GameTributesComponent(game: Game) : GameComponent(game) {
    private val tributes = mutableSetOf<Player>()
    private val participants = mutableSetOf<Player>()
    private val kits = WeakHashMap<Player, KitConfig>()

    fun setKit(player: Player, kit: KitConfig) {
        kits[player] = kit
    }

    @EventHandler
    fun onEnableDamage(event: GameEnableDamageEvent) {
        if (event.game == game) {
            // make tributes no longer invulnerable
            tributes.forEach { it.isInvulnerable = false }
        }
    }

    fun addTribute(player: Player): Boolean {
        if (!player.hasPermission("stickysurvival.join")) {
            // not allowed to join
            return false
        }
        // save inventory to restore it after game
        LobbyInventoryManager.saveInventory(player)
        if (game.phase == Game.Phase.WAITING) {
            // joining as player

            // try to get spawn point
            if (!game.spawnPointComponent.givePlayerSpawnPoint(player)) {
                // failed to get spawn point, restore inventory and return false
                LobbyInventoryManager.restoreInventory(player)
                return false
            }
            // give a random kit to the player
            kits[player] = settings.kits.random()
            // freeze the player
            player.freeze()
            // give the player the pre-game hotbar
            player.loadPreGameHotbar()
            // add the player to the list
            tributes += player
            // call the event so components listening for this can react
            TributeAddEvent(player).callSafe()
            // if we have enough players, start the countdown
            if (tributes.size >= game.config.minPlayers && game.countdownComponent.countdown == -1) {
                StartCountdownEvent(game).callSafe()
            }
            // update displays
            BossBarNeedsUpdatingEvent(game).callSafe()
            HologramNeedsUpdatingEvent(game).callSafe()
            // success
            return true
        } else {
            // joining as spectator

            // put into pseudo-spectator
            player.spectate()
            // try to teleport the player
            val success = player.teleport(tributes.random().location)
            if (!success) {
                // if failed, undo what we've done
                player.reset()
                HiddenPlayerManager.remove(player)
                LobbyInventoryManager.restoreInventory(player)
            }
            // give the player the spectator hotbar
            player.loadSpectatorHotbar()
            return success
        }
    }

    @EventHandler
    fun onGameStart(event: GameStartEvent) {
        if (event.game == game) {
            tributes.forEach {
                // tribute is a participant
                participants += it
                // reset tribute, set to survival mode
                it.reset(GameMode.SURVIVAL)
                // give tribute their selected kit
                kits[it]?.giveTo(it)
                // no damage until noDamage is false
                it.isInvulnerable = true
            }
        }
    }

    @EventHandler
    fun onTributeRemove(event: TributeRemoveEvent) {
        if (event.game == game) {
            tributes -= event.player
            if (event.cause == TributeRemoveEvent.Cause.QUIT) {
                if (game.phase == Game.Phase.WAITING && tributes.size < game.config.minPlayers) {
                    StopCountdownEvent(game).callSafe()
                }
            }

            game.checkForWinner()
        }
    }

    @EventHandler
    fun onUpdateStats(event: UpdateStatsEvent) {
        if (event.game == game) {
            val winner = game.winner
            participants.forEach { player ->
                StatsManager[player]?.let {
                    var (uuid, wins, losses, kills) = it
                    kills += game.killsFor(player)
                    if (player == winner) wins += 1 else losses += 1
                    StatsManager[player] = PlayerStats(uuid, wins, losses, kills)
                }
            }
        }
    }

    operator fun contains(player: Player) = player in tributes
    val size get() = tributes.size
    fun winner() = tributes.firstOrNull().takeIf { size == 1 }
}
