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

package com.dumbdogdiner.stickysurvival.config.language

import com.dumbdogdiner.stickysurvival.config.ConfigHelper

class ChatMessages(
    val cooldown: String,
    val joining: String,
    val join: String,
    val leave: String,
    val death: String,
    val countdown: String,
    val countdownCancelled: String,
    val refill: String,
    val start: String,
    val damageEnabled: String,
    val winner: String,
    val draw: String,
    val reward: String,
    val border: String,
    val randomChestDrop: String,
    val kitSelect: String,
) {
    constructor(cfg: ConfigHelper) : this (
        cfg.loadMessage("cooldown"),
        cfg.loadMessage("joining"),
        cfg.loadMessage("join"),
        cfg.loadMessage("leave"),
        cfg.loadMessage("death"),
        cfg.loadMessage("countdown"),
        cfg.loadMessage("countdown cancelled"),
        cfg.loadMessage("refill"),
        cfg.loadMessage("start"),
        cfg.loadMessage("damage enabled"),
        cfg.loadMessage("winner"),
        cfg.loadMessage("draw"),
        cfg.loadMessage("reward"),
        cfg.loadMessage("border"),
        cfg.loadMessage("random chest drop"),
        cfg.loadMessage("kit select"),
    )
}
