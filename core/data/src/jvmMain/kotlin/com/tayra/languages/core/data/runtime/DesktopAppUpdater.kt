package com.tayra.languages.core.data.runtime

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.service.AppUpdate
import com.tayra.languages.core.domain.service.AppUpdater
import com.tayra.languages.core.domain.service.UpdateInstaller
import com.tayra.languages.core.domain.service.Versions
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.system.exitProcess

/** How the running desktop app was installed, which says which installer updates it and how. */
enum class Installation(
    /** The end of the release asset's name that updates this kind of installation. */
    val assetSuffix: String,
) {
    /** An app bundle from the DMG, anywhere its folder can be written to. */
    MAC_APP("-macos-arm64.dmg"),
    /** Installed from the MSI, for this user. */
    WINDOWS_MSI("-windows-x64.msi"),
    /** The Debian package, under /opt. */
    LINUX_DEB("-linux-amd64.deb"),
    /** The RPM package, under /opt. */
    LINUX_RPM("-linux-x86_64.rpm"),
    /** The tarball, unpacked anywhere. */
    LINUX_TAR("-linux-x64.tar.gz"),
}

/**
 * Updates the installed desktop app from its GitHub releases.
 *
 * The release workflow passes the version to the packaged app as `-Dtayra.version` (the installers'
 * own version is 1.0.0 on macOS until the app reaches 1.0), and a run from the sources has none, so
 * it is never offered an update. The releases are read from GitHub's API: the newest release whose
 * tag is `vX.Y.Z` (the data releases, dictionaries, books and courses, have other tags) and that is
 * newer than the running app. Its installer for this installation is downloaded and checked against
 * the SHA-256 GitHub records for it.
 *
 * Installing: a running app cannot replace itself, so on macOS, Windows and from the tarball a
 * script started before the app quits waits for it to end, puts the new version in its place and
 * opens it: on macOS the DMG's app bundle replaces this one (the old one kept until the new one is
 * in place), on Windows `msiexec` upgrades the per-user install (the MSIs share their upgrade code),
 * and the tarball's folder replaces this one. Debian and RPM packages install under /opt as root, so
 * `pkexec` asks for the password while the app still runs (Linux keeps the running files), and
 * the new version is opened once that is done. What a script does goes to its log next to the
 * installer.
 */
