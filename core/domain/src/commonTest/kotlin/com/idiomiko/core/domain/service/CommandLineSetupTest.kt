package com.idiomiko.core.domain.service

import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.domain.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandLineSetupTest {
    private class FakeTool(
        override val available: Boolean = true,
        var onPath: Boolean = false,
        private val failWith: String? = null,
    ) : CommandLineTool {
        var installs = 0
        override suspend fun status() = CommandLineStatus(installed = onPath)
        override suspend fun install(): CommandLineStatus {
            installs++
            if (failWith != null) return CommandLineStatus(installed = false, error = failWith)
            onPath = true
            return CommandLineStatus(installed = true, location = "/home/me/.local/bin/idiomiko", removable = true)
        }
        override suspend fun uninstall(): CommandLineStatus {
            onPath = false
            return status()
        }
    }

    private class MemorySettings(initial: UserSettings = UserSettings()) : SettingsRepository {
        private val state = MutableStateFlow(initial)
        override val settings: StateFlow<UserSettings> = state
        override suspend fun update(transform: (UserSettings) -> UserSettings) = state.update(transform)
    }

    @Test
    fun installsOnceWhenTheAppHasTheCommandAndNoTerminalFindsIt() = runTest {
        val tool = FakeTool()
        val settings = MemorySettings()

        val result = CommandLineSetup(tool, settings).run()

        assertEquals(1, tool.installs)
        assertTrue(result?.installed == true)
        assertTrue(settings.current.commandLineSetUp)
    }

    @Test
    fun aCommandRemovedAfterTheSetupIsNotPutBack() = runTest {
        val tool = FakeTool()
        val settings = MemorySettings()
        CommandLineSetup(tool, settings).run()
        tool.uninstall()

        assertNull(CommandLineSetup(tool, settings).run())

        assertEquals(1, tool.installs)
        assertFalse(tool.onPath)
    }

    @Test
    fun aRunFromTheSourcesDoesNothing() = runTest {
        val tool = FakeTool(available = false)
        val settings = MemorySettings()

        assertNull(CommandLineSetup(tool, settings).run())

        assertEquals(0, tool.installs)
        assertFalse(settings.current.commandLineSetUp, "an installed app run later still sets it up")
    }

    @Test
    fun aCommandAlreadyOnThePathIsOnlyMarked() = runTest {
        val tool = FakeTool(onPath = true)
        val settings = MemorySettings()

        assertNull(CommandLineSetup(tool, settings).run())

        assertEquals(0, tool.installs)
        assertTrue(settings.current.commandLineSetUp)
    }

    @Test
    fun aFailedInstallIsMarkedSoLaunchesDoNotRetryIt() = runTest {
        val tool = FakeTool(failWith = "Permission denied")
        val settings = MemorySettings()

        val result = CommandLineSetup(tool, settings).run()
        CommandLineSetup(tool, settings).run()

        assertEquals("Permission denied", result?.error)
        assertEquals(1, tool.installs)
        assertTrue(settings.current.commandLineSetUp)
    }
}
