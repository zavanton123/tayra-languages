package com.tayra.languages.core.data.di

import com.russhwolf.settings.Settings
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.network.MyMemoryTranslationProvider
import com.tayra.languages.core.data.network.TatoebaExamplesProvider
import com.tayra.languages.core.data.network.TranslationSuggestionProvider
import com.tayra.languages.core.data.network.WebPageImporter
import com.tayra.languages.core.data.network.WiktionaryTranslationProvider
import com.tayra.languages.core.data.network.createHttpClient
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.DatabaseMaintenanceImpl
import com.tayra.languages.core.data.dictionary.DictionaryDatabaseProvider
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.repository.DictionaryRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.repository.DatabaseMaintenance
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.repository.WordsReadRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.DemoDataService
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.LanguageService
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.StatsService
import com.tayra.languages.core.domain.service.TermImportService
import com.tayra.languages.core.domain.service.TermPopupBuilder
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.settings.SettingsRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/** Platform-specific bindings: the database driver factories and [Settings]. */
expect val platformDataModule: Module

val dataModule: Module = module {
    includes(platformDataModule)

    single { DatabaseProvider(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get<Settings>()) }
    single<LanguageRepository> { LanguageRepositoryImpl(get()) }
    single<BookRepository> { BookRepositoryImpl(get()) }
    single<TermRepository> { TermRepositoryImpl(get()) }
    single<WordsReadRepository> { WordsReadRepositoryImpl(get()) }
    single<DatabaseMaintenance> { DatabaseMaintenanceImpl(get()) }
    single { DictionaryDownloader(get()) }
    single<DictionaryPackStore> { get<DictionaryPackStorage>() }
    single { DictionaryDatabaseProvider(get()) }
    single<DictionaryRepository> { DictionaryRepositoryImpl(get()) }
    single { DictionaryService(get(), get()) }
    single<OfflineDictionary> { get<DictionaryService>() }

    single { createHttpClient() }
    single { WebPageImporter(get()) }
    single<ExampleSentencesProvider> { TatoebaExamplesProvider(get()) }
    single<TermTranslationProvider> {
        TranslationSuggestionProvider(WiktionaryTranslationProvider(get()), MyMemoryTranslationProvider(get(), get()), get())
    }

    single { TermService(get(), get()) }
    single { ReadingService(get(), get(), get(), get(), get()) }
    single { TermPopupBuilder(get(), get(), get(), get()) }
    single { BookService(get(), get()) }
    single { BookStatsService(get(), get(), get(), get()) }
    single { LanguageService(get(), get(), get()) }
    single { DemoDataService(get(), get(), get(), get(), get()) }
    single { StatsService(get()) }
    single { TermImportService(get(), get(), get()) }
}
