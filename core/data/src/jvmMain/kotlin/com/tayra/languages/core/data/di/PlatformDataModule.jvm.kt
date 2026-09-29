package com.tayra.languages.core.data.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.data.settings.SecureStore
import com.tayra.languages.core.data.speech.KokoroSpeechEngine
import com.tayra.languages.core.data.speech.PiperSpeechEngine
import com.tayra.languages.core.data.speech.TtsWorker
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.data.settings.desktopSecureStore
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.translation.ArgosSentenceTranslator
import com.tayra.languages.core.domain.service.LocalTranslation
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences

actual val platformDataModule: Module = module {
    single { ManagedPython() }
    single { LocalTranslation(ArgosSentenceTranslator(get(), get())) }
    single { TtsWorker(get()) }
    single { LocalSpeech(listOf(PiperSpeechEngine(get(), get()), KokoroSpeechEngine(get(), get()))) }
    single { DatabaseDriverFactory() }
    single { DictionaryPackStorage(get()) }
    single<Settings> { PreferencesSettings(Preferences.userRoot().node("com/tayra/languages")) }
    single<SecureStore> { desktopSecureStore() }
}
