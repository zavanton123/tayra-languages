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
import javax.imageio.ImageIO

private val isMacOs = System.getProperty("os.name").lowercase().contains("mac")

/** Height of the macOS title bar that the app content extends under. */
private val macTitleBarHeight = 28.dp

fun main() {
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
        val resource = Thread.currentThread().contextClassLoader.getResource("window-icon.png") ?: return
        Taskbar.getTaskbar().iconImage = ImageIO.read(resource)
    }
}
