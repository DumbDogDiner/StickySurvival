package com.dumbdogdiner.stickysurvival.listener

import com.dumbdogdiner.stickysurvival.manager.HiddenPlayerManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent

object PlayerInventoryListener : Listener {
    @EventHandler
    fun onPickup(event: EntityPickupItemEvent) {
        if (event.entity is Player) {
            if (HiddenPlayerManager.contains(event.entity as Player)) {
                event.isCancelled = true
            }
        }
    }
}