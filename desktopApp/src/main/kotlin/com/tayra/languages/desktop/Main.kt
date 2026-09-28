package com.tayra.languages.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.tayra.languages.App
import com.tayra.languages.di.initKoin
import io.github.vinceglb.filekit.FileKit
import java.awt.Taskbar
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess
import javax.imageio.ImageIO

private val isMacOs = System.getProperty("os.name").lowercase().contains("mac")

/** Set when the JVM was started with the Dock name, by Gradle or by [relaunchWithDockName]. */
private const val DOCK_NAMED_PROPERTY = "tayra.dockNamed"

/** Height of the macOS title bar that the app content extends under. */
private val macTitleBarHeight = 28.dp

fun main() {
    if (relaunchWithDockName()) return
    System.setProperty("apple.awt.application.name", "Tayra Languages")
    FileKit.init(appId = "TayraLanguages")
    initKoin()
    setDockIcon()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Tayra Languages",
            icon = painterResource("window-icon.png"),
            state = WindowState(width = 1200.dp, height = 800.dp),
        ) {
            LaunchedEffect(window) {
                if (isMacOs) {
                    // Let the app's themed top bar show through the native title bar.
                    window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                    window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                    window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
                }
            }
            App(titleBarInset = if (isMacOs) macTitleBarHeight else 0.dp)
        }
    }
}

/**
 * Shows the app icon in the Dock and task bar. Packaged builds get it from the bundle, but a
 * development run would otherwise show the generic Java icon.
 */
private fun setDockIcon() {
    runCatching {
        if (!Taskbar.isTaskbarSupported() || !Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) return
        // macOS expects the rounded plate with transparent margins; other systems use the square.
        val name = if (isMacOs) "dock-icon.png" else "window-icon.png"
        val resource = Thread.currentThread().contextClassLoader.getResource(name) ?: return
        Taskbar.getTaskbar().iconImage = ImageIO.read(resource)
    }
}


/**
 * The Dock labels a bare JVM "java" unless it is started with `-Xdock:name`. Gradle passes the
 * flag, but an IDE run configuration does not, so in that case this process starts the app again
 * with the flag and then waits for it, mirroring its exit code: the IDE keeps a live process to
 * stop, and stopping it also ends the app. Skipped when a debugger is attached.
 */
private fun relaunchWithDockName(): Boolean {
    if (!isMacOs || System.getProperty(DOCK_NAMED_PROPERTY) != null) return false
    val runtime = ManagementFactory.getRuntimeMXBean()
    val args = runtime.inputArguments
    // The launcher consumes -Xdock options, so they never show up here; the property is the marker.
    if (args.any { it.contains("jdwp") }) return false
    val java = ProcessHandle.current().info().command().orElse(null) ?: return false
    val command = listOf(java) + args +
        listOf("-Xdock:name=Tayra Languages", "-D$DOCK_NAMED_PROPERTY=true", "-cp", runtime.classPath, "com.tayra.languages.desktop.MainKt")
    val child = runCatching { ProcessBuilder(command).inheritIO().start() }.getOrElse { return false }
    Runtime.getRuntime().addShutdownHook(Thread { if (child.isAlive) child.destroy() })
    exitProcess(child.waitFor())
}
