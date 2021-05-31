package com.dumbdogdiner.stickysurvival.task

import com.dumbdogdiner.stickysurvival.util.game
import fr.mrmicky.fastboard.FastBoard
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.util.WeakHashMap

class ScoreboardUpdateRunnable(val boards: WeakHashMap<Player, FastBoard>) : SafeRunnable() {
    override fun run() {
        boards.values.forEach {
            val player = it.player

            player.world.game?.let { game ->
                it.updateLines(
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
}
