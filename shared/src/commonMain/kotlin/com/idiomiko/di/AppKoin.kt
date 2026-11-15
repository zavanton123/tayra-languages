package com.idiomiko.di

import com.idiomiko.feature.courses.coursesModule
import com.idiomiko.feature.frequency.frequencyModule
import com.idiomiko.feature.home.homeModule
import com.idiomiko.feature.news.newsModule
import com.idiomiko.feature.videos.videosModule
import com.idiomiko.feature.playlists.playlistsModule
import com.idiomiko.feature.flashcards.flashcardsModule
import com.idiomiko.bootstrap.AppBootstrapViewModel
import com.idiomiko.core.data.di.dataModule
import com.idiomiko.feature.books.booksModule
import com.idiomiko.feature.languages.languagesModule
import com.idiomiko.feature.reading.readingModule
import com.idiomiko.feature.settings.settingsModule
import com.idiomiko.feature.stats.statsModule
import com.idiomiko.feature.terms.termsModule
import com.idiomiko.core.domain.morphology.ChineseScriptTables
import com.idiomiko.core.ui.chinese.ResourceChineseScriptTables
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val appModule = module {
    single<ChineseScriptTables> { ResourceChineseScriptTables() }
    viewModel { AppBootstrapViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), getOrNull(), getOrNull(), getOrNull(), getOrNull()) }
}

/** Feature modules wired into the app. */
private val featureModules: List<Module> = listOf(booksModule, coursesModule, frequencyModule, homeModule, newsModule, playlistsModule, languagesModule, termsModule, flashcardsModule, readingModule, settingsModule, statsModule, videosModule)

/**
 * Starts dependency injection. [platformModules] supply platform objects such as the
 * Android application context.
 */
fun initKoin(platformModules: List<Module> = emptyList()) {
    startKoin {
        modules(platformModules + dataModule + appModule + featureModules)
        // Koin on the web does not make the singles marked to be made at the start by itself.
        createEagerInstances()
    }
}
