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

package com.dumbdogdiner.stickysurvival.config

import com.dumbdogdiner.stickysurvival.StickySurvival
import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.util.getMaterial
import com.dumbdogdiner.stickysurvival.util.substituteAmpersand
import com.dumbdogdiner.stickysurvival.util.warn
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.CompassMeta
import java.io.File

class Config(
    val db: DatabaseConfig,
    val worldConfigs: Map<String, WorldConfig>,
    val lobbySpawn: Location,
    val countdown: Int,
    val noDamageTime: Int,
    val reward: Double,
    val randomChestInterval: Long,
    randomChestLootMin: Int,
    randomChestLootMax: Int,
    trackingCompassName: String,
    trackingCompassLore: List<String>,
    val trackingCompassMessage: String,
    trackingCompassBasicWeight: Int,
    trackingCompassBonusWeight: Int,
    val resultsTime: Long,
    val joinCooldown: Long,
    val breakableBlocks: Set<Material>,
    val bonusContainers: Set<Material>,
    val messages: MessageConfig,
    basicLoot: LootConfig,
    bonusLoot: LootConfig,
    val kits: List<KitConfig>,
) {
    val randomChestLoot = LootConfig(randomChestLootMin, randomChestLootMax, bonusLoot.entries)

    private val trackingCompass = ItemStack(Material.COMPASS).also { item ->
        item.lore = trackingCompassLore.map { it.substituteAmpersand() }
        item.itemMeta = (item.itemMeta as CompassMeta).also { meta ->
            meta.displayName(Component.text(trackingCompassName.substituteAmpersand()))
        }
    }

    @Suppress("unchecked_cast")
    constructor(cfg: ConfigHelper) : this(
        DatabaseConfig(cfg["db"]),
        loadWorlds(cfg),
        cfg["lobby spawn"].let {
            if (it.isA(String::class) && it.asString() == "world spawn") {
                WorldManager.lobbyWorld.spawnLocation
            } else {
                Location(
                    WorldManager.lobbyWorld,
                    it["x"].asDouble(),
                    it["y"].asDouble(),
                    it["z"].asDouble(),
                    it["yaw"].asFloatOr(0f),
                    it["pitch"].asFloatOr(0f)
                )
            }
        },
        cfg["countdown"].asInt(),
        cfg["no damage time"].asInt(),
        cfg["reward"].asDouble(),
        cfg["random chest"]["interval"].asLong(),
        cfg["random chest"]["min items"].asInt(),
        cfg["random chest"]["max items"].asInt(),
        cfg["tracking compass"]["name"].asString(),
        cfg["tracking compass"]["lore"].map { it.asString() },
        cfg["tracking compass"]["message"].asString().substituteAmpersand(),
        cfg["tracking compass"]["basic loot weight"].asInt(),
        cfg["tracking compass"]["bonus loot weight"].asInt(),
        cfg["results time"].asLong(),
        cfg["join cooldown"].asLong(),
        cfg["breakable blocks"].map { getMaterial(it.asString()) }.toSet(),
        cfg["bonus containers"].map { container ->
            val name = container.asString()
            if (name.equals("shulker_boxes", ignoreCase = true)) {
                Material.values().filter { it.key.key.endsWith("shulker_box") }
            } else {
                listOf(getMaterial(name))
            }
        }.flatten().toSet(),
        MessageConfig(
            ConfigHelper(
                StickySurvival.defaultLanguageConfig,
                YamlConfiguration.loadConfiguration(StickySurvival.instance.dataFolder.resolve("language.yml"))
            )
        ),
        LootConfig(cfg["basic loot"]),
        LootConfig(cfg["bonus loot"]),
        cfg["kits"].mapEntries { name, c -> KitConfig(name, c) }
    )

    val basicLoot = basicLoot.let {
        val entries = it.entries.toMutableList().apply {
            repeat(trackingCompassBasicWeight) { add(trackingCompass) }
        }
        LootConfig(it.min, it.max, entries)
    }

    val bonusLoot = bonusLoot.let {
        val entries = it.entries.toMutableList().apply {
            repeat(trackingCompassBonusWeight) { add(trackingCompass) }
        }
        LootConfig(it.min, it.max, entries)
    }

    companion object {
        lateinit var default: Config
        lateinit var current: Config

        private val worldNameRegex = Regex("^[A-Za-z0-9_]+$")

        private fun loadWorlds(cfg: ConfigHelper): Map<String, WorldConfig> {
            if (cfg.isDefault()) {
                return mapOf()
            } else {
                val worldsFolder = StickySurvival.instance.dataFolder.resolve("worlds")
                val files = worldsFolder.listFiles()
                if (files == null) {
                    worldsFolder.mkdir()
                    return mapOf()
                } else {
                    return files.asSequence().filter { it.extension.toLowerCase() in arrayOf("yaml", "yml") }.mapNotNull {
                        val name = it.nameWithoutExtension
                        val config = try {
                            verifyAndLoad(name, it)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                        if (config == null) {
                            warn("Cannot load world $name")
                            null
                        } else {
                            name to config
                        }
                    }.toMap()
                }
            }
        }

        private fun verifyAndLoad(name: String, file: File): WorldConfig? {
            if (!name.matches(worldNameRegex)) return null

            val worldFolder = Bukkit.getWorldContainer().resolve(name)
            if (!worldFolder.isDirectory) return null

            return WorldConfig(name, ConfigHelper(null, YamlConfiguration.loadConfiguration(file)))
        }
    }
}
