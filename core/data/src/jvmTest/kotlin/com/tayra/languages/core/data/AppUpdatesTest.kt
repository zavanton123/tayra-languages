package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.runtime.DesktopAppUpdater
import com.tayra.languages.core.data.runtime.Installation
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.service.AppUpdate
import com.tayra.languages.core.domain.service.AppUpdateService
import com.tayra.languages.core.domain.service.UpdateInstaller
import com.tayra.languages.core.domain.service.UpdateState
import com.tayra.languages.core.domain.service.Versions
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** The desktop app's updates: which release is offered, its installer downloaded and checked, and how it is installed. */
class AppUpdatesTest {
    private val dir: File = Files.createTempDirectory("tayra-updates").toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private val installer = ByteArray(300_000) { (it % 251).toByte() }
    private val sha256 = MessageDigest.getInstance("SHA-256").digest(installer).joinToString("") { "%02x".format(it) }

    /** GitHub's list of releases, newest first: data releases, a draft and a pre-release among the app's. */
    private fun releases(digest: String = sha256) = """
        [
          {"tag_name": "freedict-v1", "draft": false, "prerelease": false, "html_url": "https://example/freedict", "body": "", "assets": []},
          {"tag_name": "v0.9.0", "draft": true, "prerelease": false, "html_url": "", "body": "", "assets": []},
          {"tag_name": "v0.8.0-beta", "draft": false, "prerelease": true, "html_url": "", "body": "", "assets": []},
          {"tag_name": "v0.6.0", "draft": false, "prerelease": false, "html_url": "https://example/v0.6.0", "body": "Better reading",
           "assets": [
             {"name": "TayraLanguages-0.6.0-macos-arm64.dmg", "size": ${installer.size}, "digest": "sha256:$digest", "browser_download_url": "https://example/download/TayraLanguages-0.6.0-macos-arm64.dmg"},
             {"name": "TayraLanguages-0.6.0-windows-x64.msi", "size": 10, "digest": "sha256:00", "browser_download_url": "https://example/download/TayraLanguages-0.6.0-windows-x64.msi"}
           ]},
          {"tag_name": "v0.10.0", "draft": false, "prerelease": true, "html_url": "", "body": "", "assets": []},
          {"tag_name": "v0.5.0", "draft": false, "prerelease": false, "html_url": "https://example/v0.5.0", "body": "", "assets": []}
        ]
    """.trimIndent()

    private val requests = mutableListOf<String>()

    private fun client(feed: String = releases()) = HttpClient(MockEngine { request ->
        requests += request.url.toString()
        when {
            request.url.encodedPath.endsWith("/releases") -> respond(feed, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            request.url.encodedPath.endsWith(".dmg") -> respond(installer, HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, installer.size.toString()))
            else -> respond("", HttpStatusCode.NotFound)
        }
    }) { install(HttpTimeout) }

    /** An installed app bundle in a folder that can be written to, as the DMG puts it. */
    private fun macLauncher(): File = File(dir, "Apps/TayraLanguages.app/Contents/MacOS/TayraLanguages").apply { parentFile.mkdirs(); writeText("launcher") }

    private val started = mutableListOf<List<String>>()
    private var exited = false

    private fun updater(
        feed: String = releases(),
        version: String? = "0.5.0",
        launcher: File? = macLauncher(),
        os: String = "mac os x",
        arch: String = "aarch64",
        installExitCode: Int = 0,
    ) = DesktopAppUpdater(
        client(feed), version, launcher, releasesUrl = "https://api.example/repos/x/releases", os = os, arch = arch,
        workDir = File(dir, "work"), debianPackage = { true },
        start = { command, _ -> started += command }, run = { command, _ -> started += command; installExitCode }, exit = { exited = true },
    )

    @Test
    fun theNewestAppReleaseIsOfferedWithTheInstallerForThisComputer() = runTest {
        val update = updater().check()!!
        assertEquals("0.6.0", update.version, "data releases, drafts and pre-releases are passed over")
        assertEquals("Better reading", update.notes)
        assertEquals("https://example/v0.6.0", update.pageUrl)
        assertEquals(UpdateInstaller("TayraLanguages-0.6.0-macos-arm64.dmg", "https://example/download/TayraLanguages-0.6.0-macos-arm64.dmg", installer.size.toLong(), sha256), update.installer)

        assertNull(updater(version = "0.6.0").check(), "the running version is the newest")
        assertNull(updater(version = null).check(), "a run from the sources is never offered one")
        // An Intel Mac has no installer of its own: the release is offered, to download by hand.
        assertNull(updater(arch = "x86_64").check()!!.installer)
        assertEquals("TayraLanguages-0.6.0-windows-x64.msi", updater(launcher = File(dir, "TayraLanguages.exe").apply { writeText("") }, os = "windows 11", arch = "amd64").check()!!.installer!!.name)
    }

