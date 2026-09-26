package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import java.util.concurrent.Executors

/**
 * Drives the operating system's speech command, since the JVM has no text-to-speech of its own:
 * `say` on macOS, System.Speech through PowerShell on Windows, and `spd-say` or `espeak` on Linux.
 */
actual class SpeechSynthesizer {
    private val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "speech").apply { isDaemon = true } }
    private val os = System.getProperty("os.name").orEmpty().lowercase()

    @Volatile
    private var current: Process? = null

    private val macVoicesByLanguage: Map<String, String> by lazy {
        runCatching {
            val process = ProcessBuilder("say", "-v", "?").redirectErrorStream(true).start()
            process.inputStream.bufferedReader().readLines().also { process.waitFor() }
        }.getOrDefault(emptyList())
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s{2,}"))
                if (parts.size < 2) null else parts[1].trim().substringBefore('_').lowercase() to parts[0].trim()
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.first() }
    }

    actual fun speak(text: String, languageCode: String?) {
        stop()
        executor.execute {
            val command = command(text, languageCode?.lowercase())
            try {
                val process = ProcessBuilder(command).redirectErrorStream(true).start()
                current = process
                process.waitFor()
            } catch (e: Exception) {
                Logger.w(e) { "Could not run speech command ${command.first()}" }
            } finally {
                current = null
            }
        }
    }

    private fun command(text: String, language: String?): List<String> = when {
        os.contains("mac") -> {
            val voice = language?.let { macVoicesByLanguage[it] }
            if (voice != null) listOf("say", "-v", voice, text) else listOf("say", text)
        }
        os.contains("win") -> {
            val escaped = text.replace("'", "''")
            val select = if (language != null) {
                "\$voice = \$s.GetInstalledVoices() | Where-Object { \$_.VoiceInfo.Culture.TwoLetterISOLanguageName -eq '$language' } | Select-Object -First 1; " +
                    "if (\$voice) { \$s.SelectVoice(\$voice.VoiceInfo.Name) }; "
            } else ""
            listOf(
                "powershell", "-NoProfile", "-Command",
                "Add-Type -AssemblyName System.Speech; \$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; ${select}\$s.Speak('$escaped')",
            )
        }
        else -> if (language != null) listOf("spd-say", "-w", "-l", language, text) else listOf("spd-say", "-w", text)
    }

    actual fun stop() {
        current?.destroy()
        current = null
    }

    actual fun release() {
        stop()
        executor.shutdownNow()
    }
}

@Composable
actual fun rememberSpeechSynthesizer(): SpeechSynthesizer {
    val synthesizer = remember { SpeechSynthesizer() }
    DisposableEffect(synthesizer) { onDispose { synthesizer.release() } }
    return synthesizer
}
