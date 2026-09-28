package com.tayra.languages.core.data.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.tayra.languages.core.data.settings.KeychainSecureStore
import com.tayra.languages.core.data.settings.SecureStore
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.domain.service.LocalTranslation
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformDataModule: Module = module {
    single { LocalTranslation(null) }
    single { DatabaseDriverFactory() }
    single { DictionaryPackStorage(get()) }
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single<SecureStore> { KeychainSecureStore() }
}
