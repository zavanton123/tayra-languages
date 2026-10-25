package com.tayra.languages.core.ui.audio

import com.tayra.languages.core.domain.playlists.SpeechOutput

/** The playlist player's sound on this device: [player] plays the clips a local engine made, [system] is the system's voice. */
class DeviceSpeechOutput(private val system: SpeechSynthesizer, private val player: WavPlayer) : SpeechOutput {
    private enum class Output { PLAYER, SYSTEM }

    /** Where the current sound comes from, so a pause reaches the right one. */
    private var sounding: Output? = null

    override fun play(wav: ByteArray, speed: Float, onDone: () -> Unit) {
        system.stop()
        sounding = Output.PLAYER
        player.play(wav, speed, onDone)
    }

    override fun speak(text: String, languageCode: String?, rate: Float, onWord: (IntRange) -> Unit, onDone: () -> Unit) {
        player.stop()
        sounding = Output.SYSTEM
        system.speak(text, languageCode, onDone, rate, onWord)
    }

    override fun pause(): Boolean = when (sounding) {
        Output.PLAYER -> player.pause()
        Output.SYSTEM -> system.pause()
        null -> false
    }

    override fun resume() = when (sounding) {
        Output.PLAYER -> player.resume()
        Output.SYSTEM -> system.resume()
        null -> {}
    }

    override fun stop() {
        sounding = null
        player.stop()
        system.stop()
    }
}
