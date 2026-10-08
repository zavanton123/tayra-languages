package com.tayra.languages.core.data.translation

import kotlin.coroutines.cancellation.CancellationException
import co.touchlab.kermit.Logger
import com.tayra.languages.core.data.runtime.ManagedPython
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

/**
 * Argos Translate on the desktop: a Python worker (`argos_worker.py`, shipped in resources) is
 * started on first use and kept alive so the models load once. Requests are sent one at a time
 * as JSON lines.
 *
 * Nothing has to be installed on the machine: [prepare] downloads a standalone CPython build
 * into the app folder, installs argostranslate into it and fetches the models for the pair in
 * use, reporting each step through [progress]. A Python of the user's own can be set instead.
 */
class ArgosSentenceTranslator(
    private val settings: SettingsRepository,
    private val managed: ManagedPython = ManagedPython(),
) : LocalSentenceTranslator {

    override val displayName: String = "Argos Translate"
    override val description: String = "Argos Translate translates on this computer with no network. The app keeps its own Python and the language models in its data folder."
    override val packagesDescription: String = "One package per direction. Reading a language needs its package into the native language; when there is none, Argos goes through English, so install both halves."
    override val hasRuntimeSetup: Boolean = true

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
        return try {
            request(TRANSLATE_TIMEOUT_MS, "cmd" to "translate", "from" to source, "to" to target, "q" to text)["t"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotEmpty() }
                .also { _lastError.value = null }
        } catch (e: CancellationException) {
            // The caller stopped waiting (a hover moved on, a page changed); that is not a failure.
            throw e
        } catch (e: Exception) {
            Logger.w { "Argos translation failed: ${e.message}" }
            _lastError.value = LocalTranslationProblem.Failed(e.message ?: "unknown error")
            null
        }
    }

    override suspend fun prepare(fromCode: String, toCode: String, fromName: String, toName: String) {
        val pair = "$fromCode-$toCode"
        if (pair in readyPairs) return
        val problem: LocalTranslationProblem? = try {
            ensureRuntime().await()
            val installed = request(STATUS_TIMEOUT_MS, "cmd" to "status")["pairs"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
            if (pair in installed) null else missingModels(fromCode, toCode, fromName, toName)
        } catch (e: CancellationException) {
            throw e
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
            if (toEnglish == null || fromEnglish == null) return LocalTranslationProblem.NoModel(fromName, toName, displayName)
            listOf(toEnglish, fromEnglish)
        }
        val missing = needed.filter { !it.installed }
        val title = missing.joinToString(" and ") { it.title } + if (missing.size == 1) " model" else " models"
        return LocalTranslationProblem.ModelMissing(fromCode, toCode, title)
    }

    override suspend fun canTranslate(fromCode: String, toCode: String): Boolean {
        val pair = "$fromCode-$toCode"
        if (pair in readyPairs) return true
        val installed = runCatching { request(STATUS_TIMEOUT_MS, "cmd" to "status")["pairs"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty() }.getOrDefault(emptyList())
        return (pair in installed).also { if (it) readyPairs += pair }
    }

    override suspend fun installModels(fromCode: String, toCode: String) {
        try {
            _progress.value = "Downloading the models for $fromCode \u2192 $toCode\u2026"
            request(INSTALL_TIMEOUT_MS, "cmd" to "install", "from" to fromCode, "to" to toCode)
            readyPairs += "$fromCode-$toCode"
            _lastError.value = null
        } catch (e: CancellationException) {
            throw e
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
        return "Argos Translate is ready in ${managed.python.parentFile.parentFile.absolutePath}."
    }

    /** One shared setup at a time; callers wait for it and a failure is thrown to each of them. */
    private fun ensureRuntime(): Deferred<Unit> = synchronized(this) {
        runtimeSetup?.takeIf { it.isActive }?.let { return it }
        setupScope.async { installRuntime() }.also { runtimeSetup = it }
    }

    private suspend fun installRuntime() {
        if (runCatching { request(STATUS_TIMEOUT_MS, "cmd" to "status") }.isSuccess && importOk()) return
        managed.ensure { _progress.value = it }
        _progress.value = "Installing Argos Translate (about a gigabyte, a few minutes)\u2026"
        managed.pip(listOf("argostranslate"), "Installing Argos Translate") { _progress.value = it }
        settings.update { it.copy(argosPython = managed.python.absolutePath) }
        lock.withLock { stop() }
        if (!importOk()) error("argostranslate did not import after installation")
    }

    private suspend fun importOk(): Boolean = runCatching { packages() }.isSuccess

    override suspend fun status(): String = try {
        val reply = request(STATUS_TIMEOUT_MS, "cmd" to "status")
        val pairs = reply["pairs"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        val version = reply["version"]?.jsonPrimitive?.content ?: "?"
        val python = reply["python"]?.jsonPrimitive?.content?.let { " · Python $it" }.orEmpty()
        val count = if (pairs.isEmpty()) "no language packages yet" else "${pairs.size} language pair${if (pairs.size == 1) "" else "s"}"
        "Argos Translate $version$python · $count"
    } catch (e: Exception) {
        "Argos Translate is not available: ${e.message}"
    }

    override suspend fun runtimeDownloadSize(): Long? = if (managed.isInstalled && importOk()) null else PIP_DOWNLOAD_SIZE + managed.downloadSize

    override fun knownPackages(): List<LocalPackage> =
        ArgosModels.all.map { LocalPackage(it.fromCode, it.toCode, it.fromName, it.toName, installed = false, sizeBytes = it.sizeBytes) }

    override suspend fun packages(): List<LocalPackage> =
        request(INDEX_TIMEOUT_MS, "cmd" to "packages")["packages"]?.jsonArray?.map { element ->
            val o = element.jsonObject
            LocalPackage(
                fromCode = o.getValue("from").jsonPrimitive.content,
                toCode = o.getValue("to").jsonPrimitive.content,
                fromName = o["fromName"]?.jsonPrimitive?.content ?: o.getValue("from").jsonPrimitive.content,
                toName = o["toName"]?.jsonPrimitive?.content ?: o.getValue("to").jsonPrimitive.content,
                installed = o["installed"]?.jsonPrimitive?.booleanOrNull ?: false,
                sizeBytes = o["size"]?.jsonPrimitive?.longOrNull?.takeIf { it > 0 }
                    ?: ArgosModels.find(o.getValue("from").jsonPrimitive.content, o.getValue("to").jsonPrimitive.content)?.sizeBytes ?: 0L,
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
        val python = settings.current.argosPython.trim().ifEmpty { managed.python.takeIf { it.exists() }?.absolutePath ?: "python3" }
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

    private companion object {
        /** What pip fetches for argostranslate into a fresh Python, PyTorch most of it. */
        const val PIP_DOWNLOAD_SIZE = 186_000_000L
        const val STATUS_TIMEOUT_MS = 30_000L
        const val INDEX_TIMEOUT_MS = 120_000L
        const val TRANSLATE_TIMEOUT_MS = 120_000L
        const val INSTALL_TIMEOUT_MS = 15L * 60 * 1000
    }
}
