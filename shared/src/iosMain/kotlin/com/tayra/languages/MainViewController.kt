package com.tayra.languages

import androidx.compose.ui.window.ComposeUIViewController
import com.tayra.languages.di.initKoin
import org.koin.mp.KoinPlatform

@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController {
    if (KoinPlatform.getKoinOrNull() == null) initKoin()
    App()
}
