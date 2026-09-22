package com.tayra.languages

import androidx.compose.ui.window.ComposeUIViewController
import com.tayra.languages.di.initKoin
import org.koin.core.context.GlobalContext

@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController {
    if (GlobalContext.getOrNull() == null) initKoin()
    App()
}
