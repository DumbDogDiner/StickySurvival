package com.dumbdogdiner.stickysurvival.game

import com.dumbdogdiner.stickysurvival.Game
import com.dumbdogdiner.stickysurvival.event.GameStartEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class GameStartComponent(game: Game) : GameComponent(game) {
    // Monitor priority: called LAST.
    // "Act when an event happens, but not change the outcome"
    @EventHandler(priority = EventPriority.MONITOR)
    fun onGameStart(event: GameStartEvent) {
        val game = event.game
        game.phase = Game.Phase.ACTIVE
        // Close inventory for those viewing the Kit GUI (this needs to be put somewhere else soon)
        game.kitGUIViewers.forEach { it.closeInventory() }
        game.kitGUIViewers.clear()
    }
}
