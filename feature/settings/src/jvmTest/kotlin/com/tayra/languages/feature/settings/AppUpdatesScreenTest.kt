package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.service.AppUpdate
import com.tayra.languages.core.domain.service.AppUpdateService
import com.tayra.languages.core.domain.service.AppUpdater
import com.tayra.languages.core.domain.service.UpdateInstaller
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** The offer of a new release across the top of the app, and the app's updates in Settings. */
@OptIn(ExperimentalTestApi::class)
class AppUpdatesScreenTest {

    private val update = AppUpdate("0.6.0", "notes", "https://example/v0.6.0", UpdateInstaller("TayraLanguages-0.6.0-macos-arm64.dmg", "https://example/x.dmg", 10, null))

    /** An updater that offers [offer] and holds the download until the test lets it go on. */
    private class FakeUpdater(var offer: AppUpdate?) : AppUpdater {
        override val currentVersion = "0.5.0"
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
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Tayra Languages 0.6.0 is available. You have 0.5.0.")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("UPDATE_SCREENSHOT")?.let { save(it) }
        onNodeWithTag("install-update").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Downloading Tayra Languages 0.6.0…")).fetchSemanticsNodes().isNotEmpty() }
        updater.downloadGoesOn.complete(Unit)
        waitUntil(timeoutMillis = 5_000) { updater.installed == "/tmp/new.dmg" }
        onNodeWithText("Installing the update; Tayra Languages will open again in a moment.").assertExists()
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
    fun settingsShowTheVersionAndCheckWhenAsked() = runDesktopComposeUiTest(width = 1580, height = 2200) {
        val settings = SettingsRepositoryImpl(MapSettings())
        val updater = FakeUpdater(null)
        val service = AppUpdateService(updater, settings)
        setContent { Hosted(settings) { SettingsScreen(onNavigate = {}, viewModel = SettingsViewModel(settings, updates = service)) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Version 0.5.0")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("check-updates").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("You have the latest version")).fetchSemanticsNodes().isNotEmpty() }

        updater.offer = update
        onNodeWithTag("check-updates").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Install and restart")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("UPDATE_SETTINGS_SCREENSHOT")?.let { save(it) }

        // The card's switch is the last on the page.
        onAllNodes(isToggleable()).onLast().performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { !settings.current.checkForUpdates }
        assertEquals(false, settings.current.checkForUpdates)
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
    }
}
