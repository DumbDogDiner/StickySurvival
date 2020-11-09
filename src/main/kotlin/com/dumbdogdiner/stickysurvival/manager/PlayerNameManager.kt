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
import org.bukkit.Bukkit
import java.io.IOException
import java.net.URL
import java.util.UUID

object PlayerNameManager {
    class CachedPlayerName(val uuid: UUID, val name: String?) : Cacheable {
        override fun getKey() = uuid.toString()
    }

    data class NameHistory(val name: String, val changedToAt: Long?)

    private val cache = Cache(CachedPlayerName::class.java)

    operator fun get(uuid: UUID): String? {
        val cached = cache[uuid.toString()]
        return if (cached == null) {
            Bukkit.getPlayer(uuid)?.name ?: run {
                val name = try {
                    URL("https://api.mojang.com/user/profiles/${uuid.toString().replace("-", "")}/names").openStream()
                } catch (e: IOException) {
                    e.printStackTrace()
                    return null
                }.use {
                    Gson().fromJson(it.reader(), Array<NameHistory>::class.java)
                }.apply {
                    sortByDescending { it.changedToAt ?: 0 }
                }.firstOrNull()?.name
                cache.put(CachedPlayerName(uuid, name))
                name
            }
        } else {
            cached.name
        }
    }
}
