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

package com.dumbdogdiner.stickysurvival

import com.dumbdogdiner.stickyapi.bukkit.util.StartupUtil
import com.dumbdogdiner.stickysurvival.command.SGCommand
import com.dumbdogdiner.stickysurvival.command.sgSetupCommand
import com.dumbdogdiner.stickysurvival.config.Config
import com.dumbdogdiner.stickysurvival.config.ConfigHelper
import com.dumbdogdiner.stickysurvival.listener.FasterWorldLoadsListener
import com.dumbdogdiner.stickysurvival.listener.GameEventsListener
import com.dumbdogdiner.stickysurvival.listener.GameMessageBroadcastListener
import com.dumbdogdiner.stickysurvival.listener.GameTriggersListener
import com.dumbdogdiner.stickysurvival.listener.LobbyHologramListener
import com.dumbdogdiner.stickysurvival.listener.PerWorldChatListener
import com.dumbdogdiner.stickysurvival.listener.PlayerJoinAndLeaveListener
import com.dumbdogdiner.stickysurvival.manager.AnimatedScoreboardManager
import com.dumbdogdiner.stickysurvival.manager.StatsManager
import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.util.info
import com.dumbdogdiner.stickysurvival.util.settings
import com.dumbdogdiner.stickysurvival.util.severe
import com.dumbdogdiner.stickysurvival.util.warn
import dev.jorel.commandapi.CommandAPI
import net.milkbowl.vault.economy.Economy
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.Database
import java.io.InputStreamReader

@kr.entree.spigradle.annotations.PluginMain
class StickySurvival : JavaPlugin() {
    override fun onLoad() {
        instance = this // THIS MUST RUN FIRST!

        // commandapi-shade init, part 1
        CommandAPI.onLoad(true) // Load with verbose output for testing
    }

    override fun onEnable() {
        version = description.version

        info("Attempting to load the AnimatedScoreboard plugin.")
        AnimatedScoreboardManager.plugin = server.pluginManager.getPlugin("AnimatedScoreboard") as JavaPlugin?

        info("Searching for and removing any broken holograms.")
        WorldManager.lobbyWorld.entities.asSequence().filter {
            it.persistentDataContainer.has(LobbyHologram.hologramKey, PersistentDataType.BYTE)
        }.forEach {
            it.remove() // this doesn't seem to work properly
        }

        info("Setting up configuration.")
        if (!StartupUtil.setupConfig(this)) {
            warn("Could not create configuration file.")
        } else {
            info("Setting up language file.")
            val langFile = dataFolder.resolve("language.yml")
            try {
                if (!langFile.exists()) saveResource("language.yml", false)
            } catch (e: Exception) {
                e.printStackTrace()
                warn("Could not create language file.")
            }
        }

        info("Loading default configuration as a fallback in case configuration is invalid.")
        loadDefaults()

        info("Checking worlds from configuration.")
        WorldManager.loadFromConfig()

        info("Registering commands.")

        // commandapi-shade init, part 2
        CommandAPI.onEnable(this)

        CommandAPI.registerCommand(SGCommand.javaClass)
        sgSetupCommand.register()

        for (
            listener in setOf(
                FasterWorldLoadsListener,
                LobbyHologramListener,
                GameEventsListener,
                GameMessageBroadcastListener,
                GameTriggersListener,
                PlayerJoinAndLeaveListener,
                PerWorldChatListener,
            )
        ) {
            info("Registering all events in ${listener::class.simpleName}.")
            server.pluginManager.registerEvents(listener, this)
        }

        info("Registering placeholders.")
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            StickySurvivalExpansion.register()
        }

        info("Getting Vault economy.")
        if (server.pluginManager.getPlugin("Vault") == null) {
            warn("Vault could not be found. Rewards will not be given.")
        } else {
            economy = server.servicesManager.getRegistration(Economy::class.java)?.provider
            if (economy == null) {
                warn("Vault was found, but an economy provider was not. Rewards will not be given. Did you forget Wallet?")
            }
        }

        info("Connecting to database and initializing statistics.")
        try {
            StatsManager.db = Database.connect(
                url = "jdbc:postgresql://${settings.db.host}:${settings.db.port}/${settings.db.database}",
                user = settings.db.username,
                password = settings.db.password,
            )
            StatsManager.init()
        } catch (e: Exception) {
            e.printStackTrace()
            StatsManager.db = null
            warn("Database setup failed. Leaderboards will not display and statistics will not be recorded.")
        }

        info("Survival Games is enabled!! :doggers:")
    }

    override fun onDisable() {
        info("Unloading worlds.")
        WorldManager.unloadAll()
        info("Survival Games is disabled.")
        this.name
    }

    private fun loadDefaults() {
        try {
            val langResource = getResource("language.yml") ?: throw IllegalStateException("Default language configuration is missing")
            defaultLanguageConfig = ConfigHelper(null, YamlConfiguration.loadConfiguration(InputStreamReader(langResource)))
            val resource = getResource("config.yml") ?: throw IllegalStateException("Default configuration is missing")
            defaultConfig = ConfigHelper(null, YamlConfiguration.loadConfiguration(InputStreamReader(resource)))
            Config.default = Config(defaultConfig)
        } catch (e: Exception) {
            e.printStackTrace()
            severe("The default (internal) configuration is incorrect or could not be loaded! This should not happen, but if your configuration is valid and 100% complete, nothing bad should occur.")
        }
    }

    companion object {
        // the Vault economy
        var economy = null as Economy?

        // the instance of the plugin
        lateinit var instance: StickySurvival

        // the version of the plugin
        lateinit var version: String

        // ConfigHelper built from the defaults
        lateinit var defaultConfig: ConfigHelper

        // ConfigHelper built from the default language file
        lateinit var defaultLanguageConfig: ConfigHelper
    }
}
