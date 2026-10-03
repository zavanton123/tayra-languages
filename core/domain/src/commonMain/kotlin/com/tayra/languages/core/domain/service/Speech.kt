package com.tayra.languages.core.domain.service

import kotlinx.coroutines.flow.StateFlow

/** Which engine reads text aloud. */
enum class SpeechEngine(val label: String) {
    SYSTEM("System voices"),
    PIPER("Piper (offline, downloadable voices)"),
    KOKORO("Kokoro (offline, high quality)"),
}

/** Something an engine can download: one voice for Piper, the whole model for Kokoro. */
data class SpeechPackage(
    val id: String,
    val title: String,
    /** The app's language code the package serves, null when it serves several. */
    val languageCode: String?,
    /** The heading the package is listed under. */
    val group: String,
    val sizeBytes: Long,
    val installed: Boolean,
)

/** A voice that can speak right now. */
data class SpeechVoice(val id: String, val name: String, val languageCode: String)

/** A speech engine that runs on this device and produces audio the app plays itself. */
interface LocalSpeechEngine {
    val engine: SpeechEngine

    /** How the engine is named to the user. */
    val displayName: String

    /** One or two sentences for Settings on what it is. */
    val description: String

    /** What its packages are, for the package list in Settings. */
    val packagesDescription: String

    /** Whether the engine has a runtime of its own to install and check. */
    val hasRuntimeSetup: Boolean

    /** Whether [synthesize] honours its speed; the speed controls are hidden otherwise. */
    val supportsSpeed: Boolean get() = true

    /** What an install or download is doing right now, null when idle. */
    val progress: StateFlow<String?>

    /** A readable line about the installation, or what is wrong. */
    suspend fun status(): String

    suspend fun isReady(): Boolean

    /** Installs the engine's runtime. Returns a summary or throws. */
    suspend fun setUp(): String

    /** Everything the engine can download, with its installed state. */
    suspend fun packages(): List<SpeechPackage>

    suspend fun installPackage(id: String)

    suspend fun removePackage(id: String)

    /** The voices usable for [languageCode] with what is installed. */
    suspend fun voices(languageCode: String): List<SpeechVoice>

    /**
     * WAV audio of [text], or null when nothing installed can speak [languageCode].
     * [voiceId] picks among [voices]; [speed] is 1 for normal. Throws on failure.
     */
    suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray?
}

/** The speech engines this platform has besides the system voices. */
class LocalSpeech(val engines: List<LocalSpeechEngine>) {
    fun find(engine: SpeechEngine): LocalSpeechEngine? = engines.firstOrNull { it.engine == engine }

    /** Engines the user can choose here: the system voices plus the local ones. */
    val available: List<SpeechEngine> get() = listOf(SpeechEngine.SYSTEM) + engines.map { it.engine }
}
