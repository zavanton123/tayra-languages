package com.tayra.languages.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.tayra.languages.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Tayra Languages",
        state = WindowState(width = 1200.dp, height = 800.dp),
    ) {
        App()
    }
}
