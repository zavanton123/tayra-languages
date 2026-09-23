package com.tayra.languages.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.tayra.languages.App
import com.tayra.languages.di.initKoin
import io.github.vinceglb.filekit.FileKit

private val isMacOs = System.getProperty("os.name").lowercase().contains("mac")

/** Height of the macOS title bar that the app content extends under. */
private val macTitleBarHeight = 28.dp

fun main() {
    FileKit.init(appId = "TayraLanguages")
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Tayra Languages",
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
