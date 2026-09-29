package com.tayra.languages

import androidx.compose.ui.window.ComposeUIViewController
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.di.initKoin
import com.tayra.languages.translation.BridgedSentenceTranslator
import com.tayra.languages.translation.OnDeviceTranslatorBridge
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/** [translator] is the Swift ML Kit bridge, or null to run without on-device translation. */
@Suppress("unused", "FunctionName")
fun MainViewController(translator: OnDeviceTranslatorBridge? = null) = ComposeUIViewController {
    if (KoinPlatform.getKoinOrNull() == null) {
        initKoin(listOf(module { single { LocalTranslation(translator?.let { BridgedSentenceTranslator(it, get()) }) } }))
    }
    App()
}
