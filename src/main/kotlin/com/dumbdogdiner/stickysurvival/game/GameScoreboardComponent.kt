package com.dumbdogdiner.stickysurvival.game

import com.dumbdogdiner.stickysurvival.Game
import com.dumbdogdiner.stickysurvival.event.GameCloseEvent
import com.dumbdogdiner.stickysurvival.event.GameStartEvent
import com.dumbdogdiner.stickysurvival.event.TributeRemoveEvent
import com.dumbdogdiner.stickysurvival.event.TributeWinEvent
import com.dumbdogdiner.stickysurvival.task.ScoreboardUpdateRunnable
import com.dumbdogdiner.stickysurvival.util.game
import com.dumbdogdiner.stickysurvival.util.info
import fr.mrmicky.fastboard.FastBoard
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import java.util.WeakHashMap

class GameScoreboardComponent(game: Game) : GameComponent(game) {
    private val activeBoards = WeakHashMap<Player, FastBoard>()
    private val task = ScoreboardUpdateRunnable(activeBoards)

    @EventHandler
    fun onGameStart(event: GameStartEvent) {
        // Step 1: Create a board for each player
        event.game.tributesComponent.tributeIterator {
            info("Iterating for Player: $it")
            val board = FastBoard(it)
            board.updateTitle("${ChatColor.BOLD}Survival Games")

            activeBoards[it] = board
        }

        // Step 2: start the runnable to update boards (every 20 ticks?)
        task.maybeRunTaskTimer(0, 20)
    }

    @EventHandler
    fun onTributeRemove(event: TributeRemoveEvent) {
        // Remove the board from the map
        val board = activeBoards.remove(event.player)
        // Delete board if not null
        board?.delete()
    }

    @EventHandler
    fun onTributeWin(event: TributeWinEvent) {
        if (event.player?.world?.game == game) task.safelyCancel()
    }

    @EventHandler
    fun onGameClose(event: GameCloseEvent) {
        if (event.game == game) task.safelyCancel()
    }
}
