package com.tayra.languages.core.data.speech

import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
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
) : LocalSpeechEngine {

    override val hasRuntimeSetup: Boolean = true

    protected val progressState = MutableStateFlow<String?>(null)
    override val progress: StateFlow<String?> = progressState

    /** A few words on what is installed, appended to the status line. */
    protected abstract suspend fun installedSummary(): String

    override suspend fun isReady(): Boolean = runCatching { worker.request(STATUS_TIMEOUT_MS, "cmd" to "status", "engine" to workerName) }.isSuccess

    override suspend fun status(): String = try {
        val reply = worker.request(STATUS_TIMEOUT_MS, "cmd" to "status", "engine" to workerName)
        val version = reply["version"]?.jsonPrimitive?.content ?: "?"
        val py = reply["python"]?.jsonPrimitive?.content?.let { " on Python $it" }.orEmpty()
        "$displayName $version$py. ${installedSummary()}"
    } catch (e: Exception) {
        "$displayName is not installed yet: ${e.message}"
    }

    override suspend fun setUp(): String = withContext(Dispatchers.IO) {
        try {
            python.ensure { progressState.value = it }
            progressState.value = "Installing $displayName\u2026"
            python.pip(pipPackages, "Installing $displayName") { progressState.value = it }
            worker.restart()
        } finally {
            progressState.value = null
        }
        status()
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
