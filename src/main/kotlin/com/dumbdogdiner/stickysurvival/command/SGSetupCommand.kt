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

import com.dumbdogdiner.stickysurvival.StickySurvival
import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.util.schedule
import com.dumbdogdiner.stickysurvival.util.spawn
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitPlayer
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits
import com.sk89q.worldedit.math.BlockVector3
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

private val worldEdit = WorldEdit.getInstance()

private val borderGetCommand = CommandAPICommand("get")
    .withPermission("stickysurvival.setup")
    .executesPlayer(
        PlayerCommandExecutor { sender, _ ->
            val selector = worldEdit.sessionManager[BukkitPlayer(sender)].getRegionSelector(BukkitWorld(sender.world))
            spawn {
                val success = withConfig(sender, mutates = false) {
                    val limits = ActorSelectorLimits.forActor(BukkitPlayer(sender))
                    val point1 = BlockVector3.at(it.getInt("bounds.min.x"), it.getInt("bounds.min.y"), it.getInt("bounds.min.z"))
                    val point2 = BlockVector3.at(it.getInt("bounds.max.x"), it.getInt("bounds.max.y"), it.getInt("bounds.max.z"))
                    selector.selectPrimary(point1, limits)
                    selector.selectSecondary(point2, limits)
                }
                if (success) sender.sendMessage("The border has been loaded into the region.")
            }
        }
    )

private val borderSetCommand = CommandAPICommand("set")
    .withPermission("stickysurvival.setup")
    .withRequirement { it is Player }
    .withRequirement { worldEdit.sessionManager[BukkitPlayer(it as Player)].isSelectionDefined(BukkitWorld(it.world)) }
    .executesPlayer(
        PlayerCommandExecutor { sender, _ ->
            val selection = worldEdit.sessionManager[BukkitPlayer(sender)].getSelection(BukkitWorld(sender.world))
            val min = selection.minimumPoint
            val max = selection.maximumPoint
            spawn {
                val success = withConfig(sender) {
                    it.set("bounds.min.x", min.x)
                    it.set("bounds.min.y", min.y)
                    it.set("bounds.min.z", min.z)
                    it.set("bounds.max.x", max.x)
                    it.set("bounds.max.y", max.y)
                    it.set("bounds.max.z", max.z)
                }
                if (success) sender.sendMessage("The border has been updated.")
            }
        }
    )

private val borderCommand = CommandAPICommand("border")
    .withPermission("stickysurvival.setup")
    .withSubcommand(borderGetCommand)
    .withSubcommand(borderSetCommand)

private val spawnAddCommand = CommandAPICommand("add")
    .withPermission("stickysurvival.setup")
    .executesPlayer(
        PlayerCommandExecutor { sender, _ ->
            spawn {
                val success = withConfig(sender) {
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
                if (success) sender.sendMessage("Spawn point added successfully.")
            }
        }
    )

private val spawnListCommand = CommandAPICommand("list")
    .withPermission("stickysurvival.setup")
    .executesPlayer(
        PlayerCommandExecutor { sender, _ ->
            spawn {
                withConfig(sender, mutates = false) {
                    sender.sendMessage("Spawn points for this world:")
                    it.getList("spawn points")?.withIndex()?.forEach { (i, spawnPoint) ->
                        sender.sendMessage("${i + 1}: $spawnPoint")
                    }
                }
            }
        }
    )

private val spawnRemoveCommand = CommandAPICommand("remove")
    .withPermission("stickysurvival.setup")
    .withArguments(IntegerArgument("index"))
    .executesPlayer(
        PlayerCommandExecutor { sender, args ->
            spawn {
                val index = args[0] as Int
                val success = withConfig(sender) {
                    val list = (it.getList("spawn points") ?: listOf()).toMutableList()
                    // bound check
                    if (index > 0 && index <= list.size) {
                        list.removeAt(index - 1)
                        it.set("spawn points", list)
                    } else {
                        sender.sendMessage("That spawn point is out of bounds - run /sgsetup spawn list")
                    }
                }
                if (success) sender.sendMessage("Spawn point removed successfully.")
            }
        }
    )

private val spawnCommand = CommandAPICommand("spawn")
    .withPermission("stickysurvival.setup")
    .withSubcommand(spawnAddCommand)
    .withSubcommand(spawnListCommand)
    .withSubcommand(spawnRemoveCommand)

val sgSetupCommand = CommandAPICommand("sgsetup")
    .withPermission("stickysurvival.setup")
    .withSubcommand(borderCommand)
    .withSubcommand(spawnCommand)

private fun withConfig(player: Player, mutates: Boolean = true, block: (YamlConfiguration) -> Unit): Boolean {
    val name = player.world.name
    val file = StickySurvival.instance.dataFolder.resolve("worlds").resolve("$name.yml")
    if (!file.exists()) {
        player.sendMessage("The config file doesn't exist! You'll have to create a stub yourself. (TODO add a config wizard)")
        return false
    }
    val config = YamlConfiguration.loadConfiguration(file)
    if (config.getKeys(true).isEmpty()) {
        player.sendMessage("Config file was empty when reading. Check the console! If the file couldn't be opened, try restarting the server.")
        return false
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

    return true
}