    @Test
    fun eachInstallationIsTold() {
        assertEquals(Installation.MAC_APP, updater().installation)
        assertNull(updater(launcher = null).installation, "a run from the sources")
        val opt = File("/opt/tayra-languages/bin/tayra-languages")
        assertNull(updater(launcher = opt, os = "linux").installation, "no launcher there on this computer")
        val tarball = File(dir, "tayra-languages/bin/tayra-languages").apply { parentFile.mkdirs(); writeText("") }
        assertEquals(Installation.LINUX_TAR, updater(launcher = tarball, os = "linux", arch = "amd64").installation)
    }

    @Test
    fun theInstallerIsDownloadedAndCheckedAgainstItsDigest() = runTest {
        val progress = mutableListOf<Float?>()
        val updater = updater()
        val path = updater.download(updater.check()!!) { progress += it }
        assertTrue(File(path).readBytes().contentEquals(installer))
        assertEquals(1f, progress.last())
        assertTrue(progress.zipWithNext().all { (a, b) -> b!! >= a!! })

        val damaged = updater(feed = releases(digest = "ab".repeat(32)))
        val message = assertFailsWith<IllegalStateException> { damaged.download(damaged.check()!!) {} }.message!!
        assertTrue("damaged" in message)
        assertFalse(File(dir, "work").listFiles().orEmpty().any { it.name.endsWith(".part") }, "nothing of a damaged download is kept")
    }

    @Test
    fun installingStartsTheScriptThatReplacesTheAppAndEndsTheApp() = runTest {
        val updater = updater()
        updater.installAndRestart(File(dir, "new.dmg").absolutePath)
        val command = started.single()
        assertEquals("/bin/bash", command[0])
        assertTrue(command[1].endsWith("install-mac.sh") && File(command[1]).readText() == DesktopAppUpdater.MAC_SCRIPT)
        assertEquals(listOf(ProcessHandle.current().pid().toString(), File(dir, "new.dmg").absolutePath, File(dir, "Apps/TayraLanguages.app").absolutePath, "open"), command.drop(2))
        assertTrue(exited)
    }

    @Test
    fun aDebianPackageIsInstalledWithThePasswordBeforeTheAppEnds() = runTest {
        val opt = File(dir, "opt")
        val launcher = File(opt, "tayra-languages/bin/tayra-languages").apply { parentFile.mkdirs(); writeText("") }
        fun debian(exitCode: Int) = DesktopAppUpdater(
            client(), "0.5.0", launcher, "https://api.example/repos/x/releases", "linux", "amd64", File(dir, "work"), packagesRoot = opt.absolutePath + "/",
            debianPackage = { true }, start = { command, _ -> started += command }, run = { command, _ -> started += command; exitCode }, exit = { exited = true },
        )
        assertEquals(Installation.LINUX_DEB, debian(0).installation)

        // The password dialog was closed: nothing is installed and the app goes on.
        val message = assertFailsWith<IllegalStateException> { debian(126).installAndRestart(File(dir, "new.deb").absolutePath) }.message!!
        assertTrue("password" in message)
        assertEquals(listOf("pkexec", "dpkg", "-i", File(dir, "new.deb").absolutePath), started.single())
        assertFalse(exited)

        started.clear()
        debian(0).installAndRestart(File(dir, "new.deb").absolutePath)
        assertEquals(listOf(listOf("pkexec", "dpkg", "-i", File(dir, "new.deb").absolutePath), listOf(launcher.absolutePath)), started)
        assertTrue(exited)
    }

    /** A process that has already ended, for the scripts to wait on. */
    private fun endedProcess(): String = ProcessBuilder("true").start().apply { waitFor() }.pid().toString()

    private fun runScript(script: String, vararg args: String): String {
        val file = File(dir, "script.sh").apply { writeText(script) }
        val process = ProcessBuilder(listOf("/bin/bash", file.absolutePath) + args).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return output
    }

    @Test
    fun theTarballScriptPutsTheNewFolderInPlaceOfTheOld() {
        if (System.getProperty("os.name").lowercase().contains("win")) return
        val root = File(dir, "apps/tayra-languages").apply { File(this, "bin").mkdirs(); File(this, "bin/version").writeText("0.5.0") }
        val unpacked = File(dir, "new/tayra-languages").apply { File(this, "bin").mkdirs(); File(this, "bin/version").writeText("0.6.0") }
        val tarball = File(dir, "TayraLanguages-0.6.0-linux-x64.tar.gz")
        assertEquals(0, ProcessBuilder("tar", "-czf", tarball.absolutePath, "-C", unpacked.parentFile.absolutePath, ".").start().waitFor())

        val output = runScript(DesktopAppUpdater.TAR_SCRIPT, endedProcess(), tarball.absolutePath, root.absolutePath, "/usr/bin/true")
        assertTrue("updated" in output, output)
        assertEquals("0.6.0", File(root, "bin/version").readText())
        assertEquals(listOf("tayra-languages"), root.parentFile.list()!!.toList(), "nothing of the old or the unpacked copy is left")
        assertFalse(tarball.exists())
    }

