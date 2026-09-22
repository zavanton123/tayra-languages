package com.tayra.languages.di

import com.tayra.languages.bootstrap.AppBootstrapViewModel
import com.tayra.languages.core.data.di.dataModule
import com.tayra.languages.feature.books.booksModule
import com.tayra.languages.feature.languages.languagesModule
import com.tayra.languages.feature.reading.readingModule
import com.tayra.languages.feature.settings.settingsModule
import com.tayra.languages.feature.stats.statsModule
import com.tayra.languages.feature.terms.termsModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val appModule = module {
    viewModel { AppBootstrapViewModel(get(), get()) }
}

/** Feature modules wired into the app. */
private val featureModules: List<Module> = listOf(booksModule, languagesModule, termsModule, readingModule, settingsModule, statsModule)

/**
 * Starts dependency injection. [platformModules] supply platform objects such as the
 * Android application context.
 */
fun initKoin(platformModules: List<Module> = emptyList()) {
    startKoin {
        modules(platformModules + dataModule + appModule + featureModules)
    }
}
