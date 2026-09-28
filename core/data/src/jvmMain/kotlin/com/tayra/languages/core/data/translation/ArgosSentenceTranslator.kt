package com.tayra.languages.core.data.translation

import co.touchlab.kermit.Logger
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.LocalPackage
import com.tayra.languages.core.domain.service.LocalSentenceTranslator
import com.tayra.languages.core.domain.service.LocalTranslationProblem
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit

/**
 * Argos Translate on the desktop: a Python worker (`argos_worker.py`, shipped in resources) is
 * started on first use and kept alive so the models load once. Requests are sent one at a time
 * as JSON lines.
 *
 * Nothing has to be installed on the machine: [prepare] downloads a standalone CPython build
 * into the app folder, installs argostranslate into it and fetches the models for the pair in
 * use, reporting each step through [progress]. A Python of the user's own can be set instead.
 */
class ArgosSentenceTranslator(private val settings: SettingsRepository) : LocalSentenceTranslator {

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var startedWith: String? = null
    private var nextId = 0L

    private val _lastError = MutableStateFlow<LocalTranslationProblem?>(null)
    override val lastError: StateFlow<LocalTranslationProblem?> = _lastError
    private val _progress = MutableStateFlow<String?>(null)
    override val progress: StateFlow<String?> = _progress

    // Setup outlives the screen that asked for it, so a page change never leaves a half-extracted runtime.
    private val setupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runtimeSetup: Deferred<Unit>? = null
    private val readyPairs = mutableSetOf<String>()

    override suspend fun translate(text: String, language: Language): String? {
        val source = LanguageCodes.codeFor(language.name) ?: return null
        val target = nativeCode()
        if (source == target) return null
        return runCatching {
            request(TRANSLATE_TIMEOUT_MS, "cmd" to "translate", "from" to source, "to" to target, "q" to text)["t"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotEmpty() }
        }.onSuccess { _lastError.value = null }
            .onFailure { Logger.w { "Argos translation failed: ${it.message}" }; _lastError.value = LocalTranslationProblem.Failed(it.message ?: "unknown error") }
            .getOrNull()
    }

