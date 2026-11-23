package com.idiomiko.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.service.ClearStep
import com.idiomiko.core.ui.components.ScrollColumn
import kotlin.test.Test
import kotlin.test.assertEquals

/** Clearing from the Data page asks before it removes anything, then shows each step going and what failed. */
@OptIn(ExperimentalTestApi::class)
class ClearDataBlockTest {
    @Test
    fun clearingIsConfirmedFirstAndThenReported() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        var state by mutableStateOf(ClearDataUiState())
        var cleared = 0
        var backups = 0
        var done = 0
        setContent {
            Hosted(SettingsRepositoryImpl(MapSettings())) {
                // Laid out as the page column lays it out on the screen.
                Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ClearDataBlock(state, backupEnabled = true, onClear = { cleared++ }, onBackup = { backups++ }, onDone = { done++ })
                }
            }
        }
        onNodeWithText("Will be removed").assertExists()
        onNodeWithText("Will remain").assertExists()
        onNodeWithText("This action is permanent").assertExists()
        onNodeWithText("Cancel").assertDoesNotExist()
        onNodeWithTag("clear-backup").performClick()
        assertEquals(1, backups, "the reminder's button makes a backup")

        onNodeWithTag("clear-all").performClick()
        onNodeWithText("Clear all data?").assertExists()
        onAllNodesWithText("Cancel").onLast().performClick()
        assertEquals(0, cleared, "cancelling the dialog removes nothing")

        onNodeWithTag("clear-all").performClick()
        onNodeWithText("Clear everything").performClick()
        assertEquals(1, cleared)

        state = ClearDataUiState(started = true, running = ClearStep.DICTIONARIES)
        waitForIdle()
        onNodeWithText("This takes a moment; the items above show how far it is.").assertExists()

        state = ClearDataUiState(started = true, done = true, problems = mapOf(ClearStep.VOICES to "Piper: the folder is in use"))
        waitForIdle()
        onNodeWithText("Some things could not be removed").assertExists()
        onNodeWithText("Could not remove: Piper: the folder is in use").assertExists()
        onNodeWithTag("clear-done").performClick()
        assertEquals(1, done)
    }

    /** A phone stacks the two lists, the backup reminder and the permanent-action box, each with a full-width button. */
    @Test
    fun aPhoneConfirmsAndReportsFromTheStackedBlock() = runDesktopComposeUiTest(width = 400, height = 1500) {
        var state by mutableStateOf(ClearDataUiState())
        var cleared = 0
        var backups = 0
        setContent {
            Hosted(SettingsRepositoryImpl(MapSettings())) {
                ScrollColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PhoneClearDataBlock(state, backupEnabled = true, onClear = { cleared++ }, onBackup = { backups++ }, onDone = {})
                }
            }
        }
        onNodeWithText("Will be removed").assertExists()
        onNodeWithText("Will remain").assertExists()
        onNodeWithText("Clear all data").assertExists()

        onNodeWithTag("clear-backup").performScrollTo().performClick()
        assertEquals(1, backups)
        onNodeWithTag("clear-all").performScrollTo().performClick()
        onNodeWithText("Clear all data?").assertExists()
        onAllNodesWithText("Cancel").onLast().performClick()
        assertEquals(0, cleared, "cancelling the dialog removes nothing")
        onNodeWithTag("clear-all").performClick()
        onNodeWithText("Clear everything").performClick()
        assertEquals(1, cleared)

        state = ClearDataUiState(started = true, done = true)
        waitForIdle()
        onNodeWithText("Everything was cleared").assertExists()
        onNodeWithTag("clear-done").performScrollTo().assertExists()
    }
}
