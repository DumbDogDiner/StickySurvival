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

import com.dumbdogdiner.stickyapi.bukkit.command.AsyncCommand
import com.dumbdogdiner.stickyapi.bukkit.command.ExitCode
import com.dumbdogdiner.stickysurvival.StickySurvival
import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.util.game
import com.dumbdogdiner.stickysurvival.util.goToLobby
import com.dumbdogdiner.stickysurvival.util.hasJoinPermission
import com.dumbdogdiner.stickysurvival.util.hasKitPermission
import com.dumbdogdiner.stickysurvival.util.hasKitsPermission
import com.dumbdogdiner.stickysurvival.util.hasLeavePermission
import com.dumbdogdiner.stickysurvival.util.hasReloadPermission
import com.dumbdogdiner.stickysurvival.util.schedule
import com.dumbdogdiner.stickysurvival.util.settings
import com.dumbdogdiner.stickysurvival.util.waitFor
import com.dumbdogdiner.stickysurvival.util.worlds
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class SGCommand(pluginInstance: StickySurvival) : AsyncCommand("survivalgames", pluginInstance) {
    init {
        aliases = listOf("sg")
        tabCompleter = TabCompleter { sender, _, _, args ->
            when {
                args.matches("kit", ANY) -> {
                    if (sender.hasKitPermission()) {
                        settings.kits.map { it.name }.filter { it.startsWith(args[1]) }.toMutableList()
                    } else {
                        mutableListOf()
                    }
                }

                args.matches("join", ANY) -> {
                    if (sender.hasJoinPermission()) {
                        worlds.keys.filter { it.startsWith(args[1]) }.toMutableList()
                    } else {
                        mutableListOf()
                    }
                }

                args.matches(ANY) -> {
                    setOf("join", "leave", "reload", "kit", "kits").filter {
                        sender.hasPermission("stickysurvival.$it") && it.startsWith(args[0])
                    }.toMutableList()
                }

                else -> mutableListOf()
            }
        }
    }

    override fun executeCommand(sender: CommandSender, commandLabel: String?, args: Array<out String>): ExitCode {
        return when {
            args.matches("join", ANY) -> sgJoin(sender, args[1])
            args.matches("leave") -> sgLeave(sender)
            args.matches("kit") -> sgKit(sender, null)
            args.matches("kit", ANY) -> sgKit(sender, args[1])
            args.matches("kits") -> sgKits(sender)
            args.matches("reload") -> sgReload(sender)
            args.matches("forcestart") -> {
                if (!sender.isOp) return ExitCode.EXIT_PERMISSION_DENIED
                if (sender !is Player) return ExitCode.EXIT_MUST_BE_PLAYER
                val game = sender.world.game ?: return ExitCode.EXIT_INVALID_STATE
                schedule { game.forceStartGame() }
                return ExitCode.EXIT_SUCCESS
            }

            else -> ExitCode.EXIT_INVALID_SYNTAX
        }
    }

    private fun sgJoin(sender: CommandSender, worldName: String): ExitCode {
        return when {
            !sender.hasJoinPermission()
            -> ExitCode.EXIT_PERMISSION_DENIED
            sender !is Player
            -> ExitCode.EXIT_MUST_BE_PLAYER
            sender.world.game != null
            -> ExitCode.EXIT_INVALID_STATE
            WorldManager.isPlayerWaitingToJoin(sender)
            -> ExitCode.EXIT_INVALID_STATE
            worldName !in worlds
            -> ExitCode.EXIT_INVALID_SYNTAX
            WorldManager.playerJoinCooldownExists(sender)
            -> ExitCode.EXIT_COOLDOWN

            else -> {
                try {
                    return if (!WorldManager.putPlayerInWorldNamed(sender, worldName)) {
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

    private fun sgLeave(sender: CommandSender): ExitCode {
        return when {
            !sender.hasLeavePermission()
            -> ExitCode.EXIT_PERMISSION_DENIED
            sender !is Player
            -> ExitCode.EXIT_MUST_BE_PLAYER
            sender.world.game == null
            -> ExitCode.EXIT_INVALID_STATE

            else -> {
                var success: Boolean? = null
                schedule { success = sender.goToLobby() }
                if (waitFor { success }) ExitCode.EXIT_SUCCESS else ExitCode.EXIT_ERROR
            }
        }
    }

    private fun sgKit(sender: CommandSender, kitName: String?): ExitCode {
        return when {
            !sender.hasKitPermission()
            -> ExitCode.EXIT_PERMISSION_DENIED
            sender !is Player
            -> ExitCode.EXIT_MUST_BE_PLAYER

            else -> {
                val game = sender.world.game ?: return ExitCode.EXIT_INVALID_STATE
                if (kitName == null) {
                    game.removeKit(sender)
                } else {
                    val kit = settings.kits.find {
                        it.name == kitName
                    } ?: return ExitCode.EXIT_INVALID_SYNTAX
                    game.setKit(sender, kit)
                }
                ExitCode.EXIT_SUCCESS
            }
        }
    }

    private fun sgKits(sender: CommandSender): ExitCode {
        return when {
            !sender.hasKitsPermission()
            -> ExitCode.EXIT_PERMISSION_DENIED

            else -> {
                sender.sendMessage("kits: ${settings.kits.joinToString(", ") { it.name }}")
                ExitCode.EXIT_SUCCESS
            }
        }
    }

    private fun sgReload(sender: CommandSender): ExitCode {
        return when {
            !sender.hasReloadPermission()
            -> ExitCode.EXIT_PERMISSION_DENIED

            else -> {
                StickySurvival.instance.reloadConfig()
                schedule { WorldManager.loadFromConfig() }
                ExitCode.EXIT_SUCCESS
            }
        }
    }
}
