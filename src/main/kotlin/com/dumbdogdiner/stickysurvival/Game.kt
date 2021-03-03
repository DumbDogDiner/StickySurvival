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

package com.dumbdogdiner.stickysurvival

import com.destroystokyo.paper.Title
import com.dumbdogdiner.stickysurvival.config.KitConfig
import com.dumbdogdiner.stickysurvival.config.WorldConfig
import com.dumbdogdiner.stickysurvival.event.BossBarNeedsUpdatingEvent
import com.dumbdogdiner.stickysurvival.event.GameEnableDamageEvent
import com.dumbdogdiner.stickysurvival.event.GameStartEvent
import com.dumbdogdiner.stickysurvival.event.HologramNeedsUpdatingEvent
import com.dumbdogdiner.stickysurvival.event.StartCountdownEvent
import com.dumbdogdiner.stickysurvival.event.StopCountdownEvent
import com.dumbdogdiner.stickysurvival.event.TributeAddEvent
import com.dumbdogdiner.stickysurvival.event.TributeRemoveEvent
import com.dumbdogdiner.stickysurvival.event.TributeWinEvent
import com.dumbdogdiner.stickysurvival.event.TributeWinRewardEvent
import com.dumbdogdiner.stickysurvival.game.GameBossBarComponent
import com.dumbdogdiner.stickysurvival.game.GameCarePackageComponent
import com.dumbdogdiner.stickysurvival.game.GameChestComponent
import com.dumbdogdiner.stickysurvival.game.GameChestRemovalComponent
import com.dumbdogdiner.stickysurvival.game.GameCountdownComponent
import com.dumbdogdiner.stickysurvival.game.GameSpawnPointComponent
import com.dumbdogdiner.stickysurvival.game.GameTrackingCompassComponent
import com.dumbdogdiner.stickysurvival.manager.AnimatedScoreboardManager
import com.dumbdogdiner.stickysurvival.manager.HiddenPlayerManager
import com.dumbdogdiner.stickysurvival.manager.LobbyInventoryManager
import com.dumbdogdiner.stickysurvival.manager.StatsManager
import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.stats.PlayerStats
import com.dumbdogdiner.stickysurvival.task.AutoQuitRunnable
import com.dumbdogdiner.stickysurvival.util.broadcastMessage
import com.dumbdogdiner.stickysurvival.util.broadcastSound
import com.dumbdogdiner.stickysurvival.util.callSafe
import com.dumbdogdiner.stickysurvival.util.freeze
import com.dumbdogdiner.stickysurvival.util.info
import com.dumbdogdiner.stickysurvival.util.loadPreGameHotbar
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.radiusForBounds
import com.dumbdogdiner.stickysurvival.util.reset
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.settings
import com.dumbdogdiner.stickysurvival.util.spectate
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.WeakHashMap
import kotlin.math.roundToLong

class Game(val world: World, val config: WorldConfig) {
    enum class Phase { WAITING, ACTIVE, COMPLETE }

    val noDamageTime = config.noDamageTime ?: settings.noDamageTime

    // Runnables
    val autoQuit = AutoQuitRunnable(this)

    // Player metadata
    private val kills = WeakHashMap<Player, Int>()
    private val kits = WeakHashMap<Player, KitConfig>()

    private val tributes = mutableSetOf<Player>()
    private val participants = mutableSetOf<Player>()

    val spawnPointComponent = GameSpawnPointComponent(this)

    // for debugging, trying to figure out why sometimes games end with zero players
    private val tributeLog = mutableListOf<String>()

    // specific values that require updating one or both displays

    var countdown = -1
        set(value) {
            field = value
            BossBarNeedsUpdatingEvent(this).callSafe()
        }

    var winner = null as Player?
        private set(value) {
            field = value
            BossBarNeedsUpdatingEvent(this).callSafe()
            HologramNeedsUpdatingEvent(this).callSafe()
        }

    var phase = Phase.WAITING
        private set(value) {
            field = value
            BossBarNeedsUpdatingEvent(this).callSafe()
            HologramNeedsUpdatingEvent(this).callSafe()
        }

