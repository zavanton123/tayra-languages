package com.tayra.languages.feature.terms.examples

import androidx.compose.runtime.Composable
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.service.SentenceAudioState
import com.tayra.languages.core.domain.service.SentenceAudio
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.ui.audio.AudioPlayback
import com.tayra.languages.core.ui.audio.Speaker
import com.tayra.languages.core.ui.audio.rememberAudioPlayback
import com.tayra.languages.core.ui.audio.rememberSpeaker
import com.tayra.languages.core.ui.components.AppIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** How an example's play button looks: which sound it plays and whether that is under way. */
data class ExampleSound(
    /** A speaker's recording plays rather than the speech engine. */
    val recorded: Boolean,
    val playing: Boolean,
    /** The recording is downloading, or the speech engine is preparing the sentence. */
    val loading: Boolean,
) {
    /** A recording shows a speaking person; the speech engine has the reader's speaker icon. */
    val icon: ImageVector get() = if (recorded) AppIcons.RecordVoiceOver else AppIcons.VolumeUp

    val description: String
        get() = when {
            loading && recorded -> "Loading recording"
            loading -> "Preparing speech"
            playing -> "Stop"
            recorded -> "Play recording"
            else -> "Read aloud"
        }
}

/**
 * Plays example sentences: the speaker's recording, or the chosen speech engine and voice when
 * the example has none or its recording fails to play. Pressing a playing example stops it.
 */
class ExampleAudio internal constructor(
    private val playback: AudioPlayback,
    private val speaker: Speaker,
    private val scope: CoroutineScope,
    private val speaking: State<String?>,
    private val synthesizing: State<Boolean>,
    internal val prepared: SentenceAudio,
    private val preparedStates: State<Map<String, SentenceAudioState>>,
) {
    fun soundOf(example: ExampleSentence): ExampleSound {
        val url = example.audioUrl
        val spoken = speaking.value == example.text
        val preparing = url == null && preparedStates.value[example.text] == SentenceAudioState.PREPARING
        return ExampleSound(
            recorded = url != null,
            playing = (url != null && playback.isPlaying(url)) || spoken,
            loading = (url != null && playback.isLoading(url)) || (spoken && synthesizing.value) || preparing,
        )
    }

    fun toggle(example: ExampleSentence, languageCode: String?) {
        val url = example.audioUrl
        val wasPlaying = speaker.playing.value == example.text || (url != null && playback.isPlaying(url))
        speaker.stop()
        playback.stop()
        if (wasPlaying) return
        if (url == null) speaker.speak(example.text, languageCode)
        else playback.toggle(url) { scope.launch { speaker.speak(example.text, languageCode) } }
    }
}

@Composable
fun rememberExampleAudio(): ExampleAudio {
    val playback = rememberAudioPlayback()
    val prepared = koinInject<SentenceAudio>()
    val speaker = rememberSpeaker(koinInject(), koinInject(), prepared)
    val scope = rememberCoroutineScope()
    val speaking = speaker.playing.collectAsState()
    val synthesizing = speaker.working.collectAsState()
    val preparedStates = prepared.states.collectAsState()
    return remember(playback, speaker, scope, prepared) { ExampleAudio(playback, speaker, scope, speaking, synthesizing, prepared, preparedStates) }
}

/**
 * Makes the speech of [examples] that have no recording ahead of time, as the reader does for its
 * sentences, again whenever they or the speech settings change; their buttons spin meanwhile.
 */
@Composable
fun ExampleAudio.PrepareSpeech(examples: List<ExampleSentence>, languageCode: String?) {
    val settings = koinInject<SettingsRepository>()
    val prefs by settings.settings.collectAsState()
    val texts = remember(examples) { examples.filter { it.audioUrl == null }.map { it.text } }
    LaunchedEffect(texts, languageCode, prefs.speechEngine, prefs.speechVoices, prefs.speechSpeed) {
        prepared.prepare(texts, languageCode)
    }
}
