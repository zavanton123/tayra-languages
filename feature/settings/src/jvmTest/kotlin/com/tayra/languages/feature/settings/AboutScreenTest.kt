package com.tayra.languages.feature.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test

/** About names the release the app was installed from. */
@OptIn(ExperimentalTestApi::class)
class AboutScreenTest {

    @Test
    fun aboutSaysWhichVersionThisIs() {
        val before = System.getProperty("tayra.version")
        System.setProperty("tayra.version", "0.11.2")
        try {
            runDesktopComposeUiTest {
                setContent { AboutScreen(onNavigate = {}) }
                onNodeWithTag("about-version").assertTextEquals("Version 0.11.2")
            }
        } finally {
            if (before == null) System.clearProperty("tayra.version") else System.setProperty("tayra.version", before)
        }
    }

    @Test
    fun aRunFromTheSourcesNamesNone() {
        if (System.getProperty("tayra.version") != null) return
        runDesktopComposeUiTest {
            setContent { AboutScreen(onNavigate = {}) }
            onNodeWithTag("about-version").assertDoesNotExist()
        }
    }
}
