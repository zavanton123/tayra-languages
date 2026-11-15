package com.idiomiko

import androidx.compose.ui.window.ComposeUIViewController
import com.idiomiko.core.data.speech.SherpaKokoroEngine
import com.idiomiko.core.data.speech.SherpaPiperEngine
import com.idiomiko.core.data.speech.SherpaVideoTranscriber
import com.idiomiko.core.domain.media.VideoTranscriber
import com.idiomiko.core.domain.service.LocalSpeech
import com.idiomiko.core.data.account.GoogleOAuth
import com.idiomiko.core.data.network.createAccountHttpClient
import com.idiomiko.core.domain.account.IdentitySignIn
import com.idiomiko.core.domain.account.StorePurchases
import com.idiomiko.core.domain.service.LocalTranslation
import com.idiomiko.speech.IosSherpaRuntime
import com.idiomiko.speech.IosSherpaTranscription
import com.idiomiko.speech.OnDeviceSpeechBridge
import com.idiomiko.store.BridgedStorePurchases
import com.idiomiko.store.StoreKitBridge
import com.idiomiko.di.initKoin
import com.idiomiko.identity.IdentityBridge
import com.idiomiko.identity.IosIdentitySignIn
import com.idiomiko.translation.BridgedSentenceTranslator
import com.idiomiko.translation.OnDeviceTranslatorBridge
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/**
 * [translator] is the Swift ML Kit bridge, [speech] the Swift sherpa-onnx bridge, [store] the Swift
 * StoreKit bridge and [identity] the Swift AuthenticationServices bridge; any may be null to run without it.
 */
@Suppress("unused", "FunctionName")
fun MainViewController(
    translator: OnDeviceTranslatorBridge? = null,
    speech: OnDeviceSpeechBridge? = null,
    store: StoreKitBridge? = null,
    identity: IdentityBridge? = null,
) = ComposeUIViewController {
    if (KoinPlatform.getKoinOrNull() == null) {
        initKoin(
            listOf(
                module {
                    single { LocalTranslation(translator?.let { BridgedSentenceTranslator(it, get()) }) }
                    single {
                        val runtime = speech?.let { IosSherpaRuntime(it) }
                        LocalSpeech(if (runtime == null) emptyList() else listOf(SherpaPiperEngine(runtime), SherpaKokoroEngine(runtime)))
                    }
                    speech?.let { bridge -> single<VideoTranscriber> { SherpaVideoTranscriber(IosSherpaRuntime(bridge), IosSherpaTranscription(bridge)) } }
                    store?.let { bridge -> single<StorePurchases> { BridgedStorePurchases(bridge) } }
                    identity?.let { bridge -> single<IdentitySignIn> { IosIdentitySignIn(bridge, GoogleOAuth(createAccountHttpClient())) } }
                },
            ),
        )
    }
    App()
}
