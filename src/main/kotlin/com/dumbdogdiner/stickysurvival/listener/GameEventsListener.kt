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
import com.dumbdogdiner.stickysurvival.StickySurvival
import com.dumbdogdiner.stickysurvival.event.TributeWinRewardEvent
import com.dumbdogdiner.stickysurvival.gui.KitGUI
import com.dumbdogdiner.stickysurvival.util.game
import com.dumbdogdiner.stickysurvival.util.goToLobby
import com.dumbdogdiner.stickysurvival.util.settings
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
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
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleFlightEvent
import java.util.WeakHashMap

object GameEventsListener : Listener {
    // Code in this listener should be kept pretty minimal. The Game class should do most of the work.
    // ...and yes, i am aware that some of this code is not too minimal. i'm working on it.

    // PlayerInteractEvent can fire lots of times when it should fire just once, so keep track of when players click to
    // ignore multiple events on the same tick
    private val clickTimes = WeakHashMap<Player, Int>()

    // Run later (2nd last), just before MONITOR
    // This way other plugins can listen for this event
    // and cancel it so the default logic doesn't run
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onTributeWinReward(event: TributeWinRewardEvent) {
        event.game.giveDefaultWinReward(event.player)
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val game = event.block.world.game ?: return
        if (event.player !in game.tributesComponent) {
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
            if (entity is Player && entity !in game.tributesComponent) {
                event.setIntensity(entity, 0.0) // spectators may not receive splash potion effects
            }
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val game = event.entity.world.game ?: return
        if (event.damager.type == EntityType.PLAYER && event.damager as Player !in game.tributesComponent) {
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
        if (event.player.type == EntityType.PLAYER && event.player !in game.tributesComponent) {
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
            if (entity !in game.tributesComponent) {
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

        if (game.phase == Game.Phase.WAITING || event.player as Player !in game.tributesComponent) {
            // probably a gui, don't mess with it
            if (event.inventory.holder == null) return

            // don't let players open chests before the game starts and don't let spectators open chests
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        // don't let players move items around in their inventory unless the game is active
        // this is to make clickable hotbar items work

        val player = event.whoClicked
        val game = player.world.game ?: return

        if (game.phase != Game.Phase.ACTIVE) {
            // game is not active
            if (event.clickedInventory == player.inventory) {
                // inventory is the player's inventory
                event.isCancelled = true
            }
        }
    }

    // run this before onPlayerInteract, because this will trigger a PlayerInteractEvent and we want to tell it to
    // ignore the event
    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        // same use as above event handler

        val player = event.player
        val game = player.world.game ?: return

        if (game.phase != Game.Phase.ACTIVE) {
            event.isCancelled = true
            clickTimes[player] = Bukkit.getCurrentTick()
        }
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
        if (player !in game.tributesComponent) {
            event.isCancelled = true // spectators may not interact
        }
        // if a GUI is already open, do not handle GUI stuff here
        if (player.openInventory.type == InventoryType.CHEST) return
        val hasClickableHotbarItems =
            player !in game.tributesComponent || // is a spectator, has spectator hotbar
                game.phase == Game.Phase.WAITING // game has not yet started, has pre-game hotbar
        if (hasClickableHotbarItems) {
            // multiple events might fire at the same time, remember when the last event fired in order to catch the
            // first and ignore the rest
            val currentTick = Bukkit.getCurrentTick()
            if (clickTimes[player] != currentTick) {
                clickTimes[player] = currentTick

                when (event.item?.type) {
                    Material.RED_BED -> if (player.hasPermission("stickysurvival.leave")) player.goToLobby()
                    Material.BOW -> KitGUI().open(player)
                    else -> Unit
                }
            }
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
    fun onAsyncChat(event: AsyncChatEvent) {
        // to simplify things, currently spectators cannot chat. it's possible to make it so that spectators see only
        // spectator messages, and tributes see only tribute messages, but this should be okay for now
        if (event.player.world.game?.tributesComponent?.contains(event.player) == false) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        val game = event.player.world.game ?: return
        if (event.player in game.tributesComponent) {
            event.isCancelled = true // players are not allowed to fly
            event.player.allowFlight = false // disable double-space to fly
            event.player.isFlying = false // stop the player flying
            return
        }
    }

    @EventHandler
    fun onPlayerGameModeChange(event: PlayerGameModeChangeEvent) {
        val game = event.player.world.game ?: return
        if (event.player in game.tributesComponent) {
            event.isCancelled = true
            return
        }
    }

    /**
     * Bukkit event handler to supplement GameTriggers
     * Prevents WorldEdit commands (eg. //wand, /tool, //pos1, etc) from running for players in-game
     */
    @EventHandler
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val game = event.player.world.game ?: return
        val pluginManager = Bukkit.getServer().pluginManager

        // Check if the player is in-game && WorldEdit is enabled
        if (
            event.player in game.tributesComponent &&
            pluginManager.isPluginEnabled("WorldEdit")
        ) {
            // Get the WorldEdit plugin class
            val worldEditPlugin = pluginManager.getPlugin("WorldEdit") as WorldEditPlugin

            // CommandManager command names are strings without the first '/'
            // eg. '//wand' in-game becomes '/wand'
            // eg. '/worldedit' in-game becomes 'worldedit'
            val commandManager = worldEditPlugin.worldEdit.platformManager.platformCommandManager.commandManager

            // Get the first section of the command being called
            val cmdMsg = event.message.split(" ")[0]

            // Strip the first character (the '/') from the string
            // Now the cmd val matches the same format as WorldEdit's CommandManager! (see above comment)
            val cmd = cmdMsg.substring(1)

            // Check if the command exists in the (WorldEdit) Command Manager
            // aka. check if the command was a WorldEdit command
            if (commandManager.containsCommand(cmd)) {
                // If so, log to console and cancel the event so the command doesn't run.
                StickySurvival.instance.logger.info("${event.player.name} tried to issue WorldEdit command '${event.message}' but is in-game!")
                event.isCancelled = true
            }
        }
    }
}
