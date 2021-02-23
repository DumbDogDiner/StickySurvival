package com.dumbdogdiner.stickysurvival.events

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class TributeAddEvent(val player: Player) : Event() { // , Cancellable {

    companion object {
        // can't call it handlers or getHandlers will think it's handlers from getHandlers()
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }
}
