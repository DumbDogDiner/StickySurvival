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

package com.dumbdogdiner.stickysurvival

import com.dumbdogdiner.stickysurvival.manager.PlayerNameManager
import com.dumbdogdiner.stickysurvival.manager.StatsManager
import com.dumbdogdiner.stickysurvival.util.game
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

object StickySurvivalExpansion : PlaceholderExpansion() {
    override fun getIdentifier() = StickySurvival.instance.description.name
    override fun getAuthor() = StickySurvival.instance.description.authors.joinToString()
    override fun getVersion() = StickySurvival.instance.description.version
    override fun canRegister() = true
    override fun persist() = true

    override fun onPlaceholderRequest(p: Player, params: String): String? {
        return when {
            params.startsWith("lb_player_") -> {
                val place = params.substringAfter("lb_player_").toInt()
                val (statPlayer, _) = StatsManager.topWins.getOrNull(place - 1) ?: return "----"
                PlayerNameManager[statPlayer]
            }

            params.startsWith("lb_score_") -> {
                val place = params.substringAfter("lb_score_").toInt()
                val (_, stat) = StatsManager.topWins.getOrNull(place - 1) ?: return "----"
                "$stat"
            }

            else -> {
                val game = p.world.game ?: return null
                when (params) {
                    "kills" -> game.killsFor(p).toString()
                    "world" -> game.config.friendlyName
                    "tributes_left" -> game.tributesComponent.size.toString()
                    else -> null
                }
            }
        }
    }
}
