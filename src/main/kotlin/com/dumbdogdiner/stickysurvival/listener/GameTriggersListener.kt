package com.dumbdogdiner.stickysurvival.listener

import com.dumbdogdiner.stickysurvival.event.TributeAddEvent
import com.dumbdogdiner.stickysurvival.event.TributeRemoveEvent
import com.dumbdogdiner.stickysurvival.util.messages
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.command.tool.SelectionWand
import com.sk89q.worldedit.world.item.ItemType
import com.thevoxelbox.voxelsniper.VoxelProfileManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

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

    // private val worldEditWandStorage = ConcurrentHashMap<UUID, Tool>()

    private fun getWorldEditLocalSession(player: org.bukkit.entity.Player): LocalSession? {
        // Make sure WorldEdit is actually enabled
        if (Bukkit.getServer().pluginManager.isPluginEnabled("WorldEdit")) {
            val actor = BukkitAdapter.adapt(player)
            val sessionManager = WorldEdit.getInstance().sessionManager
            return sessionManager.get(actor)
        }
        return null
    }

    private fun getWorldEditWoodenAxeItemType(): ItemType {
        // Get the ItemType for a wooden axe (default wg wand)
        val item = ItemStack(Material.WOODEN_AXE)
        val baseItemStack = BukkitAdapter.adapt(item)
        return baseItemStack.type
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

        // WorldEdit
        (
            getWorldEditLocalSession(event.player)?.let { localSession ->
                val type = getWorldEditWoodenAxeItemType()

                // If gettool returns a value (ie. one is set) add it to our map for safekeeping
                // localSession.getTool(type)?.let { worldEditWandStorage.put(event.player.uniqueId, it) }

                event.player.sendMessage("Found tool of type: " + type.richName)
                event.player.sendMessage("Found getTool of: " + localSession.getTool(type))

                event.player.sendMessage("Matches wand item?" + type.id.equals(localSession.wandItem))

                event.player.sendMessage("Unbinding tool...")
                localSession.setTool(type, null)
                event.player.sendMessage("tool unbinded")
            }
            )
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

        (getWorldEditLocalSession(event.player))?.let { localSession ->
            // Re-bind to wooden axe
            val type = getWorldEditWoodenAxeItemType()

            event.player.sendMessage("Found tool of type: " + type.richName)
            event.player.sendMessage("Found getTool of: " + localSession.getTool(type))

            event.player.sendMessage("Matches wand item?" + type.id.equals(localSession.wandItem))

            localSession.setTool(type, SelectionWand())

            event.player.sendMessage("Re-bound selectionWand to wooden pick! Re-checking...")

            event.player.sendMessage("Found tool of type: " + type.richName)
            event.player.sendMessage("Found getTool of: " + localSession.getTool(type))

            event.player.sendMessage("Matches wand item?" + type.id.equals(localSession.wandItem))
        }
    }
}
