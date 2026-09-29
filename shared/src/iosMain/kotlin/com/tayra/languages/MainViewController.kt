package com.tayra.languages

import androidx.compose.ui.window.ComposeUIViewController
import com.tayra.languages.core.data.speech.SherpaKokoroEngine
import com.tayra.languages.core.data.speech.SherpaPiperEngine
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.speech.IosSherpaRuntime
import com.tayra.languages.speech.OnDeviceSpeechBridge
import com.tayra.languages.di.initKoin
import com.tayra.languages.translation.BridgedSentenceTranslator
import com.tayra.languages.translation.OnDeviceTranslatorBridge
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/**
 * [translator] is the Swift ML Kit bridge and [speech] the Swift sherpa-onnx bridge; either may be
 * null to run without that on-device engine.
 */
@Suppress("unused", "FunctionName")
fun MainViewController(translator: OnDeviceTranslatorBridge? = null, speech: OnDeviceSpeechBridge? = null) = ComposeUIViewController {
    if (KoinPlatform.getKoinOrNull() == null) {
        initKoin(
            listOf(
                module {
                    single { LocalTranslation(translator?.let { BridgedSentenceTranslator(it, get()) }) }
                    single {
                        val runtime = speech?.let { IosSherpaRuntime(it) }
                        LocalSpeech(if (runtime == null) emptyList() else listOf(SherpaPiperEngine(runtime), SherpaKokoroEngine(runtime)))
                    }
                },
            ),
        )
    }
    App()
}
