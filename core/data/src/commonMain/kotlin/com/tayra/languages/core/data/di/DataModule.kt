package com.tayra.languages.core.data.di

import com.russhwolf.settings.Settings
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.network.MyMemoryTranslationProvider
import com.tayra.languages.core.domain.service.effectiveEngine
import com.tayra.languages.core.domain.service.AlibabaTranslation
import com.tayra.languages.core.domain.service.AzureTranslation
import com.tayra.languages.core.domain.service.BaiduTranslation
import com.tayra.languages.core.domain.service.DeeplTranslation
import com.tayra.languages.core.domain.service.QwenTranslation
import com.tayra.languages.core.domain.service.GoogleTranslation
import com.tayra.languages.core.data.network.AlibabaTranslationProvider
import com.tayra.languages.core.data.network.AzureTranslationProvider
import com.tayra.languages.core.data.network.BaiduTranslationProvider
import com.tayra.languages.core.data.network.DeeplTranslationProvider
import com.tayra.languages.core.data.network.QwenTranslationProvider
import com.tayra.languages.core.data.network.GoogleTranslationProvider
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
import com.tayra.languages.core.data.repository.SentenceTranslationCacheImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.domain.repository.SentenceTranslationCache
import com.tayra.languages.core.data.settings.SecureStore
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
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.domain.service.CachedSentenceTranslator
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.RoutingSentenceTranslator
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.TranslationLanguageKeeper
import com.tayra.languages.core.domain.settings.SettingsRepository
import org.koin.core.module.Module
import org.koin.dsl.module
import com.tayra.languages.core.domain.service.SentenceAudio

/** Platform-specific bindings: the database driver factories and [Settings]. */
expect val platformDataModule: Module

val dataModule: Module = module {
    includes(platformDataModule)

    single { DatabaseProvider(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get<Settings>(), get<SecureStore>()) }
    single<LanguageRepository> { LanguageRepositoryImpl(get()) }
    single<BookRepository> { BookRepositoryImpl(get()) }
    single<TermRepository> {
        val settings = get<SettingsRepository>()
        TermRepositoryImpl(get(), translationLanguage = { settings.current.nativeLanguage.ifBlank { "en" } })
    }
    single<WordsReadRepository> { WordsReadRepositoryImpl(get()) }
    single<DatabaseMaintenance> { DatabaseMaintenanceImpl(get()) }
    single { DictionaryDownloader(get()) }
    single<DictionaryPackStore> { get<DictionaryPackStorage>() }
    single { DictionaryDatabaseProvider(get()) }
    single<DictionaryRepository> { DictionaryRepositoryImpl(get()) }
    single { DictionaryService(get(), get()) }
    single { WordTranslationService(get(), get<DictionaryService>(), get(), get()) }
    single<OfflineDictionary> { get<DictionaryService>() }

    single { createHttpClient() }
    single { WebPageImporter(get()) }
    single<ExampleSentencesProvider> { TatoebaExamplesProvider(get()) }
    single { MyMemoryTranslationProvider(get(), get()) }
    single { GoogleTranslationProvider(get(), get()) }
    single<GoogleTranslation> { get<GoogleTranslationProvider>() }
    single { AzureTranslationProvider(get(), get()) }
    single<AzureTranslation> { get<AzureTranslationProvider>() }
    single { AlibabaTranslationProvider(get(), get()) }
    single<AlibabaTranslation> { get<AlibabaTranslationProvider>() }
    single { BaiduTranslationProvider(get(), get()) }
    single<BaiduTranslation> { get<BaiduTranslationProvider>() }
    single { DeeplTranslationProvider(get(), get()) }
    single<DeeplTranslation> { get<DeeplTranslationProvider>() }
    single { QwenTranslationProvider(get(), get()) }
    single<QwenTranslation> { get<QwenTranslationProvider>() }
    single<SentenceTranslationCache> { SentenceTranslationCacheImpl(get()) }
    single<SentenceTranslator> {
        val settings = get<SettingsRepository>()
        val local = get<LocalTranslation>().translator
        val engine = { settings.current.effectiveEngine(local != null) }
        val routed = RoutingSentenceTranslator(get<MyMemoryTranslationProvider>(), get<GoogleTranslationProvider>(), get<AzureTranslationProvider>(), get<AlibabaTranslationProvider>(), get<BaiduTranslationProvider>(), get<DeeplTranslationProvider>(), get<QwenTranslationProvider>(), local, engine)
        // Stored translations carry the engine in their key so switching engines never mixes results.
        CachedSentenceTranslator(routed, get(), targetLanguage = {
            val native = settings.current.nativeLanguage
            when (engine()) {
                TranslationEngine.ARGOS -> "argos:$native"
                TranslationEngine.GOOGLE -> "google:$native"
                TranslationEngine.AZURE -> "azure:$native"
                TranslationEngine.ALIBABA -> "alibaba:$native"
                TranslationEngine.BAIDU -> "baidu:$native"
                TranslationEngine.DEEPL -> "deepl:$native"
                TranslationEngine.QWEN -> "qwen:$native"
                TranslationEngine.MYMEMORY -> native
            }
        })
    }
    single<TermTranslationProvider> {
        TranslationSuggestionProvider(WiktionaryTranslationProvider(get()), get<MyMemoryTranslationProvider>(), get<GoogleTranslationProvider>(), get<AzureTranslationProvider>(), get<AlibabaTranslationProvider>(), get<BaiduTranslationProvider>(), get<DeeplTranslationProvider>(), get<QwenTranslationProvider>(), get(), get())
    }

    single { TermService(get(), get()) }
    single { SentenceAudio(get(), get(), get()) }
    single { TranslationLanguageKeeper(get(), get(), get<DictionaryService>(), get(), get()) }
    single { ReadingService(get(), get(), get(), get(), get()) }
    single { TermPopupBuilder(get(), get(), get()) }
    single { BookService(get(), get()) }
    single { BookStatsService(get(), get(), get(), get()) }
    single { LanguageService(get(), get(), get()) }
    single { DemoDataService(get(), get(), get(), get(), get()) }
    single { StatsService(get()) }
    single { TermImportService(get(), get(), get()) }
}
