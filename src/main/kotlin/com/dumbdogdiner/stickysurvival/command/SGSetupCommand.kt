/*
 * StickySurvival - an implementation of the Survival Games minigame
 * Copyright (C) 2021 Dumb Dog Diner <dumbdogdiner.com>
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
import com.dumbdogdiner.stickysurvival.StickySurvival
import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.util.schedule
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitPlayer
import com.sk89q.worldedit.bukkit.BukkitWorld
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

private val spawnAdd = cmd("add") { sender, _, _ ->
    sender as Player
    try {
        withConfig(sender) {
            val spawnPointsYaml = (it.getList("spawn points") ?: listOf()).toMutableList()
            spawnPointsYaml += mapOf(
                "x" to sender.location.x,
                "y" to sender.location.y,
                "z" to sender.location.z,
                "yaw" to sender.location.yaw,
                "pitch" to sender.location.pitch,
            )
            it.set("spawn points", spawnPointsYaml)
        }
        sender.sendMessage("Spawn point added successfully.")
        ExitCode.EXIT_SUCCESS
    } catch (e: Exception) {
        e.printStackTrace()
        sender.sendMessage(e.message ?: return@cmd ExitCode.EXIT_ERROR)
        ExitCode.EXIT_ERROR_SILENT
    }
}

private val spawnList = cmd("list") { sender, _, _ ->
    sender as Player
    try {
        withConfig(sender, mutates = false) {
            sender.sendMessage("Spawn points for this world:")
            it.getList("spawn points")?.withIndex()?.forEach { (i, spawnPoint) ->
                sender.sendMessage("${i + 1}: $spawnPoint")
            }
        }
        ExitCode.EXIT_SUCCESS
    } catch (e: Exception) {
        e.printStackTrace()
        sender.sendMessage(e.message ?: return@cmd ExitCode.EXIT_ERROR)
        ExitCode.EXIT_ERROR_SILENT
    }
}

private val spawnRemove = cmd("remove", int("index")) { sender, args, _ ->
    sender as Player
    try {
        val index = args.getInt("index")
        // bound check
        var success = false
        withConfig(sender) {
            val list = (it.getList("spawn points") ?: listOf()).toMutableList()
            if (index > 0 && index <= list.size) {
                list.removeAt(index - 1)
                it.set("spawn points", list)
                success = true
            }
        }
        if (success) {
            sender.sendMessage("Spawn point removed successfully.")
            ExitCode.EXIT_SUCCESS
        } else {
            sender.sendMessage("That spawn point is out of bounds - run /sgsetup spawn list")
            ExitCode.EXIT_ERROR_SILENT
        }
    } catch (e: Exception) {
        e.printStackTrace()
        sender.sendMessage(e.message ?: return@cmd ExitCode.EXIT_ERROR)
        ExitCode.EXIT_ERROR_SILENT
    }
}

private val spawn = cmdStub("spawn")
    .subCommand(spawnAdd)
    .subCommand(spawnList)
    .subCommand(spawnRemove)

private val border = cmd("border") { sender, _, _ ->
    sender as Player
    val worldEdit = WorldEdit.getInstance()
    val world = BukkitWorld(sender.world)
    val session = worldEdit.sessionManager[BukkitPlayer(sender)]
    if (!session.isSelectionDefined(world)) {
        sender.sendMessage("You don't have a region selected! (Use WorldEdit)")
        ExitCode.EXIT_ERROR_SILENT
    } else {
        val selection = session.getSelection(world)
        val min = selection.minimumPoint
        val max = selection.maximumPoint
        withConfig(sender) {
            it.set("bounds.min.x", min.x)
            it.set("bounds.min.y", min.y)
            it.set("bounds.min.z", min.z)
            it.set("bounds.max.x", max.x)
            it.set("bounds.max.y", max.y)
            it.set("bounds.max.z", max.z)
        }
        sender.sendMessage("The bounds have been updated.")
        ExitCode.EXIT_SUCCESS
    }
}

val sgSetupCommandBuilder: BukkitCommandBuilder = cmdStub("sgsetup")
    .requiresPlayer()
    .permission("stickysurvival.setup")
    .onTabComplete { _, _, args ->
        val argArray = args.rawArgs.toTypedArray()
        when {
            argArray.matches("spawn", ANY) -> {
                setOf("add", "list", "remove").filter { it.startsWith(argArray[1]) }.toMutableList()
            }

            argArray.matches(ANY) -> {
                setOf("border", "spawn").filter { it.startsWith(argArray[0]) }.toMutableList()
            }

            else -> mutableListOf()
        }
    }
    .subCommand(border)
    .subCommand(spawn)

private fun withConfig(player: Player, mutates: Boolean = true, block: (YamlConfiguration) -> Unit) {
    val name = player.world.name
    // this is good enough
    // if (name !in worlds) {
    //     throw IllegalArgumentException("No such world $name - make a config file for this world first and reload!")
    // }
    val file = StickySurvival.instance.dataFolder.resolve("worlds").resolve("$name.yml")
    if (!file.exists()) throw IllegalArgumentException("config file not exist")
    val config = YamlConfiguration.loadConfiguration(file)

    block(config)

    if (mutates) {
        config.save(file)
        schedule {
            if (!WorldManager.loadFromConfig()) {
                player.sendMessage("While reloading worlds after a /sgsetup command, something went wrong")
            }
        }
    }
}
