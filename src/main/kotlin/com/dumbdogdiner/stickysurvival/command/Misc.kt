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

import com.dumbdogdiner.stickyapi.bukkit.command.BukkitCommandBuilder
import com.dumbdogdiner.stickyapi.common.arguments.Arguments
import com.dumbdogdiner.stickyapi.common.command.ExitCode
import org.bukkit.command.CommandSender

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

internal fun Arguments.end() {
    if (unparsedArgs.size > argsPositionField.getInt(this)) {
        invalidate("<too many arguments>")
    }
}

internal fun printError(exitCode: ExitCode, sender: CommandSender) {
    @Suppress("deprecation")
    com.dumbdogdiner.stickyapi.bukkit.command.ExitCode.valueOf(exitCode.name).message?.let { sender.sendMessage(it) }
}

internal class Argument(val name: String, val type: ArgumentType)

internal enum class ArgumentType { INT, STRING }

internal fun int(name: String) = Argument(name, ArgumentType.INT)
internal fun str(name: String) = Argument(name, ArgumentType.STRING)

internal fun cmd(name: String, vararg args: Argument, block: BukkitCommandBuilder.Executor): BukkitCommandBuilder {
    return BukkitCommandBuilder(name)
        .synchronous(false)
        .onError { err, sender, _, _ -> printError(err, sender) }
        .onExecute { sender, cmdArgs, vars ->
            for (arg in args) {
                when (arg.type) {
                    ArgumentType.INT -> cmdArgs.requiredInt(arg.name)
                    ArgumentType.STRING -> cmdArgs.requiredString(arg.name)
                }
            }
            cmdArgs.end()
            if (!cmdArgs.valid()) {
                return@onExecute ExitCode.EXIT_INVALID_SYNTAX
            }

            block.apply(sender, cmdArgs, vars)
        }
}

internal fun cmdStub(name: String): BukkitCommandBuilder {
    return BukkitCommandBuilder(name)
        .synchronous(false)
        .onError { err, sender, _, _ -> printError(err, sender) }
        .onExecute { _, _, _ -> ExitCode.EXIT_INVALID_SYNTAX }
}
