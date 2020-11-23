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

import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.worlds
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

class LobbyHologram(val worldName: String) {
    companion object {
        // use this key to identify and remove broken holograms, should the plugin fail to remove them
        val hologramKey = NamespacedKey(StickySurvival.instance, "hologram")
    }
    val config = worlds.getValue(worldName)

    private fun makeEntity(relativeY: Double, type: EntityType): Entity {
        return WorldManager.lobbyWorld.spawnEntity(config.hologram.clone().add(0.0, relativeY, 0.0), type)
    }

    private fun makeStand(relativeY: Double): ArmorStand {
        return makeEntity(relativeY, EntityType.ARMOR_STAND) as ArmorStand
    }

    private val armorStand1 = makeStand(-0.0)
    private val armorStand2 = makeStand(-0.3)
    private val armorStand3 = makeStand(-0.6)
    private val armorStand4 = makeStand(-0.9)
    val armorStands = setOf(armorStand1, armorStand2, armorStand3, armorStand4)
    private val floatingSword = makeEntity(0.5, EntityType.DROPPED_ITEM) as Item
    private val keepItemTask = object : BukkitRunnable() {
        override fun run() {
            floatingSword.pickupDelay = Int.MAX_VALUE
            floatingSword.ticksLived = 1
        }
    }.apply {
        runTaskTimer(StickySurvival.instance, 60 * 20, 60 * 20)
    }

    init {
        for (stand in armorStands) {
            stand.persistentDataContainer.set(hologramKey, PersistentDataType.BYTE, 0)
            stand.setGravity(false)
            stand.isVisible = false
            stand.isCustomNameVisible = true
            stand.isInvulnerable = true
        }

        floatingSword.setGravity(false)
        floatingSword.persistentDataContainer.set(hologramKey, PersistentDataType.BYTE, 0)
        floatingSword.itemStack = ItemStack(config.icon)
        floatingSword.pickupDelay = Int.MAX_VALUE
        floatingSword.isInvulnerable = true
        // item doesn't properly spawn in the current location without manual zeroing of velocity
        floatingSword.velocity = Vector()
        // floatingSword.teleport(config.hologram)

        armorStand1.customName = config.friendlyName
        update()
    }

    fun cleanup() {
        for (stand in armorStands) {
            stand.remove()
        }
        floatingSword.pickupDelay = 0
        floatingSword.ticksLived = Int.MAX_VALUE
        floatingSword.remove()
        keepItemTask.cancel()
    }

    fun update() {
        val game = WorldManager.getLoadedGame(worldName)
        val areChunksForceLoaded = BooleanArray(armorStands.size)
        for ((i, armorStand) in armorStands.withIndex()) {
            areChunksForceLoaded[i] = armorStand.chunk.isForceLoaded
            armorStand.chunk.isForceLoaded = true
        }
        if (game != null) {
            val tributesLeft = game.getTributesLeft()
            val spaceLeft = game.getSpaceLeft()
            val minPlayers = game.config.minPlayers
            armorStand2.customName = when (game.phase) {
                Game.Phase.WAITING ->
                    if (tributesLeft < minPlayers) {
                        messages.lobby.waiting
                    } else {
                        messages.lobby.starting
                    }
                Game.Phase.ACTIVE -> messages.lobby.active
                Game.Phase.COMPLETE -> messages.lobby.complete
            }
            armorStand3.customName = when (game.phase) {
                Game.Phase.WAITING ->
                    if (tributesLeft < minPlayers) {
                        messages.lobby.moreNeeded.safeFormat(minPlayers - tributesLeft)
                    } else {
                        messages.lobby.spaceLeft.safeFormat(spaceLeft)
                    }
                Game.Phase.ACTIVE -> messages.lobby.alive.safeFormat(tributesLeft)
                Game.Phase.COMPLETE -> {
                    val winner0 = game.winner
                    if (winner0 != null) {
                        messages.lobby.winner.safeFormat(winner0.name)
                    } else {
                        null
                    }
                }
            }
            armorStand4.customName = when (game.phase) {
                Game.Phase.WAITING -> messages.lobby.click
                else -> messages.lobby.spectate
            }
        } else {
            armorStand2.customName = messages.lobby.empty
            armorStand3.customName = null
            armorStand4.customName = messages.lobby.click
        }
        armorStand3.isCustomNameVisible = armorStand3.customName != null
        for ((i, armorStand) in armorStands.withIndex()) {
            armorStand.chunk.isForceLoaded = areChunksForceLoaded[i]
        }
    }
}
