package com.tayra.languages.core.data.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single { DatabaseDriverFactory() }
    single { DictionaryPackStorage() }
    single<Settings> { StorageSettings() }
}
