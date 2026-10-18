package com.tayra.languages.core.data

import com.tayra.languages.core.data.runtime.DesktopAppUpdater
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Installs a real update the way the installed app does, for .github/workflows/update-check.yml:
 * this test plays the app of version TAYRA_UPDATE_FROM whose launcher is TAYRA_UPDATE_LAUNCHER, finds
 * the update in the releases list at TAYRA_UPDATE_FEED, downloads and checks it, and starts its
 * installation; the install script goes on once this test's process has ended, and the workflow
 * checks what it left. `sudo` stands in for `pkexec`, which needs someone to type the password.
 * Skipped unless TAYRA_UPDATE_LAUNCHER is set.
 */
class UpdateInstallCheck {
    @Test
    fun theInstalledAppIsUpdated() = runBlocking {
        val launcher = System.getenv("TAYRA_UPDATE_LAUNCHER") ?: return@runBlocking
        val work = File(System.getenv("TAYRA_UPDATE_WORK"))
        val updater = DesktopAppUpdater(
            HttpClient { install(HttpTimeout) },
            currentVersion = System.getenv("TAYRA_UPDATE_FROM"),
            appLauncher = File(launcher),
            releasesUrl = System.getenv("TAYRA_UPDATE_FEED"),
            workDir = work,
            run = { command, log ->
                ProcessBuilder(command.map { if (it == "pkexec") "sudo" else it }).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log)).start().waitFor()
            },
            exit = { println("The app would quit now") },
        )
        val installation = assertNotNull(updater.installation, "this installation cannot update itself")
        System.getenv("TAYRA_UPDATE_EXPECT")?.let { assertEquals(it, installation.name) }
        val update = assertNotNull(updater.check(), "no update offered")
        println("Installing ${update.version} from ${update.installer?.name} as $installation")
        updater.installAndRestart(updater.download(update) {})
    }
}
