package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Reads text aloud with the engine chosen in settings. A local engine synthesizes audio that the
 * app plays; when it has no voice for the language, or fails, the system voice speaks instead.
 * [playing] names the text from the moment it is asked for until the sound ends or is stopped.
 */
class Speaker(
    private val system: SpeechSynthesizer,
    private val player: WavPlayer,
    private val local: LocalSpeech,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    /** Increases with every request, so a late completion of an earlier one changes nothing. */
    private var request = 0

    private val _working = MutableStateFlow(false)

    /** True while a local engine is synthesizing, which can take a second or two. */
    val working: StateFlow<Boolean> = _working

    private val _playing = MutableStateFlow<String?>(null)

    /** The text being prepared or spoken, null when silent. */
    val playing: StateFlow<String?> = _playing

    /** Stops [text] when it is the one playing, and otherwise starts it. */
    fun toggle(text: String, languageCode: String?) {
        if (_playing.value == text) stop() else speak(text, languageCode)
    }

    fun speak(text: String, languageCode: String?) {
        stop()
        if (text.isBlank()) return
        val current = ++request
        _playing.value = text
        // Completions may arrive on audio threads; the state changes happen back on this scope's thread.
        val done: () -> Unit = { scope.launch { if (request == current) _playing.value = null } }
        val prefs = settings.current
        val engine = local.find(prefs.speechEngine)
        if (engine == null || languageCode == null) {
            system.speak(text, languageCode, done)
            return
        }
        job = scope.launch {
            _working.value = true
            val wav = runCatching { engine.synthesize(text, languageCode, prefs.speechVoices["${engine.engine.name}:$languageCode"], prefs.speechSpeed) }
                .onFailure { Logger.w { "${engine.displayName} could not speak: ${it.message}" } }
                .getOrNull()
            _working.value = false
            if (request != current) return@launch
            if (wav != null) player.play(wav, done) else system.speak(text, languageCode, done)
        }
    }

    fun stop() {
        request++
        job?.cancel()
        _working.value = false
        _playing.value = null
        player.stop()
        system.stop()
    }
}

/** A speaker scoped to the composition; [local] and [settings] come from dependency injection. */
@Composable
fun rememberSpeaker(local: LocalSpeech, settings: SettingsRepository): Speaker {
    val system = rememberSpeechSynthesizer()
    val player = rememberWavPlayer()
    val scope = rememberCoroutineScope()
    return remember(system, player, local, settings, scope) { Speaker(system, player, local, settings, scope) }
}
