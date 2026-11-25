package com.idiomiko.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.IdiomikoTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun aboutMarksTheNameAsATrademark() = runDesktopComposeUiTest {
        setContent { AboutScreen(onNavigate = {}) }
        assertEquals(1, onAllNodesWithText("Idiomiko™").fetchSemanticsNodes().size, "the heading, not the bar's wordmark")
        onNodeWithTag("about-trademark").assertExists()
    }

    /** A wide window shows About on its own, without the settings sidebar. */
    @Test
    fun aWideWindowShowsAboutWithoutTheSettingsSidebar() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        setContent { IdiomikoTheme { ProvideWindowWidth { AboutScreen(onNavigate = {}) } } }
        onNodeWithTag("settings-sidebar").assertDoesNotExist()
        System.getenv("ABOUT_SCREENSHOT")?.let { path ->
            waitForIdle()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
        }
    }

    /** A run from the sources names the release it was built after and how far past it. */
    @Test
    fun aRunFromTheSourcesNamesWhereItStands() = withProperties("idiomiko.version" to null, "idiomiko.sourceVersion" to "0.19.0-77-g12599fb") {
        runDesktopComposeUiTest {
            setContent { AboutScreen(onNavigate = {}) }
            onNodeWithTag("about-version").assertTextEquals("Version 0.19.0-77-g12599fb (from the sources)")
        }
    }

    @Test
    fun withNoVersionAtAllNoneIsNamed() = withProperties("idiomiko.version" to null, "idiomiko.sourceVersion" to null) {
        runDesktopComposeUiTest {
            setContent { AboutScreen(onNavigate = {}) }
            onNodeWithTag("about-version").assertDoesNotExist()
        }
    }

    private fun withProperties(vararg values: Pair<String, String?>, block: () -> Unit) {
        val before = values.associate { (key, _) -> key to System.getProperty(key) }
        try {
            values.forEach { (key, value) -> if (value == null) System.clearProperty(key) else System.setProperty(key, value) }
            block()
        } finally {
            before.forEach { (key, value) -> if (value == null) System.clearProperty(key) else System.setProperty(key, value) }
        }
    }
}