    var noDamage = true
        private set(value) {
            field = value
            BossBarNeedsUpdatingEvent(this).callSafe()
        }

    init {
        AnimatedScoreboardManager.addWorld(world.name)
        world.isAutoSave = false
        world.worldBorder.size = 2.0 * radiusForBounds(
            centerX = config.center.x,
            centerZ = config.center.z,
            xBounds = config.xBounds,
            zBounds = config.zBounds,
        )
        world.worldBorder.setCenter(config.center.x, config.center.z)
        BossBarNeedsUpdatingEvent(this).callSafe()

        // some components we don't need to hold onto, they will register themselves for events and unregister when the
        // game ends.
        GameBossBarComponent(this)
        GameCarePackageComponent(this)
        GameCountdownComponent(this)
        GameChestComponent(this)
        GameChestRemovalComponent(this)
        GameTrackingCompassComponent(this)
    }

    fun enableDamage() {
        noDamage = false
        countdown = config.time

        for (tribute in tributes) {
            tribute.isInvulnerable = false
        }

        world.broadcastMessage(messages.chat.damageEnabled)

        GameEnableDamageEvent(this).callSafe()
    }

    fun addPlayer(player: Player): Boolean {
        if (!player.hasPermission("stickysurvival.join")) {
            return false // not allowed to join
        }

        if (phase != Phase.WAITING) {
            LobbyInventoryManager.saveInventory(player)
            player.spectate()
            return if (!player.teleport(tributes.random().location)) {
                player.reset()
                HiddenPlayerManager.remove(player)
                LobbyInventoryManager.restoreInventory(player)
                false
            } else {
                true
            }
        }

        LobbyInventoryManager.saveInventory(player)
        player.inventory.clear()
        if (!spawnPointComponent.givePlayerSpawnPoint(player)) {
            LobbyInventoryManager.restoreInventory(player)
            return false
        }
        tributes += player

        TributeAddEvent(player).callSafe()

        player.freeze()

        world.broadcastMessage(messages.chat.join.safeFormat(player.name))

        setKit(player, settings.kits.random())
        player.loadPreGameHotbar()

        if (tributes.size >= config.minPlayers && countdown == -1) {
            beginStartCountdown()
        }

        BossBarNeedsUpdatingEvent(this).callSafe()
        HologramNeedsUpdatingEvent(this).callSafe()
        return true
    }

    private fun beginStartCountdown() {
        countdown = settings.countdown
        playCountdownClick()
        StartCountdownEvent(this).callSafe()
    }

    fun playCountdownClick() {
        world.broadcastSound(Sound.BLOCK_NOTE_BLOCK_HAT, 1F, 1F)
    }

    fun forceStartGame() {
        beginStartCountdown()
    }

    fun startGame() {
        countdown = noDamageTime

        for (tribute in tributes) {
            participants += tribute
            tribute.reset(GameMode.SURVIVAL)
            kits[tribute]?.giveTo(tribute)
            tribute.isInvulnerable = true // no damage until noDamage is false
        }

        world.broadcastMessage(messages.chat.start.safeFormat(noDamageTime))

        logTributes()

        phase = Phase.ACTIVE

        GameStartEvent(this).callSafe()
    }

    fun onPlayerQuit(player: Player) {
        tributes -= player

        TributeRemoveEvent(player).callSafe()

        if (phase == Phase.WAITING) {
            spawnPointComponent.takePlayerSpawnPoint(player)
        } else {
            logTributes()
        }

        world.broadcastMessage(messages.chat.leave.safeFormat(player.name))

        if (phase == Phase.WAITING && tributes.size < config.minPlayers) {
            countdown = -1
            StopCountdownEvent(this).callSafe()
        }

        if (world.players.none { it != player }) {
            WorldManager.unloadGame(this)
        } else {
            checkForWinner()
            BossBarNeedsUpdatingEvent(this).callSafe()
            HologramNeedsUpdatingEvent(this).callSafe()
        }
    }

