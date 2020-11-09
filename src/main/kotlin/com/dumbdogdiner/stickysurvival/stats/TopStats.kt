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
import com.dumbdogdiner.stickysurvival.util.info
import com.dumbdogdiner.stickysurvival.util.warn
import de.tr7zw.nbtapi.NBTContainer
import de.tr7zw.nbtapi.NBTFile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.bukkit.Bukkit
import java.io.File
import java.util.UUID
import kotlin.reflect.KProperty

class TopStats private constructor(val name: String) {
    private val collection = mutableListOf<Pair<UUID, Long>>()
    private val lock = Mutex()

    private constructor(name: String, c: List<Pair<UUID, Long>>) : this(name) {
        collection.addAll(c)
        adjust()
    }

    // returns true if list changed, false otherwise
    private fun adjust(): Boolean {
        val oldCollection = collection.toList()
        collection.apply {
            sortByDescending { (_, stat) -> stat }
            removeAll(takeLast((size - MAX_STATS).coerceAtLeast(0)))
        }
        return oldCollection != collection
    }

    suspend fun updateStat(uuid: UUID, stat: Long) {
        collection.apply {
            removeIf { (u, _) -> uuid == u }
            add(uuid to stat)
        }
        if (adjust()) {
            write()
        }
    }

    private fun toNBT(): NBTContainer {
        val nbt = NBTContainer()
        val list = nbt.getCompoundList(name)
        for ((uuid, stat) in collection) {
            list.addCompound().apply {
                setUUID("uuid", uuid)
                setLong("stat", stat)
            }
        }

        return nbt
    }

    suspend fun write() {
        lock.lock(null)
        try {
            findFile(name).outputStream().use { toNBT().writeCompound(it) }
        } finally {
            lock.unlock(null)
        }
    }

    fun get() = collection.map { (uuid, stat) ->
        Bukkit.getOfflinePlayer(uuid) to stat
    }

    companion object {
        const val MAX_STATS = 10
        private val restoreStatsFolderRegex = Regex("^[0-9a-f][0-9a-f]$")

        private fun findFile(name: String) =
            StickySurvival.instance.dataFolder.resolve("stats").resolve("top").apply(File::mkdirs).resolve("$name.dat")

        private fun restoreStats(name: String, field: KProperty<Long>): MutableList<Pair<UUID, Long>>? {
            val folders = StickySurvival.instance.dataFolder.resolve("stats").apply(File::mkdirs).listFiles()
            if (folders == null) {
                warn("Could not restore stats for $name: couldn't ls the stats folder")
                return null
            }
            return folders.asSequence().filter {
                it.name.matches(restoreStatsFolderRegex)
            }.map {
                val files = it.list()
                if (files == null) {
                    warn("Couldn't ls the folder ${it.name}")
                }
                files
            }.filterNotNull().flatMap {
                it.asSequence()
            }.map {
                val uuid = UUID.fromString(it.substringBeforeLast(".dat"))
                uuid to field.call(PlayerStats.load(uuid))
            }.toMutableList().apply {
                sortByDescending { (_, stat) -> stat }
            }
        }

        fun load(name: String, field: KProperty<Long>): TopStats {
            val file = findFile(name)
            if (!file.exists()) {
                // i noticed while testing that my leaderboard went empty once (not deleted, just empty).
                // this should do for now.
                warn("Top stats for $name were missing! Attempting to rebuild...")
                val restored = restoreStats(name, field)
                file.createNewFile()
                if (restored == null) {
                    warn("Couldn't rebuild the stats for $name. It might eventually fix itself, but not for an indefinite amount of time.")
                    runBlocking { TopStats(name).write() }
                } else {
                    info("Stats restored successfully.")
                    runBlocking { TopStats(name, restored).write() }
                }
            }
            val nbt = NBTFile(file)
            val collection = mutableListOf<Pair<UUID, Long>>()
            for (compound in nbt.getCompoundList(name)) {
                collection.add(compound.getUUID("uuid") to compound.getLong("stat"))
            }
            return TopStats(name, collection)
        }
    }
}
