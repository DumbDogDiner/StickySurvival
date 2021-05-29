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
import com.dumbdogdiner.stickyapi.common.cache.Cacheable
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.bukkit.Bukkit
import java.io.IOException
import java.net.URL
import java.util.UUID

/**
 * Caches player names for use in systems that require the names of potentially offline players, such as leaderboards.
 */
object PlayerNameManager {
    /** Maps a UUID to a username. */
    data class UUIDToUsername(val uuid: UUID, val username: String) : Cacheable {
        override fun getKey() = uuid.toString()
    }

    /** Cache of entries */
    private val cache = Cache(UUIDToUsername::class.java)

    /**
     * Get the username for a player by their UUID, or null if it cannot be obtained.
     */
    operator fun get(uuid: UUID): String? {
        // Check the cache first
        return cache[uuid.toString()]?.username
            // Was not in the cache, try Bukkit next
            ?: checkBukkit(uuid)?.username
            // Was not in Bukkit's offline player storage, ask Ashcon
            ?: try {
                // Attempt to query Ashcon for the information
                queryAshcon(uuid).username
            } catch (e: Exception) {
                if (e is IOException) {
                    // TODO use an API with proper HTTP exceptions
                    e.message?.let { message ->
                        if ("400" in message) {
                            // status code 400, probably... let's try the xuid
                            return queryXbox(uuid).username
                        }
                    }
                }
                e.printStackTrace()
                // If all else fails, return null
                null
            }
    }

    /**
     * Attempt to get the username from Bukkit's OfflinePlayer system.
     */
    private fun checkBukkit(uuid: UUID): UUIDToUsername? {
        Bukkit.getOfflinePlayer(uuid).name?.let {
            // Bukkit knows this player's name, cache the entry and return it
            val entry = UUIDToUsername(uuid, it)
            cache.put(entry)
            return@checkBukkit entry
        }
        // Not found, return null
        return null
    }

    /**
     * Make a query to Ashcon for a given UUID.
     */
    private fun queryAshcon(uuid: UUID): UUIDToUsername {
        // Make query
        val response = URL("https://api.ashcon.app/mojang/v2/user/$uuid").openStream().use {
            // Use Gson to serialize what we need
            Gson().fromJson(it.reader(), UUIDToUsername::class.java)
        }
        // Cache the response and return it
        cache.put(response)
        return response
    }

    private fun queryXbox(uuid: UUID): UUIDToUsername {
        // Make query
        val response = URL("https://xbl-api.prouser123.me/profile/xuid/${uuid.leastSignificantBits}").openStream().use {
            val gamertag = JsonParser().parse(it.reader())
                .asJsonObject["profileUsers"]
                .asJsonArray[0]
                .asJsonObject["settings"]
                .asJsonArray
                .find { setting -> setting.asJsonObject["id"].asString == "Gamertag" }!!
                .asJsonObject["value"].asString
            UUIDToUsername(uuid, gamertag)
        }
        // Cache the response and return it
        cache.put(response)
        return response
    }
}
