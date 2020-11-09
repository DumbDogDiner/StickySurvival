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

package com.dumbdogdiner.stickysurvival.stats

import com.dumbdogdiner.stickysurvival.StickySurvival
import com.dumbdogdiner.stickysurvival.manager.PlayerNameManager
import com.dumbdogdiner.stickysurvival.util.info
import de.tr7zw.nbtapi.NBTContainer
import de.tr7zw.nbtapi.NBTFile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.util.UUID

data class PlayerStats(val uuid: UUID, val kills: Long, val wins: Long, val losses: Long) {
    // game count can be derived from wins and losses without needing to store it
    val games get() = wins + losses
    private val lock = Mutex()

    private fun toNBT(): NBTContainer {
        val nbt = NBTContainer()

        nbt.setUUID("uuid", uuid)
        nbt.setLong("kills", kills)
        nbt.setLong("wins", wins)
        nbt.setLong("losses", losses)

        return nbt
    }

    suspend fun write() {
        lock.lock(null)
        try {
            findFile(uuid).outputStream().use { toNBT().writeCompound(it) }
        } finally {
            lock.unlock(null)
        }
    }

    companion object {
        private fun findFile(uuid: UUID) =
            StickySurvival.instance.dataFolder
                .resolve("stats")
                .resolve(uuid.toString().take(2))
                .apply(File::mkdirs)
                .resolve("$uuid.dat")

        fun load(playerId: UUID): PlayerStats {
            val file = findFile(playerId)
            if (!file.exists()) {
                info("Creating stats file for $playerId (${PlayerNameManager[playerId]}), as they have no records for Survival Games yet.")
                file.createNewFile()
                runBlocking { PlayerStats(playerId, 0L, 0L, 0L).write() }
            }
            val nbt = NBTFile(file)
            return PlayerStats(
                nbt.getUUID("uuid"),
                nbt.getLong("kills"),
                nbt.getLong("wins"),
                nbt.getLong("losses"),
            )
        }
    }
}
