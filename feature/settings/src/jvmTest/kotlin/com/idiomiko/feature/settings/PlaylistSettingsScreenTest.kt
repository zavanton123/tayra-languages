package com.idiomiko.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.playlists.PlaylistEnd
import com.russhwolf.settings.MapSettings
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** The Playlists page sets the pause between tracks, the end of a playlist and whether listening counts. */
@OptIn(ExperimentalTestApi::class)
class PlaylistSettingsScreenTest {

    @Test
    fun thePlayersSettingsAreSavedAsTheyAreChanged() = runDesktopComposeUiTest(width = 1580, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        setContent { Hosted(settings) { PlaylistSettingsScreen(onNavigate = {}, onBack = {}, viewModel = SettingsViewModel(settings)) } }

        onNodeWithTag("settings-section-PLAYLISTS").assertIsSelected()
        // The pause's length shows once there is a pause.
        onNodeWithTag("pause-seconds").assertDoesNotExist()
        onNodeWithTag("pause-between-tracks").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.playlistPauseBetweenTracks }
        onNodeWithContentDescription("Increase length of the pause").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.playlistPauseSeconds == 4 }

        onNodeWithTag("playlist-end").performClick()
        onNodeWithText("Play the next playlist").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.playlistEnd == PlaylistEnd.NEXT_PLAYLIST }

        onNodeWithTag("listening-counts").performClick()
        waitUntil(timeoutMillis = 5_000) { !settings.current.playlistCountsTowardGoal }
        assertFalse(settings.current.playlistCountsTowardGoal)
        System.getenv("PLAYLIST_SETTINGS_SCREENSHOT")?.let { save(it) }
    }

    @Test
    fun aPhoneGoesBackWithTheBarsArrow() = runDesktopComposeUiTest(width = 400, height = 900) {
        val settings = SettingsRepositoryImpl(MapSettings())
        var back = 0
        setContent { Hosted(settings) { PlaylistSettingsScreen(onNavigate = {}, onBack = { back++ }, viewModel = SettingsViewModel(settings)) } }

        onNodeWithTag("settings-sidebar").assertDoesNotExist()
        onNodeWithTag("listening-counts").assertExists()
        System.getenv("PLAYLIST_SETTINGS_SCREENSHOT")?.let { save(it.replace(".png", "-phone.png")) }
        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, back)
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
    }
}
