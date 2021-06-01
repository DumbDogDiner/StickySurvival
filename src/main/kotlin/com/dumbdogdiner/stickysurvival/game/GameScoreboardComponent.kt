package com.dumbdogdiner.stickysurvival.game

import com.dumbdogdiner.stickysurvival.Game
import com.dumbdogdiner.stickysurvival.event.GameCloseEvent
import com.dumbdogdiner.stickysurvival.event.TributeAddEvent
import com.dumbdogdiner.stickysurvival.event.TributeRemoveEvent
import fr.mrmicky.fastboard.FastBoard
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import java.util.WeakHashMap

// TODO: put these strings into a language config
class GameScoreboardComponent(game: Game) : GameComponent(game) {
    private val activeBoards = WeakHashMap<Player, FastBoard>()

    @EventHandler
    fun onTributeAdd(event: TributeAddEvent) {
        val board = FastBoard(event.player)
        board.updateTitle("${ChatColor.BOLD}Survival Games")
        activeBoards[event.player] = board
        this.updateBoards()
    }

    @EventHandler
    fun onTributeRemove(event: TributeRemoveEvent) {
        if (event.game == this.game) {
            if (event.cause == TributeRemoveEvent.Cause.QUIT) {
                // Remove the board from the map
                val board = this.activeBoards.remove(event.player)
                // Delete board if not null
                board?.delete()
            }
            // update board for everyone else, as tribute count and/or kill count may have changed
            this.updateBoards()
        }
    }

    @EventHandler
    fun onGameClose(event: GameCloseEvent) {
        if (event.game == this.game) {
            // to be safe, i guess..
            for (board in activeBoards.values) {
                board.delete()
            }
        }
    }

    private fun updateBoards() {
        for ((player, board) in this.activeBoards) {
            board.updateLines(
                "${ChatColor.LIGHT_PURPLE}${ChatColor.BOLD}Arena",
                game.config.friendlyName,
                "${ChatColor.GOLD}${ChatColor.BOLD}Kills",
                game.killsFor(player).toString(),
                "${ChatColor.GREEN}${ChatColor.BOLD}Tributes",
                game.tributesComponent.size.toString()
            )
        }
    }
}