    @Test
    fun theMacScriptPutsTheDmgsAppInPlaceOfTheOld() {
        if (!System.getProperty("os.name").lowercase().contains("mac")) return
        val app = File(dir, "Apps/TayraLanguages.app").apply { File(this, "Contents").mkdirs(); File(this, "Contents/version").writeText("0.5.0") }
        val image = File(dir, "image/TayraLanguages.app").apply { File(this, "Contents").mkdirs(); File(this, "Contents/version").writeText("0.6.0") }
        val dmg = File(dir, "TayraLanguages-0.6.0-macos-arm64.dmg")
        assertEquals(0, ProcessBuilder("hdiutil", "create", "-quiet", "-srcfolder", image.parentFile.absolutePath, "-volname", "Tayra", "-format", "UDZO", dmg.absolutePath).start().waitFor())

        val output = runScript(DesktopAppUpdater.MAC_SCRIPT, endedProcess(), dmg.absolutePath, app.absolutePath, "stay-closed")
        assertTrue("updated" in output, output)
        assertEquals("0.6.0", File(app, "Contents/version").readText())
        assertEquals(listOf("TayraLanguages.app"), app.parentFile.list()!!.toList(), "nothing of the old or the staged copy is left")
        assertFalse(dmg.exists())

        // A DMG that cannot be opened leaves the installed app as it was.
        val broken = File(dir, "broken.dmg").apply { writeText("not a disk image") }
        assertTrue("could not open the DMG" in runScript(DesktopAppUpdater.MAC_SCRIPT, endedProcess(), broken.absolutePath, app.absolutePath, "stay-closed"))
        assertEquals("0.6.0", File(app, "Contents/version").readText())
    }

    @Test
    fun versionsCompareByTheirNumbers() {
        assertTrue(Versions.isNewer("0.10.0", "0.9.9"))
        assertTrue(Versions.isNewer("v1.0.0", "0.12.3"))
        assertFalse(Versions.isNewer("0.5.0", "0.5.0"))
        assertFalse(Versions.isNewer("0.4.9", "0.5.0"))
        assertFalse(Versions.isNewer("data-v1", "0.5.0"))
    }

    /** An updater whose answers the test sets. */
    private class FakeUpdater(var offer: AppUpdate?) : com.tayra.languages.core.domain.service.AppUpdater {
        override val currentVersion = "0.5.0"
        var checks = 0
        var installed: String? = null
        var failDownload = false
        override suspend fun check(): AppUpdate? { checks++; return offer }
        override suspend fun download(update: AppUpdate, onProgress: (Float?) -> Unit): String {
            onProgress(0.5f)
            if (failDownload) throw IllegalStateException("network down")
            return "/tmp/${update.installer!!.name}"
        }
        override suspend fun installAndRestart(installer: String) { installed = installer }
    }

    private val update = AppUpdate("0.6.0", "notes", "https://example/v0.6.0", UpdateInstaller("TayraLanguages-0.6.0-macos-arm64.dmg", "https://example/x.dmg", 10, null))

    private class Ticking(var now: Instant) : Clock { override fun now() = now }

    @Test
    fun theAppChecksAtMostOnceADayAndNeverOffersASkippedVersionAtStart() = runTest {
        val settings = SettingsRepositoryImpl(MapSettings())
        val clock = Ticking(Instant.fromEpochMilliseconds(1_000_000_000_000))
        val updater = FakeUpdater(update)
        val service = AppUpdateService(updater, settings, clock)

        service.checkOnStart()
        assertEquals(UpdateState.Available(update), service.state.value)
        service.checkOnStart()
        assertEquals(1, updater.checks, "checked a moment ago")

        service.skip(update)
        clock.now = Instant.fromEpochMilliseconds(clock.now.toEpochMilliseconds() + AppUpdateService.CHECK_EVERY.inWholeMilliseconds)
        service.checkOnStart()
        assertEquals(2, updater.checks)
        assertTrue(service.state.value is UpdateState.UpToDate, "the skipped version is not offered at start")
        service.check()
        assertEquals(UpdateState.Available(update), service.state.value, "but it is when asked for")

        settings.update { it.copy(checkForUpdates = false) }
        clock.now = Instant.fromEpochMilliseconds(clock.now.toEpochMilliseconds() + AppUpdateService.CHECK_EVERY.inWholeMilliseconds)
        service.checkOnStart()
        assertEquals(3, updater.checks, "switched off, the app does not look at start")
    }

    @Test
    fun anUpdateIsDownloadedThenInstalledAndAFailureCanBeRetried() = runTest {
        val updater = FakeUpdater(update).apply { failDownload = true }
        val service = AppUpdateService(updater, SettingsRepositoryImpl(MapSettings()))
        service.check()
        service.install()
        assertEquals(UpdateState.Failed("network down", update), service.state.value)
        updater.failDownload = false
        service.install()
        assertEquals("/tmp/TayraLanguages-0.6.0-macos-arm64.dmg", updater.installed)
        assertEquals(UpdateState.Installing(update), service.state.value)
    }
}
