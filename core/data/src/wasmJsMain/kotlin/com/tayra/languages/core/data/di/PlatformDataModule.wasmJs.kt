package com.tayra.languages.core.data.di

import com.russhwolf.settings.Settings
import com.tayra.languages.core.data.settings.BrowserSecureStore
import com.tayra.languages.core.data.settings.SecureStore
import com.russhwolf.settings.StorageSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.domain.service.LocalTranslation
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single { LocalTranslation(null) }
    single { DatabaseDriverFactory() }
    single { DictionaryPackStorage() }
    single<Settings> { StorageSettings() }
    single<SecureStore> { BrowserSecureStore() }
}
