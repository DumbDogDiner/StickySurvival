package com.dumbdogdiner.stickysurvival.events

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class TributeWinEvent(val player: Player?) : Event(), Cancellable {
    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }

    private var cancelled = false

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    // https://youtrack.jetbrains.com/issue/KT-6653
    // "Kotlin properties do not override Java-style getters and setters"
    // So we have to override the functions ourselves :(

    /**
     * Gets the cancellation state of this event. A cancelled event will not
     * be executed in the server, but will still pass to other plugins
     *
     * @return true if this event is cancelled
     */
    override fun isCancelled(): Boolean {
        return cancelled
    }

    /**
     * Sets the cancellation state of this event. A cancelled event will not
     * be executed in the server, but will still pass to other plugins.
     *
     * @param cancel true if you wish to cancel this event
     */
    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }
}
