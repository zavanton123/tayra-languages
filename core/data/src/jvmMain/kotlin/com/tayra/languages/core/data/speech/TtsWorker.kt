package com.tayra.languages.core.data.speech

import co.touchlab.kermit.Logger
import com.tayra.languages.core.data.runtime.ManagedPython
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File

/** The Python speech worker (`tts_worker.py`), started on first use and kept alive so models load once. */
class TtsWorker(private val python: ManagedPython) {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var nextId = 0L

    /** Sends one request and returns the reply; throws on transport or worker errors. */
    suspend fun request(timeoutMs: Long, vararg fields: Pair<String, String>): JsonObject = lock.withLock {
        withContext(Dispatchers.IO) {
            if (!python.isInstalled) error("the speech runtime is not installed yet")
            val id = ++nextId
            val (w, r) = ensure()
            val payload = JsonObject(fields.associate { it.first to JsonPrimitive(it.second) } + ("id" to JsonPrimitive(id)))
            w.write(json.encodeToString(JsonObject.serializer(), payload)); w.newLine(); w.flush()
            val line = withTimeout(timeoutMs) { r.readLine() } ?: run { stopNow(); error("the speech worker exited") }
            val reply = json.parseToJsonElement(line).jsonObject
            reply["error"]?.jsonPrimitive?.content?.let { error(it) }
            reply
        }
    }

    /** Stops the worker so the next request starts a fresh one, for example after new packages were installed. */
    suspend fun restart() = lock.withLock { stopNow() }

    private fun ensure(): Pair<BufferedWriter, BufferedReader> {
        val current = process
        if (current != null && current.isAlive) return writer!! to reader!!
        stopNow()
        val started = ProcessBuilder(python.python.absolutePath, "-u", script().absolutePath).redirectErrorStream(false).start()
        Thread { started.errorStream.bufferedReader().useLines { lines -> lines.forEach { Logger.d { "tts: $it" } } } }.apply { isDaemon = true; start() }
        process = started
        writer = started.outputStream.bufferedWriter()
        reader = started.inputStream.bufferedReader()
        return writer!! to reader!!
    }

    private fun stopNow() {
        runCatching { writer?.close() }
        runCatching { process?.destroy() }
        process = null; writer = null; reader = null
    }

    private fun script(): File {
        val target = File(System.getProperty("java.io.tmpdir"), "tayra-tts-worker.py")
        val bytes = TtsWorker::class.java.classLoader.getResourceAsStream("tts_worker.py")?.readBytes() ?: error("tts_worker.py is missing from the app")
        if (!target.exists() || !target.readBytes().contentEquals(bytes)) target.writeBytes(bytes)
        return target
    }
}
