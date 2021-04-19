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

package com.dumbdogdiner.stickysurvival.stats

import com.dumbdogdiner.stickyapi.common.cache.Cacheable
import org.jetbrains.exposed.sql.Column
import java.util.UUID

data class PlayerStats(val uuid: UUID, val wins: Int, val losses: Int, val kills: Int) : Cacheable {
    override fun getKey() = uuid.toString()

    constructor(getter: (Column<*>) -> Any?) : this(
        getter(SurvivalGamesStats.id) as UUID,
        getter(SurvivalGamesStats.wins) as Int,
        getter(SurvivalGamesStats.losses) as Int,
        getter(SurvivalGamesStats.kills) as Int,
    )

    fun putIntoDB(setter: (Column<Int>, Int) -> Unit) {
        setter(SurvivalGamesStats.wins, wins)
        setter(SurvivalGamesStats.losses, losses)
        setter(SurvivalGamesStats.kills, kills)
    }
}
