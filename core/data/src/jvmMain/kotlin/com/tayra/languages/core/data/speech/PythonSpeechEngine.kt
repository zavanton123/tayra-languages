package com.tayra.languages.core.data.speech

import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/** What Piper and Kokoro share on the desktop: the managed Python, the worker and the progress line. */
abstract class PythonSpeechEngine(
    protected val python: ManagedPython,
    protected val worker: TtsWorker,
    /** The engine's name in the worker protocol. */
    private val workerName: String,
    /** The pip packages the engine needs. */
    private val pipPackages: List<String>,
    /** About how much pip downloads for [pipPackages] into a fresh Python. */
    private val pipDownloadSize: Long,
    /** What some of [pipPackages] are, shown while pip fetches them. */
    private val pipNotes: Map<String, String> = emptyMap(),
) : LocalSpeechEngine {

    override val hasRuntimeSetup: Boolean = true

    protected val progressState = MutableStateFlow<String?>(null)
    override val progress: StateFlow<String?> = progressState

    /** A few words on what is installed, appended to the status line. */
    protected abstract suspend fun installedSummary(): String

    /**
     * What an installation made by an older version of the app lacks, from the worker's status
     * reply; null when nothing. Such an installation is not ready, so setting up again adds it.
     */
    protected open fun missing(reply: JsonObject): String? = null

    /** About how much [setUp] downloads when only what [missing] reports is to be added. */
    protected open val missingDownloadSize: Long get() = pipDownloadSize

    private suspend fun statusReply(): JsonObject = worker.request(STATUS_TIMEOUT_MS, "cmd" to "status", "engine" to workerName)

    override suspend fun isReady(): Boolean = runCatching { missing(statusReply()) == null }.getOrDefault(false)

    override suspend fun status(): String = try {
        val reply = statusReply()
        val version = reply["version"]?.jsonPrimitive?.content ?: "?"
        val py = reply["python"]?.jsonPrimitive?.content?.let { " · Python $it" }.orEmpty()
        "$displayName $version$py · ${listOfNotNull(installedSummary(), missing(reply)).joinToString(" · ")}"
    } catch (e: Exception) {
        "$displayName is not installed yet: ${e.message}"
    }

    override suspend fun setUp(): String = withContext(Dispatchers.IO) {
        try {
            python.ensure { progressState.value = it }
            progressState.value = "Installing $displayName\u2026"
            python.pip(pipPackages, "Installing $displayName", pipNotes) { progressState.value = it }
            worker.restart()
        } finally {
            progressState.value = null
        }
        status()
    }

    override suspend fun runtimeDownloadSize(): Long? {
        val reply = runCatching { statusReply() }.getOrNull() ?: return pipDownloadSize + python.downloadSize
        return if (missing(reply) == null) null else missingDownloadSize
    }

    /** Runs one synthesis in the worker and returns the WAV bytes. */
    protected suspend fun synthesizeWith(vararg fields: Pair<String, String>): ByteArray {
        val out = withContext(Dispatchers.IO) { File.createTempFile("tayra-speech", ".wav") }
        try {
            worker.request(SYNTHESIS_TIMEOUT_MS, "cmd" to "synthesize", "engine" to workerName, "out" to out.absolutePath, *fields)
            return withContext(Dispatchers.IO) { out.readBytes() }
        } finally {
            out.delete()
        }
    }

    protected fun megabytes(bytes: Long): String = "${bytes / 1_000_000} MB"

    protected companion object {
        const val STATUS_TIMEOUT_MS = 60_000L
        const val SYNTHESIS_TIMEOUT_MS = 120_000L
    }
}
