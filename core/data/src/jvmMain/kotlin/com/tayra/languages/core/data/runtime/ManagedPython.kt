package com.tayra.languages.core.data.runtime

import co.touchlab.kermit.Logger
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit

/**
 * The app's own Python: a standalone CPython build downloaded into the data folder, so nothing
 * has to be installed on the machine. Argos Translate and the speech engines install their
 * packages into it. All calls block and belong on an IO dispatcher.
 */
class ManagedPython(private val dir: File = File(DatabaseDriverFactory.dataDirectory(), "argos-python")) {

    /** The interpreter inside the standalone build: `python/bin/python3`, or `python/python.exe` on Windows. */
    val python: File get() = if (isWindows()) File(dir, "python/python.exe") else File(dir, "python/bin/python3")

    val isInstalled: Boolean get() = python.exists()

    /** Downloads and unpacks the interpreter when it is missing. */
    fun ensure(onProgress: (String) -> Unit) {
        if (isInstalled) return
        dir.deleteRecursively(); dir.mkdirs()
        val archive = File(dir, "python.tar.gz")
        download(pythonUrl(), archive) { done, total -> onProgress("Downloading Python (${done / 1_000_000} of ${total / 1_000_000} MB)\u2026") }
        onProgress("Unpacking Python\u2026")
        run(listOf("tar", "-xzf", archive.absolutePath, "-C", dir.absolutePath), UNPACK_TIMEOUT_MS)
        archive.delete()
        if (!isInstalled) error("the Python download did not contain ${python.absolutePath}")
    }

    /** Installs [packages] with pip, reporting each package it fetches as "[label]: fetching x". */
    fun pip(packages: List<String>, label: String, onProgress: (String) -> Unit) {
        run(listOf(python.absolutePath, "-m", "pip", "install", "--disable-pip-version-check", "--no-input") + packages, PIP_TIMEOUT_MS) { line ->
            when {
                line.startsWith("Collecting ") -> onProgress("$label: fetching ${line.removePrefix("Collecting ").substringBefore(' ')}\u2026")
                line.startsWith("Installing collected") -> onProgress("$label: unpacking\u2026")
            }
        }
    }

    /** Runs a command to completion, failing with its last output lines when it exits with an error or overruns [timeoutMs]. */
    fun run(command: List<String>, timeoutMs: Long, onLine: (String) -> Unit = {}) {
        val process = try {
            ProcessBuilder(command).redirectErrorStream(true).start()
        } catch (e: Exception) {
            error("cannot run '${command.first()}' (${e.message})")
        }
        val output = ArrayDeque<String>()
        val pump = Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    Logger.d { "python setup: $line" }
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

    /** Streams [url] into [target], reporting bytes done and total. */
    fun download(url: String, target: File, onProgress: (Long, Long) -> Unit) {
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
        val response = client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() != 200) error("download failed with HTTP ${response.statusCode()}")
        val total = response.headers().firstValueAsLong("Content-Length").orElse(-1)
        var done = 0L
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, target.name + ".part")
        response.body().use { input ->
            partial.outputStream().use { out ->
                val buffer = ByteArray(1 shl 16)
                while (true) {
                    val n = input.read(buffer); if (n < 0) break
                    out.write(buffer, 0, n); done += n
                    onProgress(done, if (total > 0) total else done)
                }
            }
        }
        target.delete()
        if (!partial.renameTo(target)) error("could not save ${target.name}")
    }

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
        // A relocatable CPython from astral-sh/python-build-standalone; 3.12 has wheels for every dependency.
        const val PYTHON_VERSION = "3.12.14"
        const val PYTHON_BUILD = "20260924"
        const val UNPACK_TIMEOUT_MS = 5L * 60 * 1000
        const val PIP_TIMEOUT_MS = 30L * 60 * 1000
    }
}
