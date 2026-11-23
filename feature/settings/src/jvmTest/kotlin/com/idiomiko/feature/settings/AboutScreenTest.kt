package com.idiomiko.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.IdiomikoTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/** About names the release the app was installed from. */
@OptIn(ExperimentalTestApi::class)
class AboutScreenTest {

    @Test
    fun aboutSaysWhichVersionThisIs() {
        val before = System.getProperty("idiomiko.version")
        System.setProperty("idiomiko.version", "0.11.2")
        try {
            runDesktopComposeUiTest {
                setContent { AboutScreen(onNavigate = {}) }
                onNodeWithTag("about-version").assertTextEquals("Version 0.11.2")
            }
        } finally {
            if (before == null) System.clearProperty("idiomiko.version") else System.setProperty("idiomiko.version", before)
        }
    }

    /** A wide window shows About beside the settings sidebar, with About lit. */
    @Test
    fun aWideWindowShowsAboutBesideTheSettingsSidebar() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        setContent { IdiomikoTheme { ProvideWindowWidth { AboutScreen(onNavigate = {}) } } }
        onNodeWithTag("settings-section-ABOUT").assertIsSelected()
        System.getenv("ABOUT_SCREENSHOT")?.let { path ->
            waitForIdle()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
        }
    }

    @Test
    fun aRunFromTheSourcesNamesNone() {
        if (System.getProperty("idiomiko.version") != null) return
        runDesktopComposeUiTest {
            setContent { AboutScreen(onNavigate = {}) }
            onNodeWithTag("about-version").assertDoesNotExist()
        }
    }
}
