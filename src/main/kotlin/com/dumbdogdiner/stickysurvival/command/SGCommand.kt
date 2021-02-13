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

import com.dumbdogdiner.stickyapi.bukkit.command.BukkitCommandBuilder
import com.dumbdogdiner.stickyapi.common.arguments.Arguments
import com.dumbdogdiner.stickyapi.common.command.ExitCode
import com.dumbdogdiner.stickysurvival.Game
import com.dumbdogdiner.stickysurvival.StickySurvival
import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.util.game
import com.dumbdogdiner.stickysurvival.util.goToLobby
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.schedule
import com.dumbdogdiner.stickysurvival.util.settings
import com.dumbdogdiner.stickysurvival.util.waitFor
import com.dumbdogdiner.stickysurvival.util.worlds
import org.bukkit.entity.Player
import java.lang.reflect.Field

val sgCommandBuilder: BukkitCommandBuilder = BukkitCommandBuilder("survivalgames")
    .alias("sg")
    .synchronous(false)
    .onTabComplete { sender, _, args ->
        val argArray = args.rawArgs.toTypedArray()
        when {
            argArray.matches("kit", ANY) -> {
                if (sender.hasPermission("stickysurvival.kit")) {
                    settings.kits.map { it.name }.filter { it.startsWith(argArray[1]) }.toMutableList()
                } else {
                    mutableListOf()
                }
            }

            argArray.matches("join", ANY) -> {
                if (sender.hasPermission("stickysurvival.join")) {
                    worlds.keys.filter { it.startsWith(argArray[1]) }.toMutableList()
                } else {
                    mutableListOf()
                }
            }

            argArray.matches(ANY) -> {
                setOf("join", "leave", "reload", "kit", "kits", "forcestart").filter {
                    sender.hasPermission("stickysurvival.$it") && it.startsWith(argArray[0])
                }.toMutableList()
            }

            else -> mutableListOf()
        }
    }
    .onError { exit, sender, _, _ -> printError(exit, sender) }
    .onExecute { _, _, _ -> ExitCode.EXIT_INVALID_SYNTAX }
    .subCommand(
        BukkitCommandBuilder("join")
            .synchronous(false)
            .requiresPlayer()
            .permission("stickysurvival.join")
            .onError { exit, sender, _, _ -> printError(exit, sender) }
            .onExecute { sender, args, _ ->
                sender as Player
                args.requiredString("worldName")
                args.end()
                when {
                    !args.valid()
                    -> ExitCode.EXIT_INVALID_SYNTAX
                    sender.world.game != null
                    -> ExitCode.EXIT_INVALID_STATE
                    WorldManager.isPlayerWaitingToJoin(sender)
                    -> ExitCode.EXIT_INVALID_STATE
                    args.getString("worldName") !in worlds
                    -> ExitCode.EXIT_INVALID_SYNTAX
                    WorldManager.playerJoinCooldownExists(sender)
                    -> ExitCode.EXIT_COOLDOWN

                    else -> {
                        try {
                            if (!WorldManager.putPlayerInWorldNamed(sender, args.getString("worldName"))) {
                                ExitCode.EXIT_INVALID_STATE
                            } else {
                                ExitCode.EXIT_SUCCESS
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            ExitCode.EXIT_ERROR
                        }
                    }
                }
            }
    )
    .subCommand(
        BukkitCommandBuilder("leave")
            .synchronous(false)
            .requiresPlayer()
            .permission("stickysurvival.leave")
            .onError { exit, sender, _, _ -> printError(exit, sender) }
            .onExecute { sender, args, _ ->
                sender as Player
                args.end()
                when {
                    !args.valid()
                    -> ExitCode.EXIT_INVALID_SYNTAX
                    sender.world.game == null
                    -> ExitCode.EXIT_INVALID_STATE

                    else -> {
                        var success: Boolean? = null
                        schedule { success = sender.goToLobby() }
                        if (waitFor { success }) ExitCode.EXIT_SUCCESS else ExitCode.EXIT_ERROR
                    }
                }
            }
    )
    .subCommand(
        BukkitCommandBuilder("kit")
            .synchronous(false)
            .requiresPlayer()
            .permission("stickysurvival.kit")
            .onError { exit, sender, _, _ -> printError(exit, sender) }
            .onExecute { sender, args, _ ->
                sender as Player
                args.requiredString("kitName")
                args.end()
                when {
                    !args.valid()
                    -> ExitCode.EXIT_INVALID_SYNTAX

                    else -> {
                        val game = sender.world.game ?: return@onExecute ExitCode.EXIT_INVALID_STATE
                        if (game.phase != Game.Phase.WAITING) return@onExecute ExitCode.EXIT_INVALID_STATE
                        val kit = settings.kits.find {
                            it.name == args.getString("kitName")
                        } ?: return@onExecute ExitCode.EXIT_INVALID_SYNTAX
                        game.setKit(sender, kit)
                        sender.sendMessage(messages.chat.kitSelect.safeFormat(kit.name))
                        ExitCode.EXIT_SUCCESS
                    }
                }
            }
    )
    .subCommand(
        BukkitCommandBuilder("kits")
            .synchronous(false)
            .permission("stickysurvival.kits")
            .onError { exit, sender, _, _ -> printError(exit, sender) }
            .onExecute { sender, args, _ ->
                args.end()
                when {
                    !args.valid()
                    -> ExitCode.EXIT_INVALID_SYNTAX

                    else -> {
                        sender.sendMessage("kits: ${settings.kits.joinToString(", ") { it.name }}")
                        ExitCode.EXIT_SUCCESS
                    }
                }
            }
    )
    .subCommand(
        BukkitCommandBuilder("reload")
            .synchronous(false)
            .permission("stickysurvival.reload")
            .onError { exit, sender, _, _ -> printError(exit, sender) }
            .onExecute { sender, args, _ ->
                args.end()
                when {
                    !args.valid()
                    -> ExitCode.EXIT_INVALID_SYNTAX

                    else -> {
                        StickySurvival.instance.reloadConfig()
                        schedule {
                            if (WorldManager.loadFromConfig()) {
                                sender.sendMessage("The configuration was reloaded successfully.")
                            } else {
                                sender.sendMessage("The configuration could not be reloaded. The default configuration is being used as a fallback. See the console for more information.")
                            }
                        }
                        ExitCode.EXIT_SUCCESS
                    }
                }
            }
    )
    .subCommand(
        BukkitCommandBuilder("forcestart")
            .synchronous(false)
            .requiresPlayer()
            .permission("stickysurvival.forcestart")
            .onError { exit, sender, _, _ -> printError(exit, sender) }
            .onExecute { sender, args, _ ->
                sender as Player
                args.end()
                when {
                    !args.valid()
                    -> ExitCode.EXIT_INVALID_SYNTAX
                    else -> when (val game = sender.world.game) {
                        null -> ExitCode.EXIT_INVALID_STATE
                        else -> {
                            schedule { game.forceStartGame() }
                            ExitCode.EXIT_SUCCESS
                        }
                    }
                }
            }
    )

val argsPositionField: Field = run {
    val a = Arguments::class.java.getDeclaredField("position")
    a.isAccessible = true
    a
}