    fun onPlayerDeath(player: Player) {
        val killerMessage = player.killer?.let {
            awardKillTo(it)
            messages.title.killer.safeFormat(it.name)
        }

        tributes -= player

        TributeRemoveEvent(player).callSafe()

        logTributes()

        for (item in player.inventory) {
            if (item != null && !item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                world.dropItemNaturally(player.location, item)
            }
        }

        player.spectate()

        player.sendTitle(Title(messages.title.death, killerMessage))
        world.broadcastMessage(messages.chat.death.safeFormat(player.name))
        world.broadcastSound(Vector(0, 20, 0), Sound.ENTITY_GENERIC_EXPLODE, 4F, 0.75F)
        BossBarNeedsUpdatingEvent(this).callSafe()
        HologramNeedsUpdatingEvent(this).callSafe()

        checkForWinner()
    }

    fun checkForWinner() {
        if (phase != Phase.ACTIVE) return

        when (getTributesLeft()) {
            1 -> {
                winner = tributes.first().also { lastTribute ->
                    world.broadcastMessage(messages.chat.winner.safeFormat(lastTribute.name))
                }
            }
            0 -> {
                world.broadcastMessage("${ChatColor.RED}Zero players are left. This shouldn't happen. Tell the devs about this!")
                world.broadcastMessage("${ChatColor.RED}Maybe this debug information will help:")
                for (log in tributeLog) {
                    world.broadcastMessage(log)
                }
            }
            else -> {
                return
            }
        }

        finalizeGame()
    }

    fun endAsDraw() {
        /*
        Something odd I noticed: In the third-party plugin's source code, if the game ends by a death and multiple
        players remain, then the cash reward is split equally amongst the remaining players*. However, I can't seem to
        think of a situation where this can happen. On a similar note, at the moment, I am not differentiating between
        losses due to a draw, and deaths. Should this be a concern to you, lmk

        * https://github.com/ShaneBeeStudios/HungerGames/blob/master/src/main/java/tk/shanebee/hg/game/Game.java#L316
         */

        world.broadcastMessage(messages.chat.draw)
        finalizeGame()
    }

    private fun finalizeGame() {
        autoQuit.maybeRunTaskLater(settings.resultsTime)

        val winner0 = winner

        info("(debug.jcx): finalizeGame() - Found potential winner: ${winner0?.name}")
        val event = TributeWinEvent(winner0)
        Bukkit.getPluginManager().callEvent(event)

        if (event.isCancelled) {
            info("(debug.jcx): event was cancelled! not updating stats or economy.")
            return
        }

        info("(debug.jcx): event still running! updating stats and economy...")

        for (player in participants) {
            StatsManager[player]?.let {
                var (uuid, wins, losses, kills) = it
                kills += killsFor(player)
                if (player == winner0) wins += 1 else losses += 1
                StatsManager[player] = PlayerStats(uuid, wins, losses, kills)
            }
        }
        StatsManager.updateTopStats()

        if (winner0 != null) {
            info("(debug.jcx): winner found! let's run an event to reward them...")
            TributeWinRewardEvent(winner0, this).callSafe()
        }

        phase = Phase.COMPLETE
    }

    fun giveDefaultWinReward(winner: Player) {
        info("(debug.jcx): triggered default reward event!")
        StickySurvival.economy?.depositPlayer(winner, settings.reward)
        winner.sendMessage(messages.chat.reward.safeFormat(if (settings.reward == settings.reward.roundToLong().toDouble()) settings.reward.toLong() else settings.reward))
    }

    fun playerIsTribute(player: Player) = player in tributes

    fun getTributesLeft() = tributes.size

    fun setKit(player: Player, kit: KitConfig) {
        kits[player] = kit
    }

    fun killsFor(player: Player) = kills[player] ?: 0

    private fun awardKillTo(player: Player) {
        kills[player] = killsFor(player) + 1
    }

    private fun logTributes() {
        tributeLog += "${tributes.joinToString()} at ${Bukkit.getServer().currentTick}"
    }
}
