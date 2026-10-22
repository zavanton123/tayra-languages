package com.tayra.languages.di

import com.tayra.languages.feature.courses.coursesModule
import com.tayra.languages.feature.frequency.frequencyModule
import com.tayra.languages.feature.home.homeModule
import com.tayra.languages.feature.news.newsModule
import com.tayra.languages.feature.playlists.playlistsModule
import com.tayra.languages.feature.flashcards.flashcardsModule
import com.tayra.languages.bootstrap.AppBootstrapViewModel
import com.tayra.languages.core.data.di.dataModule
import com.tayra.languages.feature.books.booksModule
import com.tayra.languages.feature.languages.languagesModule
import com.tayra.languages.feature.reading.readingModule
import com.tayra.languages.feature.settings.settingsModule
import com.tayra.languages.feature.stats.statsModule
import com.tayra.languages.feature.terms.termsModule
import com.tayra.languages.core.domain.morphology.ChineseScriptTables
import com.tayra.languages.core.ui.chinese.ResourceChineseScriptTables
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val appModule = module {
    single<ChineseScriptTables> { ResourceChineseScriptTables() }
    viewModel { AppBootstrapViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), getOrNull(), getOrNull(), getOrNull()) }
}

/** Feature modules wired into the app. */
private val featureModules: List<Module> = listOf(booksModule, coursesModule, frequencyModule, homeModule, newsModule, playlistsModule, languagesModule, termsModule, flashcardsModule, readingModule, settingsModule, statsModule)

/**
 * Starts dependency injection. [platformModules] supply platform objects such as the
 * Android application context.
 */
fun initKoin(platformModules: List<Module> = emptyList()) {
    startKoin {
        modules(platformModules + dataModule + appModule + featureModules)
    }
}
