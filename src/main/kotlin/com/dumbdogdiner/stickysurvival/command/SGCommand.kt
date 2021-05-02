/*
 * StickySurvival - an implementation of the Survival Games minigame
 * Copyright (C) 2020 Dumb Dog Diner <dumbdogdiner.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dumbdogdiner.stickysurvival.command

import com.dumbdogdiner.stickyapi.common.command.ExitCode
import com.dumbdogdiner.stickysurvival.Game
import com.dumbdogdiner.stickysurvival.StickySurvival
import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.util.game
import com.dumbdogdiner.stickysurvival.util.goToLobby
import com.dumbdogdiner.stickysurvival.util.schedule
import com.dumbdogdiner.stickysurvival.util.spawn
import com.dumbdogdiner.stickysurvival.util.worlds
import dev.jorel.commandapi.annotations.Alias
import dev.jorel.commandapi.annotations.Command
import dev.jorel.commandapi.annotations.Permission
import dev.jorel.commandapi.annotations.Subcommand
import dev.jorel.commandapi.annotations.arguments.AGreedyStringArgument
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Command("survivalgames")
@Alias("sg")
object SGCommand {
    @Subcommand("join")
    @Permission("stickysurvival.join")
    @JvmStatic fun join(player: Player, @AGreedyStringArgument name: String) {
        // TODO override tab suggestions for world name
        if (player.world.game != null) {
            player.sendMessage("You cannot run that command while in-game!")
            return
        }

        if (WorldManager.isPlayerWaitingToJoin(player)) {
            player.sendMessage("You are already joining a game!")
            return
        }
        val worldName = worlds
            .asSequence()
            .filter { (_, world) -> world.friendlyName == name }
            .firstOrNull()?.key
        if (worldName == null) {
            player.sendMessage("That world does not exist!")
            return
        }

        spawn {
            try {
                if (!WorldManager.putPlayerInWorldNamed(player, worldName)) {
                    printError(ExitCode.EXIT_INVALID_STATE, player)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.let { player.sendMessage(it) } ?: printError(ExitCode.EXIT_ERROR, player)
            }
        }
    }

    @Subcommand("leave")
    @Permission("stickysurvival.leave")
    @JvmStatic fun leave(player: Player) {
        if (player.world.game == null) {
            player.sendMessage("You are not in a game!")
            return
        }

        player.goToLobby()
    }

    @Subcommand("reload")
    @Permission("stickysurvival.reload")
    @JvmStatic fun reload(sender: CommandSender) {
        StickySurvival.instance.reloadConfig()
        schedule {
            if (WorldManager.loadFromConfig()) {
                sender.sendMessage("The configuration was reloaded successfully.")
            } else {
                sender.sendMessage("The configuration could not be reloaded. The default configuration is being used as a fallback. See the console for more information.")
            }
        }
    }

    @Subcommand("forcestart")
    @Permission("stickysurvival.forcestart")
    @JvmStatic fun forceStart(player: Player) {
        val game = player.world.game
        if (game == null) {
            player.sendMessage("You are not in a game!")
            return
        }

        if (game.phase != Game.Phase.WAITING) {
            player.sendMessage("The game has already begun!")
            return
        }

        game.forceStartGame()
    }

    @Subcommand("version")
    @Permission("stickysurvival.version")
    @JvmStatic fun version(sender: CommandSender) {
        sender.sendMessage("You are running version ${StickySurvival.version}")
    }
}
