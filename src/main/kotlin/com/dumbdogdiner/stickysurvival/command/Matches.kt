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

package com.dumbdogdiner.stickysurvival.command

// pattern matching objects
internal val REST = Any()
internal val ANY = Any()

/**
 * Helper function for pattern matching arrays.
 *
 * Use REST if the remaining elements of the array do not matter.
 *
 * Use ANY to allow any element in that place in the array.
 *
 * @param pattern The pattern to match.
 * @return True if the pattern matches, false if not.
 */
internal fun <T> Array<out T>.matches(vararg pattern: T): Boolean {
    for (element in pattern.withIndex()) {
        if (element.value === REST) {
            return true // the remaining elements do not matter
        } else if (this.size <= element.index) {
            return false // this array is too small to match
        } else if (element.value !== ANY && element.value != this[element.index]) {
            return false // this element does not match the pattern
        }
    }

    if (this.size != pattern.size) {
        return false // array is not the same size as the pattern
    }

    return true // we're good!
}
