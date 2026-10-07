package com.tayra.languages.core.data.runtime

import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** On macOS and Linux the command goes in ~/.local/bin, and the shell profile gets that folder only when its PATH lacks it. */
class DesktopCommandLineToolTest {
    private val root: File = Files.createTempDirectory("tayra-cli-install").toFile()
    private val home = File(root, "home").apply { mkdirs() }
    private val app = File(root, "Tayra Languages.app/Contents/MacOS").apply { mkdirs() }

    init {
        File(app, "TayraLanguages").writeText("")
        // Stands in for the bundled launcher: prints each argument it gets on a line of its own.
        File(app, "tayra").apply {
            writeText("#!/bin/sh\nfor a in \"\$@\"; do echo \"[\$a]\"; done\n")
            setExecutable(true)
        }
    }

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    private fun tool(path: List<String>?, shell: String = "/bin/zsh", launcher: File? = File(app, "TayraLanguages")) =
        DesktopCommandLineTool(appLauncher = launcher, home = home, windows = false, mac = true, shell = shell, terminalPath = { path })

    @Test
    fun theScriptRunsTheBundledLauncherAndTheProfileGetsThePathOnce() = runBlocking {
        val tool = tool(path = listOf("/usr/bin", "/bin"))
        assertFalse(tool.status().installed)

        val first = tool.install()
        assertTrue(first.installed)
        assertEquals(File(home, ".zshrc").absolutePath, first.pathChangedIn)
        assertTrue(tool.status().installed)
        val output = ProcessBuilder(File(home, ".local/bin/tayra").absolutePath, "books", "add", "--title", "Two words").start().inputStream.bufferedReader().readText()
        assertEquals("[books]\n[add]\n[--title]\n[Two words]\n", output)

        assertNull(tool.install().pathChangedIn, "the profile line is added once")
        assertEquals(1, File(home, ".zshrc").readLines().count { "Tayra Languages" in it })

        val removed = tool.uninstall()
        assertFalse(removed.installed)
        assertFalse(File(home, ".local/bin/tayra").exists())
    }

    @Test
    fun aPathThatHasTheFolderAlreadyLeavesTheProfileAlone() = runBlocking {
        val installed = tool(path = listOf("/usr/bin", "~/.local/bin")).install()
        assertTrue(installed.installed)
        assertNull(installed.pathChangedIn)
        assertFalse(File(home, ".zshrc").exists())
    }

    @Test
    fun bashOnMacGetsItsLoginProfileAndFishItsOwnFile() = runBlocking {
        assertEquals(File(home, ".bash_profile").absolutePath, tool(path = emptyList(), shell = "/bin/bash").install().pathChangedIn)
        val fish = tool(path = null, shell = "/opt/homebrew/bin/fish").install()
        assertEquals(File(home, ".config/fish/conf.d/tayra.fish").absolutePath, fish.pathChangedIn)
        assertTrue(File(fish.pathChangedIn!!).readText().contains("fish_add_path"))
    }

    @Test
    fun aRunFromTheSourcesHasNoCommandToInstall() = runBlocking {
        val tool = tool(path = emptyList(), launcher = null)
        assertFalse(tool.bundled)
        assertFalse(tool.install().installed)
        assertFalse(File(home, ".local/bin/tayra").exists())
    }

    @Test
    fun theLoginShellReportsItsPath() {
        val path = DesktopCommandLineTool.loginShellPath("/bin/sh")
        assertTrue(path != null && path.isNotEmpty(), "got $path")
    }
}
