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

import com.dumbdogdiner.stickysurvival.config.WorldConfig
import com.dumbdogdiner.stickysurvival.event.BossBarNeedsUpdatingEvent
import com.dumbdogdiner.stickysurvival.event.GameEnableDamageEvent
import com.dumbdogdiner.stickysurvival.event.GameStartEvent
import com.dumbdogdiner.stickysurvival.event.HologramNeedsUpdatingEvent
import com.dumbdogdiner.stickysurvival.event.StartCountdownEvent
import com.dumbdogdiner.stickysurvival.event.TributeRemoveEvent
import com.dumbdogdiner.stickysurvival.event.TributeWinEvent
import com.dumbdogdiner.stickysurvival.event.TributeWinRewardEvent
import com.dumbdogdiner.stickysurvival.event.UpdateStatsEvent
import com.dumbdogdiner.stickysurvival.game.GameBossBarComponent
import com.dumbdogdiner.stickysurvival.game.GameCarePackageComponent
import com.dumbdogdiner.stickysurvival.game.GameChestComponent
import com.dumbdogdiner.stickysurvival.game.GameChestRemovalComponent
import com.dumbdogdiner.stickysurvival.game.GameCountdownComponent
import com.dumbdogdiner.stickysurvival.game.GameSpawnPointComponent
import com.dumbdogdiner.stickysurvival.game.GameStartComponent
import com.dumbdogdiner.stickysurvival.game.GameTrackingCompassComponent
import com.dumbdogdiner.stickysurvival.game.GameTributesComponent
import com.dumbdogdiner.stickysurvival.manager.AnimatedScoreboardManager
import com.dumbdogdiner.stickysurvival.manager.WorldManager
import com.dumbdogdiner.stickysurvival.task.AutoQuitRunnable
import com.dumbdogdiner.stickysurvival.util.broadcastMessage
import com.dumbdogdiner.stickysurvival.util.broadcastSound
import com.dumbdogdiner.stickysurvival.util.callSafe
import com.dumbdogdiner.stickysurvival.util.info
import com.dumbdogdiner.stickysurvival.util.messages
import com.dumbdogdiner.stickysurvival.util.newWeakSet
import com.dumbdogdiner.stickysurvival.util.radiusForBounds
import com.dumbdogdiner.stickysurvival.util.reset
import com.dumbdogdiner.stickysurvival.util.safeFormat
import com.dumbdogdiner.stickysurvival.util.schedule
import com.dumbdogdiner.stickysurvival.util.settings
import com.dumbdogdiner.stickysurvival.util.spectate
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.ChatColor
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

    // people viewing the kit GUI - might put this into a component later
    val kitGUIViewers = newWeakSet<Player>()

    // Player metadata
    private val kills = WeakHashMap<Player, Int>()

    val countdownComponent = GameCountdownComponent(this)
    val spawnPointComponent = GameSpawnPointComponent(this)
    val tributesComponent = GameTributesComponent(this)

    // specific values that require updating one or both displays

    var winner = null as Player?
        private set(value) {
            field = value
            BossBarNeedsUpdatingEvent(this).callSafe()
            HologramNeedsUpdatingEvent(this).callSafe()
        }

    var phase = Phase.WAITING
        // setter is now internal as startGame() logic has been moved to GameStartComponent
        internal set(value) {
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

        // some components we don't need to hold onto, they will register themselves for events and unregister when the
        // game ends.
        GameBossBarComponent(this)
        GameCarePackageComponent(this)
        GameChestComponent(this)
        GameChestRemovalComponent(this)
        GameTrackingCompassComponent(this)
        GameStartComponent(this)

        schedule {
            AnimatedScoreboardManager.addWorld(world.name)
            world.isAutoSave = false
            world.worldBorder.size = 2.0 * radiusForBounds(
                xBounds = config.xBounds,
                zBounds = config.zBounds,
            )
            val centerX = config.xBounds.let { (it.start + it.endInclusive) / 2 }
            val centerZ = config.zBounds.let { (it.start + it.endInclusive) / 2 }
            world.worldBorder.setCenter(centerX, centerZ)
            BossBarNeedsUpdatingEvent(this).callSafe()
        }
    }

    fun enableDamage() {
        // mark that players can take damage
        noDamage = false
        // call the event
        GameEnableDamageEvent(this).callSafe()
    }

    fun playCountdownClick() {
        world.broadcastSound(Sound.BLOCK_NOTE_BLOCK_HAT, 1F, 1F)
    }

    fun forceStartGame() {
        StartCountdownEvent(this).callSafe()
    }

    fun startGame() {
        GameStartEvent(this).callSafe()
    }

    fun onPlayerQuit(player: Player) {
        if (player in tributesComponent) {
            TributeRemoveEvent(player, this, TributeRemoveEvent.Cause.QUIT).callSafe()
        }

        if (phase == Phase.WAITING) {
            spawnPointComponent.takePlayerSpawnPoint(player)
            player.reset()
        }

        if (world.players.none { it != player }) {
            WorldManager.unloadGame(this)
        } else {
            BossBarNeedsUpdatingEvent(this).callSafe()
            HologramNeedsUpdatingEvent(this).callSafe()
        }
    }

    fun onPlayerDeath(player: Player) {
        val killerMessage = player.killer?.let {
            awardKillTo(it)
            messages.title.killer.safeFormat(it.name)
        }

        TributeRemoveEvent(player, this, TributeRemoveEvent.Cause.DEATH).callSafe()

        for (item in player.inventory) {
            if (item != null && !item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                world.dropItemNaturally(player.location, item)
            }
        }

        player.spectate()

        player.showTitle(Title.title(Component.text(messages.title.death), Component.text(killerMessage ?: "")))
        world.broadcastSound(Vector(0, 20, 0), Sound.ENTITY_GENERIC_EXPLODE, 4F, 0.75F)
        BossBarNeedsUpdatingEvent(this).callSafe()
        HologramNeedsUpdatingEvent(this).callSafe()
    }

    fun checkForWinner() {
        if (phase != Phase.ACTIVE) return

        val w = tributesComponent.winner()

        when {
            tributesComponent.size == 0 -> {
                world.broadcastMessage("${ChatColor.RED}Zero players are left. This shouldn't happen. Tell the devs about this!")
            }
            w != null -> {
                world.broadcastMessage(messages.chat.winner.safeFormat(w.name))
                winner = w
            }
            else -> return
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
        UpdateStatsEvent(this).callSafe()

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

    fun killsFor(player: Player) = kills[player] ?: 0

    private fun awardKillTo(player: Player) {
        kills[player] = killsFor(player) + 1
    }
}
