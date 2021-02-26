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

package com.dumbdogdiner.stickysurvival.listener

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import com.dumbdogdiner.stickysurvival.Game
import com.dumbdogdiner.stickysurvival.util.game
import com.dumbdogdiner.stickysurvival.util.goToLobby
import com.dumbdogdiner.stickysurvival.util.settings
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDropItemEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import org.bukkit.event.world.ChunkLoadEvent

object GameEventsListener : Listener {
    // Code in this listener should be kept pretty minimal. The Game class should do most of the work.
    // ...and yes, i am aware that some of this code is not too minimal. i'm working on it.

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val game = event.block.world.game ?: return
        if (!game.playerIsTribute(event.player)) {
            event.isCancelled = true // spectators may not break blocks
            return
        }
        // only allow breaking of specified blocks
        if (event.block.state.type !in settings.breakableBlocks) {
            event.isCancelled = true
        }
        // even if the block does break, don't let it drop
        event.isDropItems = false
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        // might not want explosions in the lobby, but that's the lobby plugin's responsibility, not ours
        if (event.entity.world.game != null) event.blockList().clear() // explosions may not damage the world
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.isCancelled || event.player.world.game == null) return
        // don't let players place blocks
        event.isCancelled = true
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event.entity.world.game == null) return
        when (event.entityType) {
            EntityType.ITEM_FRAME, EntityType.PAINTING -> event.isCancelled = true
            else -> Unit
        }
    }

    @EventHandler
    fun onPotionSplash(event: PotionSplashEvent) {
        val game = event.potion.world.game ?: return
        for (entity in event.affectedEntities) {
            if (entity is Player && !game.playerIsTribute(entity)) {
                event.setIntensity(entity, 0.0) // spectators may not receive splash potion effects
            }
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val game = event.entity.world.game ?: return
        if (event.damager.type == EntityType.PLAYER && !game.playerIsTribute(event.damager as Player)) {
            event.isCancelled = true // spectators may not damage entities
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val game = player.world.game ?: return
        game.onPlayerDeath(player)
        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val game = event.player.world.game ?: return
        if (event.player.type == EntityType.PLAYER && !game.playerIsTribute(event.player)) {
            event.isCancelled = true // spectators may not interact with entities
            return
        }

        if (event.rightClicked is ItemFrame) {
            event.isCancelled = true // don't let players take items out of item frames
        }
    }

    @EventHandler
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity is Player) {
            val game = entity.world.game ?: return
            if (!game.playerIsTribute(entity)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        event.from.game?.onPlayerQuit(event.player)
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val config = (event.player.world.game ?: return).config
        // keep players in bounds (intentionally ignore y bounds)
        // there are legitimate ways to get past the world border, which is why this code exists
        val newLocation = event.player.location.apply {
            x = x.coerceIn(config.xBounds)
            z = z.coerceIn(config.zBounds)
        }
        if (newLocation != event.player.location) {
            event.player.teleport(newLocation)
        }
    }

    @EventHandler
    fun onFoodLevelChangeEvent(event: FoodLevelChangeEvent) {
        if (event.entityType == EntityType.PLAYER) {
            if (event.entity.world.game?.phase == Game.Phase.WAITING) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val world = event.player.world
        val game = world.game ?: return

        if (game.phase == Game.Phase.WAITING || !game.playerIsTribute(event.player as Player)) {
            // don't let players open chests before the game starts and don't let spectators open chests
            event.isCancelled = true
        } else {
            val location = event.inventory.location ?: return
            when (world.getBlockAt(location).type) {
                Material.ENDER_CHEST -> {
                    event.isCancelled = true
                    event.player.openInventory(game.getOrCreateRandomChestInventoryAt(location))
                }
                Material.CHEST, in settings.bonusContainers -> {
                    game.chestComponent.onChestOpen(location)
                }
                else -> Unit
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val world = event.player.world
        val game = world.game ?: return

        if (game.inventoryIsRandomChest(event.inventory) && event.inventory.viewers.none { it != event.player }) {
            game.destroyRandomChestInventory(event.inventory)
        }

        event.inventory.location?.let { game.chestComponent.onChestClose(it) }
    }

    @EventHandler
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (event.entity.world.game == null) return

        if (event.entityType == EntityType.FALLING_BLOCK && event.to == Material.DRIED_KELP_BLOCK) {
            event.isCancelled = true
            event.block.type = Material.ENDER_CHEST
            event.block.chunk.isForceLoaded = false
        }
    }

    @EventHandler
    fun onEntityDropItem(event: EntityDropItemEvent) {
        if (event.entity.world.game == null) return

        if (event.entityType == EntityType.FALLING_BLOCK && event.itemDrop.itemStack.type == Material.DRIED_KELP_BLOCK) {
            event.isCancelled = true
            // look one block above. the falling block's location is the location of the block that broke it, so we want
            // to place the ender chest right above that block.
            val block = event.entity.world.getBlockAt(event.entity.location.clone().add(0.0, 1.0, 0.0))
            block.type = Material.ENDER_CHEST
            block.chunk.isForceLoaded = false
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val game = player.world.game ?: return
        if (!game.playerIsTribute(event.player)) {
            event.isCancelled = true // spectators may not interact
        }
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerPostRespawnEvent) {
        event.player.goToLobby() // just in case
    }

    @EventHandler
    fun onPlayerAdvancementCriterionGrant(event: PlayerAdvancementCriterionGrantEvent) {
        if (event.player.world.game != null) {
            event.isCancelled = true // no advancements, please uwu
        }
    }

    @EventHandler
    fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
        // to simplify things, currently spectators cannot chat. it's possible to make it so that spectators see only
        // spectator messages, and tributes see only tribute messages, but this should be okay for now
        if (event.player.world.game?.playerIsTribute(event.player) == false) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        event.world.game?.removeSomeChests(event.chunk)
    }

    @EventHandler
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        val game = event.player.world.game ?: return
        if (game.playerIsTribute(event.player)) {
            event.isCancelled = true // players are not allowed to fly
            event.player.allowFlight = false // disable double-space to fly
            event.player.isFlying = false // stop the player flying
            return
        }
    }

    @EventHandler
    fun onPlayerGameModeChange(event: PlayerGameModeChangeEvent) {
        val game = event.player.world.game ?: return
        if (game.playerIsTribute(event.player)) {
            event.isCancelled = true
            return
        }
    }
}
