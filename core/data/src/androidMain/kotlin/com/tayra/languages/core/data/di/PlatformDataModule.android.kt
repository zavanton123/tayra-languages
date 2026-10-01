package com.tayra.languages.core.data.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.tayra.languages.core.data.settings.AndroidSecureStore
import com.tayra.languages.core.data.speech.AndroidSherpaRuntime
import com.tayra.languages.core.data.speech.SherpaKokoroEngine
import com.tayra.languages.core.data.speech.SherpaPiperEngine
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.data.translation.MlKitSentenceTranslator
import com.tayra.languages.core.data.settings.SecureStore
import com.russhwolf.settings.SharedPreferencesSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.domain.service.LocalTranslation
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File
import com.tayra.languages.core.domain.service.SpeechAudioCache
import com.tayra.languages.core.data.speech.FileSpeechAudioCache

actual val platformDataModule: Module = module {
    single { LocalTranslation(MlKitSentenceTranslator(get())) }
    single { AndroidSherpaRuntime(get<Context>()) }
    single { LocalSpeech(listOf(SherpaPiperEngine(get<AndroidSherpaRuntime>()), SherpaKokoroEngine(get<AndroidSherpaRuntime>()))) }
    single { DatabaseDriverFactory(get<Context>()) }
    single<SpeechAudioCache> { FileSpeechAudioCache(File(get<Context>().cacheDir, "speech-cache")) }
    single { DictionaryPackStorage(get<Context>(), get()) }
    single<Settings> { SharedPreferencesSettings(get<Context>().getSharedPreferences("tayra_settings", Context.MODE_PRIVATE)) }
    single<SecureStore> { AndroidSecureStore(get<Context>()) }
}
