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

package com.dumbdogdiner.stickysurvival.manager

import com.dumbdogdiner.stickysurvival.stats.PlayerStats
import com.dumbdogdiner.stickysurvival.stats.TopStats
import kotlinx.coroutines.runBlocking
import org.bukkit.entity.Player
import java.util.UUID

object StatsManager {
    private val players = mutableMapOf<UUID, PlayerStats>()
    private val wins = TopStats.load("wins", PlayerStats::wins)
    private val losses = TopStats.load("losses", PlayerStats::losses)
    private val kills = TopStats.load("kills", PlayerStats::kills)
    private val games = TopStats.load("games", PlayerStats::games)

    fun load(player: Player) {
        val id = profileId(player)
        players[id] = PlayerStats.load(id)
    }

    fun unload(player: Player) {
        players.remove(profileId(player))
    }

    operator fun get(player: Player): PlayerStats {
        return players[profileId(player)]
            ?: throw IllegalStateException("stats for ${player.name} were not loaded!")
    }

    operator fun set(player: Player, stats: PlayerStats) {
        val id = profileId(player)
        players[id] = stats

        runBlocking {
            wins.updateStat(id, stats.wins)
            losses.updateStat(id, stats.losses)
            kills.updateStat(id, stats.kills)
            games.updateStat(id, stats.games)
            stats.write()
        }
    }

    private fun profileId(player: Player) =
        player.playerProfile.id ?: throw IllegalStateException("Player has no ID on their profile")

    fun topWins() = wins.get()
    fun topLosses() = losses.get()
    fun topKills() = kills.get()
    fun topGames() = games.get()
}
