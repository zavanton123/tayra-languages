package com.tayra.languages.core.data.translation

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.LocalSentenceTranslator
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File

/**
 * Argos Translate on the desktop: a Python worker (`argos_worker.py`, shipped in resources) is
 * started on first use and kept alive so the models load once. Requests are sent one at a time
 * as JSON lines. The Python executable comes from settings, defaulting to `python3` on the PATH.
 */
class ArgosSentenceTranslator(private val settings: SettingsRepository) : LocalSentenceTranslator {

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var startedWith: String? = null
    private var nextId = 0L

    override suspend fun translate(text: String, language: Language): String? {
        val source = LanguageCodes.codeFor(language.name) ?: return null
        val target = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }
        if (source == target) return null
        return runCatching {
            request(TRANSLATE_TIMEOUT_MS, "cmd" to "translate", "from" to source, "to" to target, "q" to text)["t"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotEmpty() }
        }.onFailure { Logger.w { "Argos translation failed: ${it.message}" } }.getOrNull()
    }

    override suspend fun status(): String = try {
        val reply = request(STATUS_TIMEOUT_MS, "cmd" to "status")
        val pairs = reply["pairs"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        val version = reply["version"]?.jsonPrimitive?.content ?: "?"
        if (pairs.isEmpty()) "Argos Translate $version is installed but has no language packages yet."
        else "Argos Translate $version with ${pairs.size} language pair${if (pairs.size == 1) "" else "s"}: ${pairs.joinToString(", ")}"
    } catch (e: Exception) {
        "Argos Translate is not available: ${e.message}"
    }

    override suspend fun installPackage(fromCode: String, toCode: String): String {
        val reply = request(INSTALL_TIMEOUT_MS, "cmd" to "install", "from" to fromCode, "to" to toCode)
        val installed = reply["installed"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        return if (installed.isEmpty()) "Packages for $fromCode→$toCode were already installed." else "Installed ${installed.joinToString(", ")}."
    }

    /** Sends one request and returns the reply object; throws on transport or worker errors. */
    private suspend fun request(timeoutMs: Long, vararg fields: Pair<String, String>): JsonObject = lock.withLock {
        withContext(Dispatchers.IO) {
            val id = ++nextId
            val (w, r) = ensureWorker()
            val payload = JsonObject(fields.associate { it.first to JsonPrimitive(it.second) } + ("id" to JsonPrimitive(id)))
            w.write(json.encodeToString(JsonObject.serializer(), payload)); w.newLine(); w.flush()
            val line = withTimeout(timeoutMs) { r.readLine() } ?: run { stop(); error("the Argos worker exited") }
            val reply = json.parseToJsonElement(line).jsonObject
            reply["error"]?.jsonPrimitive?.content?.let { error(it) }
            reply
        }
    }

    private fun ensureWorker(): Pair<BufferedWriter, BufferedReader> {
        val python = settings.current.argosPython.trim().ifEmpty { "python3" }
        val current = process
        if (current != null && current.isAlive && startedWith == python) return writer!! to reader!!
        stop()
        val script = extractScript()
        val started = try {
            ProcessBuilder(python, "-u", script.absolutePath).redirectErrorStream(false).start()
        } catch (e: Exception) {
            error("cannot run '$python' (${e.message}); set the Python executable in Settings")
        }
        // Keep stderr drained so the worker never blocks on a full pipe.
        Thread {
            started.errorStream.bufferedReader().useLines { lines -> lines.forEach { Logger.d { "argos: $it" } } }
        }.apply { isDaemon = true; start() }
        process = started
        startedWith = python
        writer = started.outputStream.bufferedWriter()
        reader = started.inputStream.bufferedReader()
        return writer!! to reader!!
    }

    private fun stop() {
        runCatching { writer?.close() }
        runCatching { process?.destroy() }
        process = null; writer = null; reader = null
    }

    private fun extractScript(): File {
        val target = File(System.getProperty("java.io.tmpdir"), "tayra-argos-worker.py")
        val bytes = ArgosSentenceTranslator::class.java.classLoader.getResourceAsStream("argos_worker.py")?.readBytes()
            ?: error("argos_worker.py is missing from the app")
        if (!target.exists() || !target.readBytes().contentEquals(bytes)) target.writeBytes(bytes)
        return target
    }

    private companion object {
        const val STATUS_TIMEOUT_MS = 30_000L
        const val TRANSLATE_TIMEOUT_MS = 120_000L
        const val INSTALL_TIMEOUT_MS = 15L * 60 * 1000
    }
}
