package com.dumbdogdiner.stickysurvival.listener

import com.dumbdogdiner.stickysurvival.event.TributeAddEvent
import com.dumbdogdiner.stickysurvival.event.TributeRemoveEvent
import com.dumbdogdiner.stickysurvival.util.info
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.command.tool.SelectionWand
import com.sk89q.worldedit.world.item.ItemType
import com.thevoxelbox.voxelsniper.VoxelProfileManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

/**
 * GameTriggersListener
 *
 * Internal event listener for hooking into other plugins
 * (eg. to disable external plugin functionality during games)
 */
object GameTriggersListener : Listener {

    // Simple class for easier debug logging
    private class GTLLogger(val topic: String) {
        fun log(msg: String) {
            info("[GameTriggers $topic]: $msg")
        }
    }

    //region Utils (VoxelSniper, WorldEdit)

    private fun getVoxelProfileManager(): VoxelProfileManager? {
        // Create a instance of GTLLogger for logging
        val logger = GTLLogger("getVoxelProfileManager")

        if (Bukkit.getServer().pluginManager.isPluginEnabled("VoxelSniper")) {
            logger.log("plugin enabled!")
            // Make sure the VoxelSniper plugin is loaded before trying to get VoxelProfileManager
            val voxelProfileManager = VoxelProfileManager.getInstance()

            logger.log("is null: ${voxelProfileManager == null}")

            return if (voxelProfileManager != null) {
                logger.log("found voxelProfileManager!")
                // VoxelProfileManager is available, return the instance of it
                voxelProfileManager
            } else {
                logger.log("plugin enabled but instance is null!")
                // For whatever reason, the plugin is enabled but the instance is null.
                // This shouldn't happen!
                null
            }
        } else {
            logger.log("plugin is not loaded!")
            // Plugin is not loaded
            return null
        }
    }

    private fun getWorldEditLocalSession(player: Player): LocalSession? {
        // Make sure WorldEdit is actually enabled
        if (Bukkit.getServer().pluginManager.isPluginEnabled("WorldEdit")) {
            val actor = BukkitAdapter.adapt(player)
            val sessionManager = WorldEdit.getInstance().sessionManager
            return sessionManager[actor]
        }
        return null
    }

    private fun getWorldEditWoodenAxeItemType(): ItemType {
        // Get the ItemType for a wooden axe (default selection wand)
        val item = ItemStack(Material.WOODEN_AXE)
        val baseItemStack = BukkitAdapter.adapt(item)
        return baseItemStack.type
    }

    //endregion

    @EventHandler
    fun onTributeAdd(event: TributeAddEvent) {
        // Run only if we get a valid Voxel Profile Manager (ie. VoxelSniper is installed + running)
        getVoxelProfileManager()?.let { voxelProfileManager ->
            // Get the sniper for the player that joined a game
            val sniper = voxelProfileManager.getSniperForPlayer(event.player)

            // Disable the sniper
            sniper.isEnabled = false

            // Inform the player that their sniper has been disabled
            event.player.sendMessage(messages.external.featureDisabled.safeFormat("VoxelSniper"))
        }

        // Run only if we get a WorldEdit LocalSession for the player (ie. WorldEdit is installed + working)
        getWorldEditLocalSession(event.player)?.let { localSession ->
            val logger = GTLLogger("onTributeAdd/WorldEdit[${event.player.name}]")

            // Get the ItemType of the (default) selection wand
            val type = getWorldEditWoodenAxeItemType()

            // Print some debug info
            logger.log("Found tool of type: ${type.richName}")
            logger.log("Found tool: ${localSession.getTool(type)}")
            logger.log("Found tool matches wand item?: ${type.id == localSession.wandItem}")

            // Unbind the selection wand
            localSession.setTool(type, null)

            // Inform the player
            event.player.sendMessage(messages.external.featureDisabled.safeFormat("WorldEdit"))
        }
    }

    @EventHandler
    fun onTributeRemove(event: TributeRemoveEvent) {
        // Run only if we get a valid Voxel Profile Manager (ie. VoxelSniper is installed + running)
        getVoxelProfileManager()?.let { voxelProfileManager ->
            // Get the sniper for the player that joined a game
            val sniper = voxelProfileManager.getSniperForPlayer(event.player)

            // (Re-)enable the sniper
            sniper.isEnabled = true

            // Inform the player that their sniper has been disabled
            event.player.sendMessage(messages.external.featureEnabled.safeFormat("VoxelSniper"))
        }

        // Run only if we get a WorldEdit LocalSession for the player (ie. WorldEdit is installed + working)
        getWorldEditLocalSession(event.player)?.let { localSession ->
            val logger = GTLLogger("onTributeRemove/WorldEdit[${event.player.name}]")

            // Get the ItemType of the (default) selection wand
            val type = getWorldEditWoodenAxeItemType()

            // Print some debug info
            logger.log("Found tool of type: ${type.richName}")
            logger.log("Found tool: ${localSession.getTool(type)}")
            logger.log("Found tool matches wand item?: ${type.id == localSession.wandItem}")

            // Re-bind the selection wand to the wooden axe
            localSession.setTool(type, SelectionWand())

            // Inform the player
            event.player.sendMessage(messages.external.featureEnabled.safeFormat("WorldEdit"))

            // Print some more debug info (so we can check that the re-bind worked)
            logger.log("Re-bound selection wand! Re-checking...")
            logger.log("Found tool of type: ${type.richName}")
            logger.log("Found tool: ${localSession.getTool(type)}")
            logger.log("Found tool matches wand item?: ${type.id == localSession.wandItem}")
        }
    }
}
