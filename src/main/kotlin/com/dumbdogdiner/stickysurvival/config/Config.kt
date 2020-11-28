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

package com.dumbdogdiner.stickysurvival.config

import com.dumbdogdiner.stickysurvival.StickySurvival
import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.util.getKeyed
import com.dumbdogdiner.stickysurvival.util.substituteAmpersand
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.CompassMeta

class Config(
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
    val randomChestLoot = LootConfig(randomChestLootMin, randomChestLootMax, basicLoot.entries)

    private val trackingCompass = ItemStack(Material.COMPASS).also { item ->
        item.lore = trackingCompassLore.map { it.substituteAmpersand() }
        item.itemMeta = (item.itemMeta as CompassMeta).also { meta ->
            meta.setDisplayName(trackingCompassName.substituteAmpersand())
        }
    }

    @Suppress("unchecked_cast")
    constructor(cfg: ConfigHelper) : this(
        cfg["worlds"].map {
            val worldName = it.asString()
            val worldConfig = verifyAndLoad(worldName) ?: throw IllegalArgumentException("World $worldName cannot be loaded")
            worldName to worldConfig
        }.toMap(),
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
        cfg["breakable blocks"].map {
            getKeyed<Material>(it.asString())
        }.toSet(),
        cfg["bonus containers"].map { container ->
            val name = container.asString()
            if (name == "shulker_boxes") {
                Material.values().filter { it.key.key.endsWith("shulker_box") }
            } else {
                listOf(getKeyed(name))
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

        fun verifyAndLoad(worldName: String): WorldConfig? {
            if (!worldName.matches(worldNameRegex)) return null

            val worldFolder = Bukkit.getWorldContainer().resolve(worldName)
            if (!worldFolder.isDirectory) return null

            val configFile = worldFolder.resolve("survivalgames.yml")
            if (!configFile.exists()) return null

            return WorldConfig(worldName, ConfigHelper(null, YamlConfiguration.loadConfiguration(configFile)))
        }
    }
}
