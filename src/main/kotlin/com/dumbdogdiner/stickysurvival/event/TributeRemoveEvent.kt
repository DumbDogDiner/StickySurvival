package com.dumbdogdiner.stickysurvival.event

import com.dumbdogdiner.stickysurvival.Game
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class TributeRemoveEvent(val player: Player, val game: Game, val cause: Cause) : Event() {
    enum class Cause { DEATH, QUIT }

    companion object {
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
