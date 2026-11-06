package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Drives the operating system's speech command, since the JVM has no text-to-speech of its own:
 * `say` on macOS, System.Speech through PowerShell on Windows, and `spd-say` or `espeak` on Linux.
 */
actual class SpeechSynthesizer {
    private val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "speech").apply { isDaemon = true } }
    private val os = System.getProperty("os.name").orEmpty().lowercase()

    @Volatile
    private var current: Process? = null

    private val macVoices: List<String> by lazy { outputOf("say", "-v", "?") }

    private val linuxVoices: List<String> by lazy { outputOf("spd-say", "-L") }

    private val windowsHasCantonese: Boolean by lazy { outputOf(*windowsCantoneseVoiceCommand().toTypedArray()).any { it.isNotBlank() } }

    private val macVoicesByLanguage: Map<String, String> by lazy {
        macVoices
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s{2,}"))
                if (parts.size < 2) null else parts[1].trim().substringBefore('_').lowercase() to parts[0].trim()
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.first() }
    }

    private fun outputOf(vararg command: String): List<String> = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        process.inputStream.bufferedReader().readLines().also { process.waitFor() }
    }.getOrDefault(emptyList())

    /** The operating system's commands do not report words; `say` alone takes a rate. */
    actual fun speak(text: String, languageCode: String?, onDone: () -> Unit, rate: Float, onWord: ((IntRange) -> Unit)?) {
        stop()
        executor.execute {
            val command = command(text, languageCode?.lowercase(), rate)
            if (command == null) {
                onDone()
                return@execute
            }
            try {
                val process = ProcessBuilder(command).redirectErrorStream(true).start()
                current = process
                process.waitFor()
            } catch (e: Exception) {
                Logger.w(e) { "Could not run speech command ${command.first()}" }
            } finally {
                current = null
                onDone()
            }
        }
    }

    actual suspend fun hasVoice(languageCode: String?): Boolean {
        if (!CantoneseVoices.isCantonese(languageCode)) return true
        return withContext(Dispatchers.IO) {
            when {
                os.contains("mac") -> macCantoneseVoice(macVoices) != null
                os.contains("win") -> windowsHasCantonese
                else -> linuxCantoneseLanguage(linuxVoices) != null
            }
        }
    }

    /** The command that speaks [text], or null for Cantonese without a Cantonese voice. */
    private fun command(text: String, language: String?, rate: Float): List<String>? {
        val cantonese = CantoneseVoices.isCantonese(language)
        return when {
            os.contains("mac") -> {
                val voice = if (cantonese) macCantoneseVoice(macVoices) else language?.let { macVoicesByLanguage[it] }
                // `say` speaks about 175 words a minute unless told otherwise.
                val pace = if (rate != 1f) listOf("-r", (MAC_WORDS_PER_MINUTE * rate).toInt().toString()) else emptyList()
                if (cantonese && voice == null) null else (if (voice != null) listOf("say", "-v", voice) else listOf("say")) + pace + text
            }
            os.contains("win") -> windowsSpeechCommand(text, language)
            cantonese -> linuxCantoneseLanguage(linuxVoices)?.let { listOf("spd-say", "-w", "-l", it, text) }
            else -> if (language != null) listOf("spd-say", "-w", "-l", language, text) else listOf("spd-say", "-w", text)
        }
    }

    /**
     * The operating system's voices cannot be held: `say` keeps sounding what it has handed to the
     * audio system even when frozen, and `spd-say` only passes the text to a speech service. A
     * pause therefore stops, and the sentence is read again from its start.
     */
    actual fun pause(): Boolean = false

    actual fun resume() {}

    actual fun stop() {
        current?.destroy()
        current = null
    }

    actual fun release() {
        stop()
        executor.shutdownNow()
    }

    private companion object {
        const val MAC_WORDS_PER_MINUTE = 175
    }
}

private val MAC_VOICE = Regex("^(.+?)\\s+([A-Za-z]{2,3}(?:_\\w+)?)\\s+#")

/** The first Cantonese voice among the lines of `say -v ?`: Sinji               zh_HK    # 你好. Names may hold spaces. */
internal fun macCantoneseVoice(lines: List<String>): String? = lines
    .mapNotNull { MAC_VOICE.find(it)?.groupValues }
    .firstOrNull { CantoneseVoices.matches(it[2]) }
    ?.get(1)

/** The language of the first Cantonese voice among the lines of `spd-say -L`: name, language, variant. Names may hold spaces. */
internal fun linuxCantoneseLanguage(lines: List<String>): String? = lines
    .mapNotNull { it.trim().split(Regex("\\s+")).dropLast(1).lastOrNull() }
    .firstOrNull(CantoneseVoices::matches)

/** A PowerShell command that speaks [text] with System.Speech; for Cantonese it speaks only with a Cantonese voice. */
internal fun windowsSpeechCommand(text: String, language: String?): List<String> {
    val escaped = text.replace("'", "''")
    return windowsSpeech(
        when {
            CantoneseVoices.isCantonese(language) ->
                WINDOWS_CANTONESE_VOICE + "if (\$voice) { \$s.SelectVoice(\$voice.VoiceInfo.Name); \$s.Speak('$escaped') }"
            language != null ->
                windowsFindVoice("\$_.VoiceInfo.Culture.TwoLetterISOLanguageName -eq '$language'") +
                    "if (\$voice) { \$s.SelectVoice(\$voice.VoiceInfo.Name) }; \$s.Speak('$escaped')"
            else -> "\$s.Speak('$escaped')"
        },
    )
}

/** A PowerShell command that prints the name of a Cantonese voice, and nothing without one. */
internal fun windowsCantoneseVoiceCommand(): List<String> = windowsSpeech(WINDOWS_CANTONESE_VOICE + "if (\$voice) { \$voice.VoiceInfo.Name }")

private val WINDOWS_CANTONESE_VOICE = windowsFindVoice("\$_.VoiceInfo.Culture.Name -match '${CantoneseVoices.LOCALES}'")

private fun windowsFindVoice(filter: String) = "\$voice = \$s.GetInstalledVoices() | Where-Object { $filter } | Select-Object -First 1; "

private fun windowsSpeech(script: String) = listOf(
    "powershell", "-NoProfile", "-Command",
    "Add-Type -AssemblyName System.Speech; \$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; $script",
)

@Composable
actual fun rememberSpeechSynthesizer(): SpeechSynthesizer {
    val synthesizer = remember { SpeechSynthesizer() }
    DisposableEffect(synthesizer) { onDispose { synthesizer.release() } }
    return synthesizer
}
