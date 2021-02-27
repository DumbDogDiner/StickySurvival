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
import com.dumbdogdiner.stickysurvival.gui.KitGUI
import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.util.game
import com.dumbdogdiner.stickysurvival.util.goToLobby
import com.dumbdogdiner.stickysurvival.util.schedule
import com.dumbdogdiner.stickysurvival.util.settings
import com.dumbdogdiner.stickysurvival.util.spawn
import com.dumbdogdiner.stickysurvival.util.worlds
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

private fun inGame(sender: CommandSender) = (sender as? Player)?.world?.game != null

private val joinCommand = CommandAPICommand("join")
    .withPermission("stickysurvival.join")
    .withRequirement { it is Player }
    .withRequirement { !inGame(it) }
    .withRequirement { !WorldManager.isPlayerWaitingToJoin(it as Player) }
    .withArguments(
        GreedyStringArgument("world")
            .overrideSuggestions { _ -> worlds.values.map { it.friendlyName }.toTypedArray() }
    )
    .executesPlayer(
        PlayerCommandExecutor { player, args ->
            val worldName = worlds
                .asSequence()
                .filter { (_, world) -> world.friendlyName == args[0] as String }
                .firstOrNull()?.key
            if (worldName == null) {
                player.sendMessage("That world does not exist!")
            } else spawn {
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
    )

private val leaveCommand = CommandAPICommand("leave")
    .withPermission("stickysurvival.leave")
    .withRequirement { inGame(it) }
    .executesPlayer(
        PlayerCommandExecutor { player, _ ->
            schedule { player.goToLobby() }
        }
    )

private val kitCommand = CommandAPICommand("kit")
    .withPermission("stickysurvival.kit")
    .withRequirement { inGame(it) }
    .withRequirement { (it as Player).world.game!!.phase == Game.Phase.WAITING }
    .executesPlayer(
        PlayerCommandExecutor { player, _ ->
            KitGUI().open(player)
        }
    )

private val kitsCommand = CommandAPICommand("kits")
    .withPermission("stickysurvival.kits")
    .executes(
        CommandExecutor { sender, _ ->
            sender.sendMessage("kits: ${settings.kits.joinToString(", ") { it.name }}")
        }
    )

private val reloadCommand = CommandAPICommand("reload")
    .withPermission("stickysurvival.reload")
    .executes(
        CommandExecutor { sender, _ ->
            spawn {
                StickySurvival.instance.reloadConfig()
                schedule {
                    if (WorldManager.loadFromConfig()) {
                        sender.sendMessage("The configuration was reloaded successfully.")
                    } else {
                        sender.sendMessage("The configuration could not be reloaded. The default configuration is being used as a fallback. See the console for more information.")
                    }
                }
            }
        }
    )

private val forceStartCommand = CommandAPICommand("forcestart")
    .withPermission("stickysurvival.forcestart")
    .withRequirement { inGame(it) }
    .withRequirement { (it as Player).world.game!!.phase == Game.Phase.WAITING }
    .executesPlayer(
        PlayerCommandExecutor { player, _ ->
            schedule { player.world.game!!.forceStartGame() }
        }
    )

private val versionCommand = CommandAPICommand("version")
    .withPermission("stickysurvival.version")
    .executes(
        CommandExecutor { sender, _ ->
            sender.sendMessage("You are running version ${StickySurvival.version}")
        }
    )

val sgCommand = CommandAPICommand("survivalgames")
    .withAliases("sg")
    .withSubcommand(joinCommand)
    .withSubcommand(leaveCommand)
    .withSubcommand(kitCommand)
    .withSubcommand(kitsCommand)
    .withSubcommand(reloadCommand)
    .withSubcommand(forceStartCommand)
    .withSubcommand(versionCommand)
