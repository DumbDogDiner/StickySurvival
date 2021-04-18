package com.dumbdogdiner.stickysurvival.listener

import com.dumbdogdiner.stickysurvival.event.TributeAddEvent
import com.dumbdogdiner.stickysurvival.event.TributeRemoveEvent
import com.dumbdogdiner.stickysurvival.util.messages
import com.thevoxelbox.voxelsniper.VoxelProfileManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object GameTriggersListener : Listener {

    private fun getVoxelProfileManager(): VoxelProfileManager? {
        if (Bukkit.getServer().pluginManager.isPluginEnabled("VoxelSniper")) {
            Bukkit.getLogger().info("(debug) gVPM: plugin enabled!")
            // Make sure the VoxelSniper plugin is loaded before trying to get VoxelProfileManager
            val voxelProfileManager = VoxelProfileManager.getInstance()

            Bukkit.getLogger().info("(debug) gVPM: is null: " + (voxelProfileManager == null).toString())

            if (voxelProfileManager != null) {
                Bukkit.getLogger().info("(debug) gVPM: successfully found!")
                // VoxelProfileManager is available, return the instance of it
                return voxelProfileManager
            } else {

                Bukkit.getLogger().info("(debug) gVPM: plugin enabled but instance is null!")
                // For whatever reason, the plugin is enabled but the instance is null.
                // This shouldn't happen!
                return null
            }
        } else {

            Bukkit.getLogger().info("(debug) gVPM: plugin is not loaded!")
            // Plugin is not loaded
            return null
        }
    }

    @EventHandler
    fun onTributeAdd(event: TributeAddEvent) {
        (getVoxelProfileManager())?.let { voxelProfileManager ->
            // Get the sniper for the player that joined a game
            val sniper = voxelProfileManager.getSniperForPlayer(event.player)

            // Disable the sniper
            sniper.isEnabled = false

            // Inform the player that their sniper has been disabled
            event.player.sendMessage(messages.voxelsniper.disabled)
        }
    }

    @EventHandler
    fun onTributeRemove(event: TributeRemoveEvent) {
        (getVoxelProfileManager())?.let { voxelProfileManager ->
            // Get the sniper for the player that joined a game
            val sniper = voxelProfileManager.getSniperForPlayer(event.player)

            // Enable the sniper if it is disabled
            if (!sniper.isEnabled) sniper.isEnabled = true

            // Inform the player that their sniper has been disabled
            event.player.sendMessage(messages.voxelsniper.enabled)
        }
    }
}
