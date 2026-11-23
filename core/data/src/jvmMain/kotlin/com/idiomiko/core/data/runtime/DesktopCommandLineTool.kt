package com.idiomiko.core.data.runtime

import co.touchlab.kermit.Logger
import com.idiomiko.core.domain.service.CommandLineStatus
import com.idiomiko.core.domain.service.CommandLineTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Puts the `idiomiko` launcher of the installed app on the PATH.
 *
 * The app bundles it as `idiomiko-cli`, as `idiomiko` would be the app's own launcher to macOS and
 * Windows. On macOS and Linux a two-line script in `~/.local/bin` runs that launcher, and when the
 * login shell does not search that folder yet, a line in its profile adds it. On Windows an
 * `idiomiko.cmd` doing the same goes in a folder of its own, which goes on the user's PATH. Either
 * way only new terminals see the change. A run from the sources has no bundled launcher, so nothing to
 * install; there `./gradlew :cli:run` runs the command.
 */
class DesktopCommandLineTool(
    /** The app's own launcher; jpackage names it in this property. */
    appLauncher: File? = System.getProperty("jpackage.app-path")?.let(::File),
    private val home: File = File(System.getProperty("user.home")),
    private val windows: Boolean = System.getProperty("os.name").lowercase().contains("win"),
    private val mac: Boolean = System.getProperty("os.name").lowercase().contains("mac"),
    private val shell: String = System.getenv("SHELL")?.takeIf { it.isNotBlank() } ?: if (mac) "/bin/zsh" else "/bin/bash",
    /** The PATH a new terminal gets; asked of the login shell, since an app opened from the desktop has a shorter one. */
    private val terminalPath: () -> List<String>? = { loginShellPath(shell) },
) : CommandLineTool {

    private val launcher: File? = appLauncher?.parentFile?.resolve(if (windows) "idiomiko-cli.exe" else "idiomiko-cli")?.takeIf { it.isFile }

    override val available: Boolean get() = launcher != null

    private val binDir get() = File(home, ".local/bin")
    private val script get() = File(binDir, "idiomiko")
    private val windowsBinDir get() = File(System.getenv("LOCALAPPDATA") ?: File(home, "AppData/Local").path, "Idiomiko/bin")
    private val windowsScript get() = File(windowsBinDir, "idiomiko.cmd")

    /** Whether a new terminal finds a `idiomiko`, wherever it came from, and whether it is the one this app put there. */
    override suspend fun status(): CommandLineStatus = withContext(Dispatchers.IO) {
        if (windows) windowsStatus() else unixStatus()
    }

    private fun unixStatus(): CommandLineStatus {
        // When the shell does not say, the script this app wrote is the best guess.
        val path = terminalPath() ?: listOf(binDir.absolutePath)
        val found = path.map { File(it.replaceFirst("~", home.absolutePath), "idiomiko") }.firstOrNull { it.isFile && it.canExecute() }
            ?: return CommandLineStatus(installed = false)
        val ours = found.canonicalFile == script.canonicalFile && script.readText().contains(MARKER)
        return CommandLineStatus(installed = true, location = found.absolutePath, removable = ours)
    }

    private fun windowsStatus(): CommandLineStatus {
        val user = runCatching { windowsUserPath() }.getOrDefault(emptyList())
        val dirs = user + System.getenv("PATH").orEmpty().split(';').filter { it.isNotBlank() }
        val found = dirs.asSequence()
            .flatMap { dir -> listOf("idiomiko.exe", "idiomiko.bat", "idiomiko.cmd").asSequence().map { File(dir.trim(), it) } }
            .firstOrNull { it.isFile }
            ?: return CommandLineStatus(installed = false)
        val ours = found.absoluteFile == windowsScript.absoluteFile && windowsScript.readText().contains(MARKER)
        return CommandLineStatus(installed = true, location = found.absolutePath, removable = ours)
    }

    override suspend fun install(): CommandLineStatus = withContext(Dispatchers.IO) {
        val launcher = launcher ?: return@withContext CommandLineStatus(installed = false, error = "This copy of the app has no idiomiko command")
        try {
            if (windows) installOnWindows(launcher) else installOnUnix(launcher)
        } catch (e: Exception) {
            Logger.w(e) { "Could not install the command-line tool" }
            status().copy(error = e.message ?: e.toString())
        }
    }

    override suspend fun uninstall(): CommandLineStatus = withContext(Dispatchers.IO) {
        try {
            if (windows) {
                val dir = windowsBinDir.absolutePath
                setWindowsUserPath(windowsUserPath().filterNot { it.equals(dir, ignoreCase = true) })
                if (windowsScript.isFile && windowsScript.readText().contains(MARKER)) windowsScript.delete()
            } else if (script.isFile && script.readText().contains(MARKER)) {
                script.delete()
            }
            status()
        } catch (e: Exception) {
            status().copy(error = e.message ?: e.toString())
        }
    }

    private fun installOnUnix(launcher: File): CommandLineStatus {
        binDir.mkdirs()
        script.writeText("#!/bin/sh\n# $MARKER: runs the command-line tool of the app.\nexec \"${launcher.absolutePath}\" \"\$@\"\n")
        script.setExecutable(true, false)
        val path = terminalPath()
        val searched = path?.any { File(it.replaceFirst("~", home.absolutePath)).absoluteFile == binDir.absoluteFile } == true
        val profile = if (searched) null else addToProfile()
        return CommandLineStatus(installed = true, location = script.absolutePath, removable = true, pathChangedIn = profile?.absolutePath)
    }

    /** Adds `~/.local/bin` to the PATH in the profile of the user's shell, once; returns the file it changed. */
    private fun addToProfile(): File? {
        val name = File(shell).name
        val (profile, line) = when {
            name == "fish" -> File(home, ".config/fish/conf.d/idiomiko.fish") to "fish_add_path -g \$HOME/.local/bin  # $MARKER"
            name == "zsh" -> File(home, ".zshrc") to UNIX_PATH_LINE
            name == "bash" -> File(home, if (mac) ".bash_profile" else ".bashrc") to UNIX_PATH_LINE
            else -> File(home, ".profile") to UNIX_PATH_LINE
        }
        if (profile.isFile && profile.readText().contains(MARKER)) return null
        profile.parentFile?.mkdirs()
        val before = if (profile.isFile && profile.length() > 0 && !profile.readText().endsWith("\n")) "\n" else ""
        profile.appendText("$before\n$line\n")
        return profile
    }

    private fun installOnWindows(launcher: File): CommandLineStatus {
        windowsBinDir.mkdirs()
        windowsScript.writeText("@echo off\r\nrem $MARKER: runs the command-line tool of the app.\r\n\"${launcher.absolutePath}\" %*\r\n")
        val dir = windowsBinDir.absolutePath
        val path = windowsUserPath()
        if (path.none { it.equals(dir, ignoreCase = true) }) setWindowsUserPath(path + dir)
        return CommandLineStatus(installed = true, location = windowsScript.absolutePath, removable = true, pathChangedIn = "PATH")
    }

    /** The user's own PATH entries, as stored, with variables such as %USERPROFILE% left as they are. */
    private fun windowsUserPath(): List<String> {
        val out = powershell("(Get-Item -Path 'HKCU:\\Environment').GetValue('Path', '', 'DoNotExpandEnvironmentNames')")
        return out.trim().split(';').map { it.trim() }.filter { it.isNotEmpty() }
    }

    /**
     * Stores the user's PATH as an expandable string, which keeps its variables working, then
     * touches a variable through .NET so Explorer and new terminals hear that the environment changed.
     */
    private fun setWindowsUserPath(entries: List<String>) {
        val value = entries.joinToString(";").replace("'", "''")
        powershell(
            "Set-ItemProperty -Path 'HKCU:\\Environment' -Name Path -Type ExpandString -Value '$value'; " +
                "[Environment]::SetEnvironmentVariable('IDIOMIKO_PATH_REFRESH', '1', 'User'); " +
                "[Environment]::SetEnvironmentVariable('IDIOMIKO_PATH_REFRESH', \$null, 'User')",
        )
    }

    private fun powershell(script: String): String {
        val (code, output) = run(listOf("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script), seconds = 20) ?: error("PowerShell did not answer")
        if (code != 0) error("PowerShell failed: ${output.trim().take(300)}")
        return output
    }

    companion object {
        const val MARKER = "Added by Idiomiko"
        private const val UNIX_PATH_LINE = "export PATH=\"\$HOME/.local/bin:\$PATH\"  # $MARKER"
        private const val PATH_PREFIX = "__IDIOMIKO_PATH__="

        /** The PATH of a new interactive login shell, or null when the shell does not say in time. */
        fun loginShellPath(shell: String): List<String>? {
            val fish = File(shell).name == "fish"
            val print = if (fish) "printf '%s' '$PATH_PREFIX'(string join : \$PATH)" else "printf '%s' \"$PATH_PREFIX\$PATH\""
            val (_, output) = run(listOf(shell, "-l", "-i", "-c", print), seconds = 5) ?: return null
            if (PATH_PREFIX !in output) return null
            return output.substringAfterLast(PATH_PREFIX).lineSequence().first().split(':').filter { it.isNotBlank() }
        }

        /** Runs [command] with no input, its output read as it comes; null when it does not finish in [seconds]. */
        private fun run(command: List<String>, seconds: Long): Pair<Int, String>? = runCatching {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            process.outputStream.close()
            val output = StringBuilder()
            val pump = Thread { output.append(process.inputStream.bufferedReader().readText()) }.apply { isDaemon = true; start() }
            if (!process.waitFor(seconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return@runCatching null
            }
            pump.join(2_000)
            process.exitValue() to output.toString()
        }.getOrNull()
    }
}
