package com.idiomiko.core.domain.service

import com.idiomiko.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CancellationException

/**
 * Puts the installed app's `idiomiko` command on the PATH the first time the app runs, once: a
 * command the user later takes off the PATH stays off.
 */
class CommandLineSetup(
    private val tool: CommandLineTool,
    private val settings: SettingsRepository,
) {
    /**
     * Installs the command when this copy of the app has one, it was never set up here and no
     * terminal finds it yet. Returns what the install reported, with the reason when it failed,
     * or null when nothing was installed.
     */
    suspend fun run(): CommandLineStatus? {
        if (!tool.available || settings.current.commandLineSetUp) return null
        val result = try {
            if (tool.status().installed) null else tool.install()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CommandLineStatus(installed = false, error = e.message ?: e.toString())
        }
        // Marked after a failure too, so a launch does not try again each time.
        settings.update { it.copy(commandLineSetUp = true) }
        return result
    }
}
