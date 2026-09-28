package com.tayra.languages.core.data.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.translation.ArgosSentenceTranslator
import com.tayra.languages.core.domain.service.LocalTranslation
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences

actual val platformDataModule: Module = module {
    single { LocalTranslation(ArgosSentenceTranslator(get())) }
    single { DatabaseDriverFactory() }
    single { DictionaryPackStorage(get()) }
    single<Settings> { PreferencesSettings(Preferences.userRoot().node("com/tayra/languages")) }
}
