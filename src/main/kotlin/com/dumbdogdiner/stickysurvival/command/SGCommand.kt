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

private val join = cmd("join", str("worldName")) { sender, args, _ ->
    sender as Player
    val worldName = args.getString("worldName")
    when {
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
                if (!WorldManager.putPlayerInWorldNamed(sender, worldName)) {
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
}.permission("stickysurvival.join").requiresPlayer()

private val leave = cmd("leave") { sender, _, _ ->
    sender as Player
    if (sender.world.game == null) {
        ExitCode.EXIT_INVALID_STATE
    } else {
        var success: Boolean? = null
        schedule { success = sender.goToLobby() }
        if (waitFor { success }) ExitCode.EXIT_SUCCESS else ExitCode.EXIT_ERROR
    }
}.permission("stickysurvival.leave").requiresPlayer()

private val kit = cmd("kit", str("kitName")) { sender, args, _ ->
    sender as Player
    val game = sender.world.game ?: return@cmd ExitCode.EXIT_INVALID_STATE
    if (game.phase != Game.Phase.WAITING) return@cmd ExitCode.EXIT_INVALID_STATE
    val kitName = args.getString("kitName")
    val kit = settings.kits.find { it.name == kitName } ?: return@cmd ExitCode.EXIT_INVALID_SYNTAX
    game.setKit(sender, kit)
    sender.sendMessage(messages.chat.kitSelect.safeFormat(kit.name))
    ExitCode.EXIT_SUCCESS
}.permission("stickysurvival.kit").requiresPlayer()

private val kits = cmd("kits") { sender, _, _ ->
    sender.sendMessage("kits: ${settings.kits.joinToString(", ") { it.name }}")
    ExitCode.EXIT_SUCCESS
}.permission("stickysurvival.kits")

private val reload = cmd("reload") { sender, _, _ ->
    StickySurvival.instance.reloadConfig()
    schedule {
        if (WorldManager.loadFromConfig()) {
            sender.sendMessage("The configuration was reloaded successfully.")
        } else {
            sender.sendMessage("The configuration could not be reloaded. The default configuration is being used as a fallback. See the console for more information.")
        }
    }
    ExitCode.EXIT_SUCCESS
}.permission("stickysurvival.reload")

private val forceStart = cmd("forcestart") { sender, _, _ ->
    sender as Player
    sender.world.game?.let {
        schedule { it.forceStartGame() }
        ExitCode.EXIT_SUCCESS
    } ?: ExitCode.EXIT_INVALID_STATE
}.permission("stickysurvival.forcestart").requiresPlayer()

private val version = cmd("version") { sender, _, _ ->
    sender.sendMessage("You are running version ${StickySurvival.version}")
    ExitCode.EXIT_SUCCESS
}.permission("stickysurvival.version")

val sgCommandBuilder: BukkitCommandBuilder = cmdStub("survivalgames").alias("sg")
    .onTabComplete { sender, _, args ->
        val argArray = args.rawArgs.toTypedArray()

        if (argArray.matches("kit", ANY) && sender.hasPermission("stickysurvival.kit")) {
            return@onTabComplete settings.kits.map { it.name }.filter { it.startsWith(argArray[1]) }.toMutableList()
        }

        if (argArray.matches("join", ANY) && sender.hasPermission("stickysurvival.join")) {
            return@onTabComplete worlds.keys.filter { it.startsWith(argArray[1]) }.toMutableList()
        }

        if (argArray.matches(ANY)) {
            return@onTabComplete setOf("join", "leave", "reload", "kit", "kits", "forcestart", "version").filter {
                sender.hasPermission("stickysurvival.$it") && it.startsWith(argArray[0])
            }.toMutableList()
        }

        mutableListOf()
    }
    .subCommand(join)
    .subCommand(leave)
    .subCommand(kit)
    .subCommand(kits)
    .subCommand(reload)
    .subCommand(forceStart)
    .subCommand(version)
