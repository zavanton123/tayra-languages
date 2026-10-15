package com.tayra.languages.feature.terms.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.service.WordRecording
import com.tayra.languages.core.ui.audio.rememberAudioPlayback
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RecordingsSectionTest {

    @Test
    fun eachRecordingNamesItsSpeakerAndProject() = runDesktopComposeUiTest(width = 420, height = 200) {
        setContent {
            TayraTheme {
                CompositionLocalProvider(LocalRecordingPlayback provides rememberAudioPlayback()) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        RecordingsSection(
                            listOf(
                                WordRecording("semana", "https://example.org/a.mp3", "Santamarcanda", "Lingua Libre", "https://commons.wikimedia.org/wiki/A"),
                                WordRecording("semana", "https://example.org/b.mp3", "Jjunho", "Lingua Libre", "https://commons.wikimedia.org/wiki/B"),
                                WordRecording("ir", "https://example.org/c.mp3", "Someone", "Wiktionary", ""),
                            ),
                            term = "semana",
                        )
                    }
                }
            }
        }
        onNodeWithTag("recording-Santamarcanda").assertExists()
        onNodeWithText("Jjunho · Lingua Libre").assertExists()
        onNodeWithText("ir").assertExists()
        System.getenv("RECORDINGS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }
}
