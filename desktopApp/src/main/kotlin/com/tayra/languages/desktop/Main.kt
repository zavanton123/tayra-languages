package com.tayra.languages.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.tayra.languages.App
import com.tayra.languages.di.initKoin
import io.github.vinceglb.filekit.FileKit

fun main() {
    FileKit.init(appId = "TayraLanguages")
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Tayra Languages",
            state = WindowState(width = 1200.dp, height = 800.dp),
        ) {
            App()
        }
    }
}
