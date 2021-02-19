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
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits
import com.sk89q.worldedit.math.BlockVector3
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
        } ?: return@cmd ExitCode.EXIT_ERROR_SILENT
        sender.sendMessage("Spawn point added successfully.")
        ExitCode.EXIT_SUCCESS
    } catch (e: Exception) {
        e.printStackTrace()
        sender.sendMessage(e.message ?: return@cmd ExitCode.EXIT_ERROR)
        ExitCode.EXIT_ERROR_SILENT
    }
}.requiresPlayer().permission("stickysurvival.setup")

private val spawnList = cmd("list") { sender, _, _ ->
    sender as Player
    try {
        withConfig(sender, mutates = false) {
            sender.sendMessage("Spawn points for this world:")
            it.getList("spawn points")?.withIndex()?.forEach { (i, spawnPoint) ->
                sender.sendMessage("${i + 1}: $spawnPoint")
            }
        } ?: return@cmd ExitCode.EXIT_ERROR_SILENT
        ExitCode.EXIT_SUCCESS
    } catch (e: Exception) {
        e.printStackTrace()
        sender.sendMessage(e.message ?: return@cmd ExitCode.EXIT_ERROR)
        ExitCode.EXIT_ERROR_SILENT
    }
}.requiresPlayer().permission("stickysurvival.setup")

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
        } ?: return@cmd ExitCode.EXIT_ERROR_SILENT
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
}.requiresPlayer().permission("stickysurvival.setup")

private val spawn = cmdStub("spawn")
    .subCommand(spawnAdd)
    .subCommand(spawnList)
    .subCommand(spawnRemove)
    .requiresPlayer()
    .permission("stickysurvival.setup")

private val borderGet = cmd("get") { sender, _, _ ->
    sender as Player
    val worldEdit = WorldEdit.getInstance()
    val session = worldEdit.sessionManager[BukkitPlayer(sender)]
    val selector = session.getRegionSelector(BukkitWorld(sender.world))
    withConfig(sender, mutates = false) {
        val limits = ActorSelectorLimits.forActor(BukkitPlayer(sender))
        val point1 = BlockVector3.at(it.getInt("bounds.min.x"), it.getInt("bounds.min.y"), it.getInt("bounds.min.z"))
        val point2 = BlockVector3.at(it.getInt("bounds.max.x"), it.getInt("bounds.max.y"), it.getInt("bounds.max.z"))
        selector.selectPrimary(point1, limits)
        selector.selectSecondary(point2, limits)
    } ?: return@cmd ExitCode.EXIT_ERROR_SILENT
    sender.sendMessage("The border has been loaded into the region.")
    ExitCode.EXIT_SUCCESS
}.requiresPlayer().permission("stickysurvival.setup")

private val borderSet = cmd("set") { sender, _, _ ->
    sender as Player
    val worldEdit = WorldEdit.getInstance()
    val world = BukkitWorld(sender.world)
    val session = worldEdit.sessionManager[BukkitPlayer(sender)]
    if (!session.isSelectionDefined(world)) {
        sender.sendMessage("You don't have a region selected!")
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
        } ?: return@cmd ExitCode.EXIT_ERROR_SILENT
        sender.sendMessage("The border has been updated.")
        ExitCode.EXIT_SUCCESS
    }
}.requiresPlayer().permission("stickysurvival.setup")

private val border = cmdStub("border")
    .subCommand(borderGet)
    .subCommand(borderSet)
    .onExecute { sender, _, _ ->
        sender.sendMessage("This command is now /sgsetup border set")
        ExitCode.EXIT_ERROR_SILENT
    }
    .requiresPlayer()
    .permission("stickysurvival.setup")

val sgSetupCommandBuilder: BukkitCommandBuilder = cmdStub("sgsetup")
    .requiresPlayer()
    .permission("stickysurvival.setup")
    .onTabComplete { _, _, args ->
        val argArray = args.rawArgs.toTypedArray()
        when {
            argArray.matches("border", ANY) -> {
                setOf("get", "set").filter { it.startsWith(argArray[1]) }.toMutableList()
            }

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

private fun withConfig(player: Player, mutates: Boolean = true, block: (YamlConfiguration) -> Unit): Unit? {
    val name = player.world.name
    val file = StickySurvival.instance.dataFolder.resolve("worlds").resolve("$name.yml")
    if (!file.exists()) {
        player.sendMessage("The config file doesn't exist! You'll have to create a stub yourself. (TODO add a config wizard)")
        return null
    }
    val config = YamlConfiguration.loadConfiguration(file)
    if (config.getKeys(true).isEmpty()) {
        player.sendMessage("Config file was empty when reading. Check the console! If the file couldn't be opened, try restarting the server.")
        return null
    }

    block(config)

    if (mutates) {
        config.save(file)
        schedule {
            if (!WorldManager.loadFromConfig()) {
                player.sendMessage("While reloading worlds after a /sgsetup command, something went wrong")
            }
        }
    }

    return Unit // is this smart? is this dumb? whatever this is, this is what happens when i code at 9 pm
}
