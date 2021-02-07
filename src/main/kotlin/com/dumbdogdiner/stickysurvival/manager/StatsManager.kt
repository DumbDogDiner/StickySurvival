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

import com.dumbdogdiner.stickyapi.common.cache.Cache
import com.dumbdogdiner.stickysurvival.stats.PlayerStats
import com.dumbdogdiner.stickysurvival.stats.SurvivalGamesStats
import com.dumbdogdiner.stickysurvival.util.info
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
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
import org.jetbrains.exposed.sql.update
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
    val topLosses = arrayOfNulls<Pair<UUID, Int>>(10)
    val topKills = arrayOfNulls<Pair<UUID, Int>>(10)
    val topGames = arrayOfNulls<Pair<UUID, Int>>(10)

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
                if (SurvivalGamesStats.select { SurvivalGamesStats.id eq uuid }.empty()) {
                    SurvivalGamesStats.insert {
                        stats.putIntoDB { key, value -> it[key] = value }
                    }
                } else {
                    SurvivalGamesStats.update({ SurvivalGamesStats.id eq uuid }) {
                        stats.putIntoDB { key, value -> it[key] = value }
                    }
                }
            }
            cache.put(stats)
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

    fun updateTopStats() {
        if (db != null) {
            transaction(db) {
                addLogger(logger)
                fun loadTopStat(
                    expr: Expression<Int>,
                    array: Array<Pair<UUID, Int>?>,
                    calculator: (ResultRow) -> Int = { it[expr] }
                ) {
                    SurvivalGamesStats.selectAll().orderBy(expr, SortOrder.DESC).take(10).withIndex().forEach { (i, v) ->
                        array[i] = v[SurvivalGamesStats.id] to calculator(v)
                    }
                }

                loadTopStat(SurvivalGamesStats.wins, topWins)
                loadTopStat(SurvivalGamesStats.losses, topLosses)
                loadTopStat(SurvivalGamesStats.kills, topKills)
                loadTopStat(SurvivalGamesStats.wins + SurvivalGamesStats.losses, topGames) {
                    it[SurvivalGamesStats.wins] + it[SurvivalGamesStats.losses]
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
