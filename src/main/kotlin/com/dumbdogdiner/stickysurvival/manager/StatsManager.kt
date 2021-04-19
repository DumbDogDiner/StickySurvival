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

package com.dumbdogdiner.stickysurvival.manager

import com.dumbdogdiner.stickyapi.common.cache.Cache
import com.dumbdogdiner.stickysurvival.stats.PlayerStats
import com.dumbdogdiner.stickysurvival.stats.SurvivalGamesStats
import com.dumbdogdiner.stickysurvival.util.info
import com.dumbdogdiner.stickysurvival.util.schedule
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object StatsManager {
    // if no db is connected, stats are not saved and loading player stats returns null.

    private val cache = Cache(PlayerStats::class.java)

    private val logger = object : SqlLogger {
        override fun log(context: StatementContext, transaction: Transaction) {
            info("[SQL] ${context.expandArgs(transaction)}")
        }
    }

    var db = null as Database?

    val topWins = arrayOfNulls<Pair<UUID, Int>>(10)

    private var topStatsNeedUpdating = false

    operator fun get(player: Player) = if (db == null) null else {
        val uuid = player.uniqueId
        cache[uuid.toString()] ?: run {
            load(uuid)
            cache[uuid.toString()]!!
        }
    }

    // note that updateTopStats() is not automatically called for setter methods.
    // this is to minimize queries

    operator fun set(player: Player, stats: PlayerStats) {
        // noop if no database!
        if (db != null) {
            val uuid = player.uniqueId
            transaction(db) {
                addLogger(logger)
                SurvivalGamesStats.updateOrInsert({ SurvivalGamesStats.id eq uuid }) {
                    stats.putIntoDB { key, value -> it[key] = value }
                }
            }
            cache.update(stats)
            topStatsNeedUpdating = true
            schedule {
                if (topStatsNeedUpdating) {
                    updateTopStats()
                    topStatsNeedUpdating = false
                }
            }
        }
    }

    private fun load(uuid: UUID) {
        cache.put(
            transaction(db) {
                addLogger(logger)
                SurvivalGamesStats.select { SurvivalGamesStats.id eq uuid }.firstOrNull()?.let {
                    PlayerStats { key -> it[key] }
                } ?: run {
                    SurvivalGamesStats.insert {
                        it[id] = uuid
                        it[wins] = 0
                        it[losses] = 0
                        it[kills] = 0
                    }
                    PlayerStats(uuid, 0, 0, 0)
                }
            }
        )
    }

    private fun updateTopStats() {
        if (db != null) {
            transaction(db) {
                addLogger(logger)
                SurvivalGamesStats
                    .selectAll()
                    .orderBy(SurvivalGamesStats.wins, SortOrder.DESC)
                    .take(10)
                    .withIndex()
                    .forEach { (i, v) ->
                        topWins[i] = v[SurvivalGamesStats.id] to v[SurvivalGamesStats.wins]
                    }
            }
        }
    }

    fun init() {
        if (db != null) {
            transaction(db) {
                addLogger(logger)

                if (!SurvivalGamesStats.exists()) {
                    SchemaUtils.create(SurvivalGamesStats)
                }
            }

            updateTopStats()
        }
    }
}
