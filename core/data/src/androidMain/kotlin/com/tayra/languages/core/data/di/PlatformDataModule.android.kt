package com.tayra.languages.core.data.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single { DatabaseDriverFactory(get<Context>()) }
    single<Settings> { SharedPreferencesSettings(get<Context>().getSharedPreferences("tayra_settings", Context.MODE_PRIVATE)) }
}