    override suspend fun prepare(fromCode: String, toCode: String, fromName: String, toName: String) {
        val pair = "$fromCode-$toCode"
        if (pair in readyPairs) return
        val problem: LocalTranslationProblem? = try {
            ensureRuntime().await()
            val installed = request(STATUS_TIMEOUT_MS, "cmd" to "status")["pairs"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
            if (pair in installed) null else missingModels(fromCode, toCode, fromName, toName)
        } catch (e: Exception) {
            LocalTranslationProblem.Failed(e.message ?: "unknown error")
        } finally {
            _progress.value = null
        }
        _lastError.value = problem
        if (problem != null) error(problem.message)
        readyPairs += pair
    }

    /** Which models the pair needs and lacks: the direct one, or both halves of a detour through English. */
    private suspend fun missingModels(fromCode: String, toCode: String, fromName: String, toName: String): LocalTranslationProblem {
        val catalog = packages()
        val direct = catalog.firstOrNull { it.fromCode == fromCode && it.toCode == toCode }
        val needed = if (direct != null) listOf(direct) else {
            val toEnglish = catalog.firstOrNull { it.fromCode == fromCode && it.toCode == "en" }
            val fromEnglish = catalog.firstOrNull { it.fromCode == "en" && it.toCode == toCode }
            if (toEnglish == null || fromEnglish == null) return LocalTranslationProblem.NoModel(fromName, toName)
            listOf(toEnglish, fromEnglish)
        }
        val missing = needed.filter { !it.installed }
        val title = missing.joinToString(" and ") { it.title } + if (missing.size == 1) " model" else " models"
        return LocalTranslationProblem.ModelMissing(fromCode, toCode, title)
    }

    override suspend fun installModels(fromCode: String, toCode: String) {
        try {
            _progress.value = "Downloading the models for $fromCode \u2192 $toCode\u2026"
            request(INSTALL_TIMEOUT_MS, "cmd" to "install", "from" to fromCode, "to" to toCode)
            readyPairs += "$fromCode-$toCode"
            _lastError.value = null
        } catch (e: Exception) {
            _lastError.value = LocalTranslationProblem.Failed(e.message ?: "unknown error")
            throw e
        } finally {
            _progress.value = null
        }
    }

    override suspend fun setUp(): String {
        try {
            ensureRuntime().await()
        } finally {
            _progress.value = null
        }
        return "Argos Translate is ready in ${managedDir().absolutePath}."
    }

    /** One shared setup at a time; callers wait for it and a failure is thrown to each of them. */
    private fun ensureRuntime(): Deferred<Unit> = synchronized(this) {
        runtimeSetup?.takeIf { it.isActive }?.let { return it }
        setupScope.async { installRuntime() }.also { runtimeSetup = it }
    }

    private suspend fun installRuntime() {
        if (runCatching { request(STATUS_TIMEOUT_MS, "cmd" to "status") }.isSuccess && importOk()) return
        val python = managedPython()
        if (!python.exists()) {
            val dir = managedDir().apply { deleteRecursively(); mkdirs() }
            val archive = File(dir, "python.tar.gz")
            download(pythonUrl(), archive) { done, total ->
                _progress.value = "Downloading Python (${done / 1_000_000} of ${total / 1_000_000} MB)…"
            }
            _progress.value = "Unpacking Python…"
            runCommand(listOf("tar", "-xzf", archive.absolutePath, "-C", dir.absolutePath), UNPACK_TIMEOUT_MS)
            archive.delete()
            if (!python.exists()) error("the Python download did not contain ${python.absolutePath}")
        }
        _progress.value = "Installing Argos Translate (about a gigabyte, a few minutes)…"
        runCommand(listOf(python.absolutePath, "-m", "pip", "install", "--disable-pip-version-check", "--no-input", "argostranslate"), PIP_TIMEOUT_MS) { line ->
            when {
                line.startsWith("Collecting ") -> _progress.value = "Installing Argos Translate: fetching ${line.removePrefix("Collecting ").substringBefore(' ')}…"
                line.startsWith("Installing collected") -> _progress.value = "Installing Argos Translate: unpacking…"
            }
        }
        settings.update { it.copy(argosPython = python.absolutePath) }
        lock.withLock { stop() }
        if (!importOk()) error("argostranslate did not import after installation")
    }

    private suspend fun importOk(): Boolean = runCatching { packages() }.isSuccess

    override suspend fun status(): String = try {
        val reply = request(STATUS_TIMEOUT_MS, "cmd" to "status")
        val pairs = reply["pairs"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        val version = reply["version"]?.jsonPrimitive?.content ?: "?"
        if (pairs.isEmpty()) "Argos Translate $version is installed but has no language packages yet."
        else "Argos Translate $version with ${pairs.size} language pair${if (pairs.size == 1) "" else "s"}: ${pairs.joinToString(", ")}"
    } catch (e: Exception) {
        "Argos Translate is not available: ${e.message}"
    }

    override suspend fun packages(): List<LocalPackage> =
        request(INDEX_TIMEOUT_MS, "cmd" to "packages")["packages"]?.jsonArray?.map { element ->
            val o = element.jsonObject
            LocalPackage(
                fromCode = o.getValue("from").jsonPrimitive.content,
                toCode = o.getValue("to").jsonPrimitive.content,
                fromName = o["fromName"]?.jsonPrimitive?.content ?: o.getValue("from").jsonPrimitive.content,
                toName = o["toName"]?.jsonPrimitive?.content ?: o.getValue("to").jsonPrimitive.content,
                installed = o["installed"]?.jsonPrimitive?.booleanOrNull ?: false,
                sizeBytes = o["size"]?.jsonPrimitive?.longOrNull ?: 0L,
            )
        }.orEmpty().sortedBy { it.title }

    override suspend fun installPackage(fromCode: String, toCode: String) {
        request(INSTALL_TIMEOUT_MS, "cmd" to "install", "from" to fromCode, "to" to toCode, "direct" to "true")
    }

    override suspend fun removePackage(fromCode: String, toCode: String) {
        request(STATUS_TIMEOUT_MS, "cmd" to "remove", "from" to fromCode, "to" to toCode)
        readyPairs.clear()
    }

    private fun nativeCode() = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }

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
        val python = settings.current.argosPython.trim().ifEmpty { managedPython().takeIf { it.exists() }?.absolutePath ?: "python3" }
        val current = process
        if (current != null && current.isAlive && startedWith == python) return writer!! to reader!!
        stop()
        val script = extractScript()
        val started = try {
            ProcessBuilder(python, "-u", script.absolutePath).redirectErrorStream(false).start()
        } catch (e: Exception) {
            error("cannot run '$python' (${e.message})")
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

    /** Runs a command to completion, failing with its last output lines when it exits with an error or overruns [timeoutMs]. */
    private fun runCommand(command: List<String>, timeoutMs: Long, onLine: (String) -> Unit = {}) {
        val process = try {
            ProcessBuilder(command).redirectErrorStream(true).start()
        } catch (e: Exception) {
            error("cannot run '${command.first()}' (${e.message})")
        }
        val output = ArrayDeque<String>()
        val pump = Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    Logger.d { "argos setup: $line" }
                    if (line.isNotBlank()) { output.addLast(line); if (output.size > 5) output.removeFirst() }
                    onLine(line)
                }
            }
        }
        pump.isDaemon = true; pump.start()
        if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) { process.destroyForcibly(); error("'${command.take(2).joinToString(" ")}' took too long") }
        pump.join(5_000)
        if (process.exitValue() != 0) error("'${command.take(3).joinToString(" ")}' failed: ${output.joinToString(" ")}")
    }

    private fun download(url: String, target: File, onProgress: (Long, Long) -> Unit) {
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
        val response = client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() != 200) error("downloading Python failed with HTTP ${response.statusCode()}")
        val total = response.headers().firstValueAsLong("Content-Length").orElse(-1)
        var done = 0L
        response.body().use { input ->
            target.outputStream().use { out ->
                val buffer = ByteArray(1 shl 16)
                while (true) {
                    val n = input.read(buffer); if (n < 0) break
                    out.write(buffer, 0, n); done += n
                    onProgress(done, if (total > 0) total else done)
                }
            }
        }
    }

    private fun managedDir(): File = File(DatabaseDriverFactory.dataDirectory(), "argos-python")

    /** The interpreter inside the standalone build: `python/bin/python3`, or `python/python.exe` on Windows. */
    private fun managedPython(): File =
        if (isWindows()) File(managedDir(), "python/python.exe") else File(managedDir(), "python/bin/python3")

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("win")

    private fun pythonUrl(): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        val cpu = if (arch == "aarch64" || arch == "arm64") "aarch64" else "x86_64"
        val triple = when {
            os.contains("mac") -> "$cpu-apple-darwin"
            os.contains("win") -> "$cpu-pc-windows-msvc"
            else -> "$cpu-unknown-linux-gnu"
        }
        return "https://github.com/astral-sh/python-build-standalone/releases/download/$PYTHON_BUILD/cpython-$PYTHON_VERSION+$PYTHON_BUILD-$triple-install_only.tar.gz"
    }

    private companion object {
        // A relocatable CPython from astral-sh/python-build-standalone; 3.12 has wheels for every Argos dependency.
        const val PYTHON_VERSION = "3.12.14"
        const val PYTHON_BUILD = "20260924"
        const val STATUS_TIMEOUT_MS = 30_000L
        const val INDEX_TIMEOUT_MS = 120_000L
        const val TRANSLATE_TIMEOUT_MS = 120_000L
        const val INSTALL_TIMEOUT_MS = 15L * 60 * 1000
        const val UNPACK_TIMEOUT_MS = 5L * 60 * 1000
        const val PIP_TIMEOUT_MS = 30L * 60 * 1000
    }
}
