package com.idiomiko.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.service.AppUpdate
import com.idiomiko.core.domain.service.AppUpdateService
import com.idiomiko.core.domain.service.AppUpdater
import com.idiomiko.core.domain.service.UpdateInstaller
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import androidx.compose.ui.test.assertTextEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The offer of a new release across the top of the app, and the app's updates on their settings page. */
@OptIn(ExperimentalTestApi::class)
class AppUpdatesScreenTest {

    private val update = AppUpdate("0.6.0", "notes", "https://example/v0.6.0", UpdateInstaller("Idiomiko-0.6.0-macos-arm64.dmg", "https://example/x.dmg", 10, null))

    /** An updater that offers [offer] and holds the download until the test lets it go on. */
    private class FakeUpdater(var offer: AppUpdate?, override val currentVersion: String? = "0.5.0") : AppUpdater {
        val downloadGoesOn = CompletableDeferred<Unit>()
        var installed: String? = null
        override suspend fun check() = offer
        override suspend fun download(update: AppUpdate, onProgress: (Float?) -> Unit): String {
            onProgress(0.4f)
            downloadGoesOn.await()
            return "/tmp/new.dmg"
        }
        override suspend fun installAndRestart(installer: String) { installed = installer }
    }

    @Test
    fun theBannerOffersTheUpdateAndInstallsIt() = runDesktopComposeUiTest(width = 1200, height = 400) {
        val settings = SettingsRepositoryImpl(MapSettings())
        val updater = FakeUpdater(update)
        val service = AppUpdateService(updater, settings)
        setContent { Hosted(settings) { Column { AppUpdateBanner(service) } } }

        // The app checks when it starts, and offers what it found.
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Idiomiko 0.6.0 is available. You have 0.5.0.")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("UPDATE_SCREENSHOT")?.let { save(it) }
        onNodeWithTag("install-update").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Downloading Idiomiko 0.6.0…")).fetchSemanticsNodes().isNotEmpty() }
        updater.downloadGoesOn.complete(Unit)
        waitUntil(timeoutMillis = 5_000) { updater.installed == "/tmp/new.dmg" }
        onNodeWithText("Installing the update; Idiomiko will open again in a moment.").assertExists()
    }

    @Test
    fun laterPutsTheOfferAwayAndNothingIsOfferedWhenUpToDate() = runDesktopComposeUiTest(width = 1200, height = 400) {
        val settings = SettingsRepositoryImpl(MapSettings())
        val service = AppUpdateService(FakeUpdater(update), settings)
        setContent { Hosted(settings) { Column { AppUpdateBanner(service) } } }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Later")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("update-later").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Later")).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun theUpdatesPageShowsTheVersionAndChecksWhenAsked() = runDesktopComposeUiTest(width = 1580, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        val updater = FakeUpdater(null)
        val service = AppUpdateService(updater, settings)
        setContent { Hosted(settings) { UpdatesScreen(onNavigate = {}, onBack = {}, viewModel = UpdatesViewModel(settings, service)) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Version 0.5.0")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("settings-section-UPDATES").assertIsSelected()
        onNodeWithTag("settings-section-GENERAL").assertIsNotSelected()
        onNodeWithTag("check-updates").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("You have the latest version")).fetchSemanticsNodes().isNotEmpty() }

        updater.offer = update
        onNodeWithTag("check-updates").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Install and restart")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("UPDATE_SETTINGS_SCREENSHOT")?.let { save(it) }
    }

    @Test
    fun theSwitchSavesWhetherToCheckAtStart() = runDesktopComposeUiTest(width = 1580, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        val service = AppUpdateService(FakeUpdater(null), settings)
        setContent { Hosted(settings) { UpdatesScreen(onNavigate = {}, onBack = {}, viewModel = UpdatesViewModel(settings, service)) } }

        assertEquals(true, settings.current.checkForUpdates)
        onNode(isToggleable()).performClick()
        waitUntil(timeoutMillis = 5_000) { !settings.current.checkForUpdates }
        onNode(isToggleable()).performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.checkForUpdates }
    }

    /** A run from the sources names the release it was built after, says to update it with git, and offers neither the check nor the switch. */
    @Test
    fun aRunFromTheSourcesSaysToUpdateWithGit() = runDesktopComposeUiTest(width = 1580, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        val service = AppUpdateService(FakeUpdater(null, currentVersion = null), settings)
        val before = System.getProperty("idiomiko.sourceVersion")
        System.setProperty("idiomiko.sourceVersion", "0.19.0-77-g12599fb")
        try {
            setContent { Hosted(settings) { UpdatesScreen(onNavigate = {}, onBack = {}, viewModel = UpdatesViewModel(settings, service)) } }

            onNodeWithTag("updates-version").assertTextEquals("Version 0.19.0-77-g12599fb")
            onNodeWithText("A run from the sources; update it with git.").assertExists()
            onNodeWithTag("check-updates").assertDoesNotExist()
            assertTrue(onAllNodes(isToggleable()).fetchSemanticsNodes().isEmpty())
        } finally {
            if (before == null) System.clearProperty("idiomiko.sourceVersion") else System.setProperty("idiomiko.sourceVersion", before)
        }
    }

    /** The General page no longer holds the app's updates; they have a page of their own. */
    @Test
    fun theGeneralPageLeavesTheUpdatesOut() = runDesktopComposeUiTest(width = 1580, height = 2200) {
        val settings = SettingsRepositoryImpl(MapSettings())
        setContent { Hosted(settings) { SettingsScreen(onNavigate = {}, viewModel = SettingsViewModel(settings)) } }

        onNodeWithText("App updates").assertDoesNotExist()
        onNodeWithTag("check-updates").assertDoesNotExist()
        onNodeWithTag("settings-section-UPDATES").assertExists()
        System.getenv("UPDATE_GENERAL_SCREENSHOT")?.let { save(it) }
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
    }
}
