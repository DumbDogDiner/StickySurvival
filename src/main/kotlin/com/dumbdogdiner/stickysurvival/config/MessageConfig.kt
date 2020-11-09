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

import com.dumbdogdiner.stickysurvival.config.language.BossBarMessages
import com.dumbdogdiner.stickysurvival.config.language.ChatMessages
import com.dumbdogdiner.stickysurvival.config.language.LobbyMessages
import com.dumbdogdiner.stickysurvival.config.language.TitleMessages

class MessageConfig(
    val chat: ChatMessages,
    val bossBar: BossBarMessages,
    val lobby: LobbyMessages,
    val title: TitleMessages,
) {
    constructor(cfg: ConfigHelper) : this(
        ChatMessages(cfg["chat"]),
        BossBarMessages(cfg["boss bar"]),
        LobbyMessages(cfg["lobby"]),
        TitleMessages(cfg["title"]),
    )
}
