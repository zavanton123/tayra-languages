package com.idiomiko.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.idiomiko.App
import com.idiomiko.core.data.bridge.DesktopBrowserBridge
import com.idiomiko.core.data.runtime.AppInstanceLock
import com.idiomiko.core.domain.media.VideoOpenRequests
import com.idiomiko.di.initKoin
import io.github.vinceglb.filekit.FileKit
import java.awt.Desktop
import java.awt.Frame
import java.awt.Taskbar
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess
import javax.imageio.ImageIO
import org.koin.core.context.GlobalContext

private val isMacOs = System.getProperty("os.name").lowercase().contains("mac")

/** Set when the JVM was started with the Dock name, by Gradle or by [relaunchWithDockName]. */
private const val DOCK_NAMED_PROPERTY = "idiomiko.dockNamed"

/** Height of the macOS title bar that the app content extends under. */
private val macTitleBarHeight = 28.dp

fun main() {
    if (relaunchWithDockName()) return
    // Tells the `idiomiko` command that the app is open, for as long as it is.
    AppInstanceLock.acquire()
    System.setProperty("apple.awt.application.name", "Idiomiko")
    FileKit.init(appId = "Idiomiko")
    initKoin()
    // Lets the Idiomiko browser extension look words up while the app is open.
    GlobalContext.get().get<DesktopBrowserBridge>().start()
    setDockIcon()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Idiomiko",
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
            // A video opened from the browser extension comes to the front with the app.
            LaunchedEffect(window) {
                GlobalContext.get().get<VideoOpenRequests>().videos.collect {
                    if (window.extendedState and Frame.ICONIFIED != 0) window.extendedState = window.extendedState and Frame.ICONIFIED.inv()
                    window.toFront()
                    runCatching { if (Desktop.isDesktopSupported()) Desktop.getDesktop().requestForeground(true) }
                    window.requestFocus()
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
    // Debuggers and the Compose Hot Reload agent must stay attached to this very process.
    if (args.any { it.contains("jdwp") || it.contains("hot-reload") || it.contains("compose.reload") }) return false
    val java = ProcessHandle.current().info().command().orElse(null) ?: return false
    val command = listOf(java) + args +
        listOf("-Xdock:name=Idiomiko", "-D$DOCK_NAMED_PROPERTY=true", "-cp", runtime.classPath, "com.idiomiko.desktop.MainKt")
    val child = runCatching { ProcessBuilder(command).inheritIO().start() }.getOrElse { return false }
    Runtime.getRuntime().addShutdownHook(Thread { if (child.isAlive) child.destroy() })
    exitProcess(child.waitFor())
}
