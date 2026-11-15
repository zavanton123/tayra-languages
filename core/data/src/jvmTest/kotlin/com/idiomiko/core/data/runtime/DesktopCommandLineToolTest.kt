package com.idiomiko.core.data.runtime

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
    private val root: File = Files.createTempDirectory("idiomiko-cli-install").toFile()
    private val home = File(root, "home").apply { mkdirs() }
    private val app = File(root, "Idiomiko.app/Contents/MacOS").apply { mkdirs() }

    init {
        File(app, "Idiomiko").writeText("")
        // Stands in for the bundled launcher: prints each argument it gets on a line of its own.
        File(app, "idiomiko").apply {
            writeText("#!/bin/sh\nfor a in \"\$@\"; do echo \"[\$a]\"; done\n")
            setExecutable(true)
        }
    }

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    /** The PATH a new terminal would get: [path], plus ~/.local/bin once a profile line added it. */
    private fun tool(path: List<String>?, shell: String = "/bin/zsh", launcher: File? = File(app, "Idiomiko"), source: File? = null) =
        DesktopCommandLineTool(
            appLauncher = launcher, sourceLauncher = source, javaHome = "/opt/jdk", home = home, windows = false, mac = true, shell = shell,
            terminalPath = { path?.let { if (profileAdded()) it + "~/.local/bin" else it } },
        )

    private fun profileAdded() = listOf(".zshrc", ".bash_profile", ".bashrc", ".profile", ".config/fish/conf.d/idiomiko.fish")
        .any { File(home, it).let { file -> file.isFile && DesktopCommandLineTool.MARKER in file.readText() } }

    @Test
    fun theScriptRunsTheBundledLauncherAndTheProfileGetsThePathOnce() = runBlocking {
        val tool = tool(path = listOf("/usr/bin", "/bin"))
        assertFalse(tool.status().installed)

        val first = tool.install()
        assertTrue(first.installed)
        assertEquals(File(home, ".zshrc").absolutePath, first.pathChangedIn)
        val status = tool.status()
        assertTrue(status.installed && status.removable)
        assertEquals(File(home, ".local/bin/idiomiko").absolutePath, status.location)
        val output = ProcessBuilder(File(home, ".local/bin/idiomiko").absolutePath, "books", "add", "--title", "Two words").start().inputStream.bufferedReader().readText()
        assertEquals("[books]\n[add]\n[--title]\n[Two words]\n", output)

        assertNull(tool.install().pathChangedIn, "the profile line is added once")
        assertEquals(1, File(home, ".zshrc").readLines().count { "Idiomiko" in it })

        val removed = tool.uninstall()
        assertFalse(removed.installed)
        assertFalse(File(home, ".local/bin/idiomiko").exists())
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
        assertEquals(File(home, ".config/fish/conf.d/idiomiko.fish").absolutePath, fish.pathChangedIn)
        assertTrue(File(fish.pathChangedIn!!).readText().contains("fish_add_path"))
    }

    @Test
    fun aRunFromTheSourcesHasNoCommandToInstall() = runBlocking {
        val tool = tool(path = emptyList(), launcher = null)
        assertFalse(tool.available)
        assertFalse(tool.install().installed)
        assertFalse(File(home, ".local/bin/idiomiko").exists())
    }

    @Test
    fun aIdiomikoFromElsewhereIsShownButNotRemoved() = runBlocking {
        val other = File(root, "usr/local/bin").apply { mkdirs() }
        File(other, "idiomiko").apply { writeText("#!/bin/sh\n"); setExecutable(true) }
        val status = tool(path = listOf(other.absolutePath)).status()
        assertTrue(status.installed)
        assertEquals(File(other, "idiomiko").absolutePath, status.location)
        assertFalse(status.removable)
    }

    @Test
    fun aRunFromTheSourcesInstallsTheBuiltCommandWithItsJava() = runBlocking {
        val built = File(root, "cli/build/install/idiomiko/bin/idiomiko").apply { parentFile.mkdirs(); writeText("#!/bin/sh\n"); setExecutable(true) }
        val tool = tool(path = listOf("/usr/bin"), launcher = null, source = built)
        assertTrue(tool.available)
        assertTrue(tool.install().installed)
        val script = File(home, ".local/bin/idiomiko").readText()
        assertTrue("export JAVA_HOME=\"/opt/jdk\"" in script && built.absolutePath in script, script)
    }

    @Test
    fun theLoginShellReportsItsPath() {
        val path = DesktopCommandLineTool.loginShellPath("/bin/sh")
        assertTrue(path != null && path.isNotEmpty(), "got $path")
    }
}
