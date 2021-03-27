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

import com.dumbdogdiner.stickysurvival.event.GameCloseEvent
import com.dumbdogdiner.stickysurvival.event.HologramNeedsUpdatingEvent
import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.unregisterListener
import com.dumbdogdiner.stickysurvival.util.worlds
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

class LobbyHologram(val worldName: String) : Listener {
    val config = worlds.getValue(worldName)

    private fun makeEntity(relativeY: Double, type: EntityType): Entity {
        return WorldManager.lobbyWorld.spawnEntity(config.hologram.clone().add(0.0, relativeY, 0.0), type)
    }

    private fun makeStand(relativeY: Double): ArmorStand {
        return makeEntity(relativeY, EntityType.ARMOR_STAND) as ArmorStand
    }

    /** The armor stand with the arena's name */
    private val arenaNameArmorStand = makeStand(-0.0)
    /** The armor stand with the arena's status (Waiting, Starting, Active, etc.) */
    private val arenaStatusArmorStand = makeStand(-0.3)
    /** The armor stand with more detail on the arena's status (e.g. How many players are left?) */
    private val arenaDetailArmorStand = makeStand(-0.6)
    /** The armor stand inviting the player to join, or to spectate if the game is not accepting players */
    private val arenaJoinOrSpectateArmorStand = makeStand(-0.9)
    /** Collection of armor stands for iteration */
    val armorStands = setOf(
        arenaNameArmorStand,
        arenaStatusArmorStand,
        arenaDetailArmorStand,
        arenaJoinOrSpectateArmorStand
    )
    /** The floating icon displayed under the text */
    private val icon = makeEntity(0.5, EntityType.DROPPED_ITEM) as Item
    /** Task to keep item alive no matter what */
    private val keepItemTask = object : BukkitRunnable() {
        override fun run() {
            icon.pickupDelay = Int.MAX_VALUE
            icon.ticksLived = 1
        }
    }.apply {
        runTaskTimer(StickySurvival.instance, 0, 60 * 20)
    }

    init {
        for (stand in armorStands) {
            // Mark as part of a hologram, what it's used for doesn't work atm (see comment on hologramKey)
            stand.persistentDataContainer.set(hologramKey, PersistentDataType.BYTE, 0)
            // set up armor stand for hologram-ness
            stand.setGravity(false)
            stand.isVisible = false
            stand.isCustomNameVisible = true
            stand.isInvulnerable = true
        }

        icon.persistentDataContainer.set(hologramKey, PersistentDataType.BYTE, 0)
        icon.setGravity(false)
        icon.itemStack = ItemStack(config.icon)
        icon.isInvulnerable = true
        // item doesn't properly spawn in the current location without manual zeroing of velocity
        icon.velocity = Vector()

        arenaNameArmorStand.customName = config.friendlyName
        update(null)
        Bukkit.getPluginManager().registerEvents(this, StickySurvival.instance)
    }

    @EventHandler
    fun onNeedsUpdate(event: HologramNeedsUpdatingEvent) {
        if (event.game.config == config) {
            update(event.game)
        }
    }

    @EventHandler
    fun onGameClose(event: GameCloseEvent) {
        if (event.game.config == config) {
            update(null)
        }
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        for (stand in armorStands) {
            stand.remove()
        }
        icon.remove()
        keepItemTask.cancel()
        unregisterListener(this)
    }

    /**
     * Update the displays for a game in the WAITING phase.
     */
    private fun waitingPhase(game: Game) {
        val tributesLeft = game.tributesComponent.size
        val minPlayers = game.config.minPlayers
        if (tributesLeft < minPlayers) {
            arenaStatusArmorStand.customName = messages.lobby.waiting
            arenaDetailArmorStand.customName = messages.lobby.moreNeeded.safeFormat(minPlayers - tributesLeft)
        } else {
            arenaStatusArmorStand.customName = messages.lobby.starting
            arenaDetailArmorStand.customName = messages.lobby.spaceLeft.safeFormat(game.spawnPointComponent.getSpaceLeft())
        }
        arenaJoinOrSpectateArmorStand.customName = messages.lobby.click
    }

    /**
     * Update the displays for a game in the ACTIVE phase.
     */
    private fun activePhase(game: Game) {
        arenaStatusArmorStand.customName = messages.lobby.active
        arenaDetailArmorStand.customName = messages.lobby.alive.safeFormat(game.tributesComponent.size)
        arenaJoinOrSpectateArmorStand.customName = messages.lobby.spectate
    }

    /**
     * Update the displays for a game in the COMPLETE phase.
     */
    private fun completePhase(game: Game) {
        arenaStatusArmorStand.customName = messages.lobby.complete
        val winner0 = game.winner
        if (winner0 != null) {
            arenaDetailArmorStand.customName = messages.lobby.winner.safeFormat(winner0.name)
        } else {
            arenaDetailArmorStand.customName = null
        }
        arenaJoinOrSpectateArmorStand.customName = messages.lobby.spectate
    }

    /**
     * Update the displays for an arena that is not loaded.
     */
    private fun empty() {
        arenaStatusArmorStand.customName = messages.lobby.empty
        arenaDetailArmorStand.customName = null
        arenaJoinOrSpectateArmorStand.customName = messages.lobby.click
    }

    /**
     * Temporarily load the chunk(s) that the armor stands are in.
     */
    private fun loadChunks(): Map<Long, Boolean> {
        val isLoaded = mutableMapOf<Long, Boolean>()
        for (stand in armorStands) {
            val chunk = stand.chunk
            val key = chunk.chunkKey
            if (key !in isLoaded) isLoaded[key] = chunk.isForceLoaded
            chunk.isForceLoaded = true
        }
        return isLoaded
    }

    /**
     * Restore the loaded chunks to their previous force-loaded-ness.
     */
    private fun releaseChunks(isLoaded: Map<Long, Boolean>) {
        for (stand in armorStands) {
            val chunk = stand.chunk
            val key = chunk.chunkKey
            chunk.isForceLoaded = isLoaded[key]!!
        }
    }

    /**
     * Update the hologram for the given game. The game will be given as null if not loaded.
     */
    private fun update(game: Game?) {
        val isLoaded = loadChunks()
        try {
            when (game?.phase) {
                Game.Phase.WAITING -> waitingPhase(game)
                Game.Phase.ACTIVE -> activePhase(game)
                Game.Phase.COMPLETE -> completePhase(game)
                null -> empty()
            }
            // fix a quirk with empty custom names
            arenaDetailArmorStand.isCustomNameVisible = arenaDetailArmorStand.customName != null
        } finally {
            releaseChunks(isLoaded)
        }
    }

    companion object {
        // use this key to identify and remove broken holograms, should the plugin fail to remove them
        // currently this does not work
        val hologramKey = NamespacedKey(StickySurvival.instance, "hologram")
    }
}