class DesktopAppUpdater(
    private val client: HttpClient,
    override val currentVersion: String? = System.getProperty(VERSION_PROPERTY)?.takeIf { Versions.parse(it) != null },
    /** The app's own launcher; jpackage names it in this property. */
    private val appLauncher: File? = System.getProperty("jpackage.app-path")?.let(::File),
    private val releasesUrl: String = System.getProperty(FEED_PROPERTY) ?: RELEASES_URL,
    private val os: String = System.getProperty("os.name").lowercase(),
    private val arch: String = System.getProperty("os.arch").lowercase(),
    private val workDir: File = File(System.getProperty("java.io.tmpdir"), "tayra-update"),
    /** Where the Debian and RPM packages install the app. */
    private val packagesRoot: String = "/opt/",
    /** Whether the installed package is a Debian (true) or RPM (false) one, for an app under [packagesRoot]. */
    private val debianPackage: () -> Boolean? = { linuxPackage() },
    /** Starts a process that outlives the app. */
    private val start: (List<String>, File) -> Unit = { command, log -> ProcessBuilder(command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log)).start() },
    /** Runs a command to its end and gives its exit code. */
    private val run: (List<String>, File) -> Int = { command, log -> ProcessBuilder(command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log)).start().waitFor() },
    private val exit: () -> Unit = { exitProcess(0) },
) : AppUpdater {

    private val json = Json { ignoreUnknownKeys = true }

    /** How this copy was installed, or null when it cannot update itself (a run from the sources, an Intel Mac, a folder it cannot write to). */
    val installation: Installation? by lazy { detectInstallation() }

    /** The folder or bundle the update replaces: the .app on macOS, the app's folder from the tarball. */
    private val installedRoot: File? get() = when (installation) {
        Installation.MAC_APP -> appLauncher?.parentFile?.parentFile?.parentFile
        Installation.LINUX_TAR -> appLauncher?.parentFile?.parentFile
        else -> null
    }

    override suspend fun check(): AppUpdate? = withContext(Dispatchers.IO) {
        val current = currentVersion ?: return@withContext null
        val response = client.get(releasesUrl) {
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        if (!response.status.isSuccess()) throw IllegalStateException("GitHub answered ${response.status.value}")
        newest(json.parseToJsonElement(response.bodyAsText()).jsonArray)?.takeIf { Versions.isNewer(it.version, current) }
    }

    /** The newest app release in GitHub's list of releases, with the installer for this installation. */
    internal fun newest(releases: JsonArray): AppUpdate? {
        val release = releases.map { it.jsonObject }
            .filter { it.bool("draft") != true && it.bool("prerelease") != true }
            .mapNotNull { r -> r.string("tag_name")?.takeIf { TAG.matches(it) }?.let { tag -> tag.removePrefix("v") to r } }
            .maxWithOrNull { a, b -> if (Versions.isNewer(a.first, b.first)) 1 else if (Versions.isNewer(b.first, a.first)) -1 else 0 }
            ?: return null
        val (version, r) = release
        val installer = installation?.let { kind ->
            (r["assets"] as? JsonArray).orEmpty().map { it.jsonObject }.firstOrNull { it.string("name")?.endsWith(kind.assetSuffix) == true }?.let { asset ->
                UpdateInstaller(
                    name = asset.string("name")!!,
                    url = asset.string("browser_download_url") ?: return@let null,
                    sizeBytes = asset["size"]?.jsonPrimitive?.longOrNull ?: 0,
                    sha256 = asset.string("digest")?.takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:"),
                )
            }
        }
        return AppUpdate(version, r.string("body").orEmpty(), r.string("html_url").orEmpty(), installer)
    }

    override suspend fun download(update: AppUpdate, onProgress: (Float?) -> Unit): String = withContext(Dispatchers.IO) {
        val installer = update.installer ?: throw IllegalStateException("No installer for this computer")
        workDir.mkdirs()
        val file = File(workDir, installer.name)
        val partial = File(workDir, installer.name + ".part")
        val digest = MessageDigest.getInstance("SHA-256")
        client.prepareGet(installer.url) {
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = DOWNLOAD_TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }.execute { response ->
            if (!response.status.isSuccess()) throw IllegalStateException("The download answered ${response.status.value}")
            val total = response.contentLength()?.takeIf { it > 0 } ?: installer.sizeBytes.takeIf { it > 0 }
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(BUFFER)
            var read = 0L
            partial.outputStream().use { out ->
                while (true) {
                    val n = channel.readAvailable(buffer, 0, buffer.size)
                    if (n < 0) break
                    if (n == 0) { if (channel.isClosedForRead) break else continue }
                    out.write(buffer, 0, n)
                    digest.update(buffer, 0, n)
                    read += n
                    onProgress(total?.let { (read.toFloat() / it).coerceAtMost(1f) })
                }
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (installer.sha256 != null && !installer.sha256.equals(actual, ignoreCase = true)) {
            partial.delete()
            throw IllegalStateException("The downloaded installer is damaged (its SHA-256 does not match); try again")
        }
        file.delete()
        if (!partial.renameTo(file)) throw IllegalStateException("Could not keep the downloaded installer in $workDir")
        file.absolutePath
    }

    override suspend fun installAndRestart(installer: String) = withContext(Dispatchers.IO) {
        val kind = installation ?: throw IllegalStateException("This copy of the app cannot update itself")
        val file = File(installer)
        val log = File(workDir, "update.log")
        val pid = ProcessHandle.current().pid().toString()
        Logger.i { "Installing ${file.name} as $kind" }
        when (kind) {
            Installation.MAC_APP -> {
                val script = script("install-mac.sh", MAC_SCRIPT)
                start(listOf("/bin/bash", script.absolutePath, pid, file.absolutePath, installedRoot!!.absolutePath, "open"), log)
            }
            Installation.LINUX_TAR -> {
                val script = script("install-tar.sh", TAR_SCRIPT)
                start(listOf("/bin/bash", script.absolutePath, pid, file.absolutePath, installedRoot!!.absolutePath, appLauncher!!.absolutePath), log)
            }
            Installation.WINDOWS_MSI -> {
                val script = script("install-msi.cmd", MSI_SCRIPT)
                start(listOf("cmd.exe", "/c", "start", "\"\"", "/min", script.absolutePath, pid, file.absolutePath, appLauncher!!.absolutePath), log)
            }
            Installation.LINUX_DEB, Installation.LINUX_RPM -> {
                val install = if (kind == Installation.LINUX_DEB) listOf("dpkg", "-i") else listOf("rpm", "-U", "--replacepkgs")
                val code = run(listOf("pkexec") + install + file.absolutePath, log)
                if (code != 0) throw IllegalStateException("The package was not installed (${if (code == 126 || code == 127) "the password was not given" else "exit code $code"})")
                file.delete()
                start(listOf(appLauncher!!.absolutePath), log)
            }
        }
        exit()
    }

    private fun script(name: String, text: String): File = File(workDir.apply { mkdirs() }, name).apply {
        writeText(text)
        setExecutable(true)
    }

    private fun detectInstallation(): Installation? {
        val launcher = appLauncher?.takeIf { it.isFile } ?: return null
        val writable = { dir: File? -> dir != null && Files.isWritable(dir.toPath()) }
        return when {
            os.contains("mac") -> {
                val bundle = launcher.parentFile?.parentFile?.parentFile
                Installation.MAC_APP.takeIf { arch == "aarch64" && bundle?.name?.endsWith(".app") == true && writable(bundle.parentFile) }
            }
            os.contains("win") -> Installation.WINDOWS_MSI
            os.contains("linux") && arch in setOf("amd64", "x86_64") -> {
                val root = launcher.parentFile?.parentFile
                when {
                    root?.absolutePath?.startsWith(packagesRoot) == true -> when (debianPackage()) {
                        true -> Installation.LINUX_DEB
                        false -> Installation.LINUX_RPM
                        null -> null
                    }
                    writable(root?.parentFile) -> Installation.LINUX_TAR
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.bool(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull

    companion object {
        const val VERSION_PROPERTY = "tayra.version"
        /** A releases list to read instead of GitHub's, for trying updates out. */
        const val FEED_PROPERTY = "tayra.updateFeed"
        const val RELEASES_URL = "https://api.github.com/repos/zavanton123/tayra-languages/releases?per_page=30"
        private val TAG = Regex("""v\d+\.\d+\.\d+""")
        private const val TIMEOUT = 30_000L
        private const val DOWNLOAD_TIMEOUT = 30 * 60_000L
        private const val BUFFER = 64 * 1024

        /** Whether the app under /opt came from the Debian package (true), the RPM one (false), or neither. */
        private fun linuxPackage(): Boolean? = when {
            exitCode("dpkg-query", "-W", "tayra-languages") == 0 -> true
            exitCode("rpm", "-q", "tayra-languages") == 0 -> false
            else -> null
        }

        private fun exitCode(vararg command: String): Int = runCatching { ProcessBuilder(*command).redirectErrorStream(true).start().waitFor() }.getOrDefault(-1)

        /**
         * Waits for the app ($1) to quit, then puts the app bundle of the DMG ($2) in place of the
         * installed one ($3), keeping the old one until the new one is there, and opens it ($4
         * "open"; anything else leaves it closed).
         */
        internal val MAC_SCRIPT = """
            #!/bin/bash
            pid="${'$'}1"; dmg="${'$'}2"; app="${'$'}3"; reopen="${'$'}4"
            echo "${'$'}(date): updating ${'$'}app from ${'$'}dmg"
            for i in ${'$'}(seq 1 600); do kill -0 "${'$'}pid" 2>/dev/null || break; sleep 0.2; done
            mount="${'$'}(mktemp -d)"
            if ! hdiutil attach -nobrowse -readonly -noautoopen -mountpoint "${'$'}mount" "${'$'}dmg"; then
              echo "could not open the DMG"; [ "${'$'}reopen" = open ] && open "${'$'}app"; exit 1
            fi
            new="${'$'}(find "${'$'}mount" -maxdepth 1 -name '*.app' | head -n 1)"
            staged="${'$'}app.new"; old="${'$'}app.old"
            rm -rf "${'$'}staged" "${'$'}old"
            if [ -n "${'$'}new" ] && ditto "${'$'}new" "${'$'}staged" && mv "${'$'}app" "${'$'}old" && mv "${'$'}staged" "${'$'}app"; then
              rm -rf "${'$'}old"; rm -f "${'$'}dmg"; echo "updated"
            else
              echo "could not replace the app"; [ -d "${'$'}old" ] && [ ! -d "${'$'}app" ] && mv "${'$'}old" "${'$'}app"; rm -rf "${'$'}staged"
            fi
            hdiutil detach "${'$'}mount" -quiet || hdiutil detach "${'$'}mount" -force -quiet
            rmdir "${'$'}mount" 2>/dev/null
            [ "${'$'}reopen" = open ] && open "${'$'}app"
            exit 0
        """.trimIndent() + "\n"

        /**
         * Waits for the app ($1) to quit, then unpacks the tarball ($2) and puts its app folder in
         * place of the installed one ($3), and starts the launcher ($4).
         */
        internal val TAR_SCRIPT = """
            #!/bin/bash
            pid="${'$'}1"; tarball="${'$'}2"; root="${'$'}3"; launcher="${'$'}4"
            echo "${'$'}(date): updating ${'$'}root from ${'$'}tarball"
            for i in ${'$'}(seq 1 600); do kill -0 "${'$'}pid" 2>/dev/null || break; sleep 0.2; done
            staged="${'$'}root.new"; old="${'$'}root.old"
            rm -rf "${'$'}staged" "${'$'}old"; mkdir -p "${'$'}staged"
            if tar -xzf "${'$'}tarball" -C "${'$'}staged"; then
              new="${'$'}(find "${'$'}staged" -mindepth 1 -maxdepth 2 -type d -name bin | head -n 1)"
              if [ -n "${'$'}new" ] && mv "${'$'}root" "${'$'}old" && mv "${'$'}(dirname "${'$'}new")" "${'$'}root"; then
                rm -rf "${'$'}old" "${'$'}staged"; rm -f "${'$'}tarball"; echo "updated"
              else
                echo "could not replace the app"; [ -d "${'$'}old" ] && [ ! -d "${'$'}root" ] && mv "${'$'}old" "${'$'}root"; rm -rf "${'$'}staged"
              fi
            else
              echo "could not unpack the tarball"; rm -rf "${'$'}staged"
            fi
            nohup "${'$'}launcher" >/dev/null 2>&1 &
            exit 0
        """.trimIndent() + "\n"

        /**
         * Waits for the app (%1) to quit, then upgrades it from the MSI (%2) and starts it again (%3).
         * `ping` waits the second, as `timeout` refuses to run without a console to read.
         */
        internal val MSI_SCRIPT = """
            @echo off
            :wait
            tasklist /FI "PID eq %1" 2>nul | find "%1" >nul
            if not errorlevel 1 (
              ping -n 2 127.0.0.1 >nul
              goto wait
            )
            msiexec /i "%~2" /passive /norestart /l*v "%~2.log"
            if not errorlevel 1 del "%~2"
            start "" "%~3"
        """.trimIndent().replace("\n", "\r\n") + "\r\n"
    }
}
