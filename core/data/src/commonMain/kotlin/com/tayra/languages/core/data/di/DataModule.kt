package com.tayra.languages.core.data.di

import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.courses.CoursePackStore
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.InstalledCoursePacks
import com.tayra.languages.core.domain.courses.SampleCourseSource
import com.tayra.languages.core.domain.courses.CourseRepository
import com.tayra.languages.core.data.coursepack.CoursePackRepository
import com.tayra.languages.core.data.bookpack.BookPackRepository
import com.tayra.languages.core.domain.books.BookPackService
import com.tayra.languages.core.domain.books.BookPackStore
import com.tayra.languages.core.domain.books.discover.BookDiscoveryService
import com.tayra.languages.core.domain.news.NewsFeedReader
import com.tayra.languages.core.domain.news.NewsService
import com.tayra.languages.core.data.news.FeedNewsReader
import com.tayra.languages.core.data.discover.GdlCatalog
import com.tayra.languages.core.data.discover.GutenbergCatalog
import com.tayra.languages.core.data.discover.WikisourceCatalog
import com.tayra.languages.core.data.repository.CourseRepositoryImpl
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.VocabularyLevelRepositoryImpl
import com.tayra.languages.core.domain.frequency.VocabularyLevelRepository
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.repository.FlashcardRepository
import com.russhwolf.settings.Settings
import com.tayra.languages.core.data.backup.BackupRepositoryImpl
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.domain.backup.BackupRepository
import com.tayra.languages.core.data.network.MyMemoryTranslationProvider
import com.tayra.languages.core.domain.service.effectiveEngine
import com.tayra.languages.core.domain.service.GoogleTranslation
import com.tayra.languages.core.data.network.GoogleTranslationProvider
import com.tayra.languages.core.data.network.TatoebaExamplesProvider
import com.tayra.languages.core.data.network.TranslationSuggestionProvider
import com.tayra.languages.core.data.network.WiktionaryTranslationProvider
import com.tayra.languages.core.data.network.createHttpClient
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.DatabaseMaintenanceImpl
import com.tayra.languages.core.data.dictionary.DictionaryDatabaseProvider
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.yomitan.YomitanConverter
import com.tayra.languages.core.data.dictionary.SettingsImportedDictionaryStore
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.ImportedDictionaries
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
import com.tayra.languages.core.domain.service.LexiconService
import com.tayra.languages.core.domain.service.ChineseScriptService
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.WordReadings
import com.tayra.languages.core.domain.service.StatsService
import com.tayra.languages.core.domain.stats.LearningStatsRepository
import com.tayra.languages.core.data.repository.LearningStatsRepositoryImpl
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
import com.tayra.languages.core.domain.service.LearningTranslations
import com.tayra.languages.core.domain.service.LearningLanguageService
import com.tayra.languages.core.domain.export.AnkiExportService
import com.tayra.languages.core.domain.export.AnkiPackager
import com.tayra.languages.core.data.export.AnkiPackagerImpl
import com.tayra.languages.core.domain.settings.SettingsRepository
import org.koin.core.module.Module
import org.koin.dsl.module
import com.tayra.languages.core.data.network.KtorRecordingFetcher
import com.tayra.languages.core.domain.service.ExampleRecordings
import com.tayra.languages.core.domain.service.WordRecordings
import com.tayra.languages.core.data.network.CommonsRecordingSource
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.LanguageSetupService
import com.tayra.languages.core.domain.service.ClearDataService

/** Platform-specific bindings: the database driver factories and [Settings]. */
expect val platformDataModule: Module

val dataModule: Module = module {
    includes(platformDataModule)

    single { DatabaseProvider(get()) }
    single { SettingsRepositoryImpl(get<Settings>(), get<SecureStore>()) }
    single<SettingsRepository> { get<SettingsRepositoryImpl>() }
    single<BackupRepository> { BackupRepositoryImpl(get(), get(), get()) }
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
    single { ImportedDictionaries(SettingsImportedDictionaryStore(get<Settings>())) }
    single { DictionaryDatabaseProvider(get()) { id -> DictionaryPacks.find(id) ?: get<ImportedDictionaries>().pack(id) } }
    single<DictionaryRepository> { DictionaryRepositoryImpl(get()) }
    single { DictionaryService(get(), get(), get(), YomitanConverter()) }
    single { WordTranslationService(get(), get<DictionaryService>(), get(), get()) }
    single<OfflineDictionary> { get<DictionaryService>() }

    single { createHttpClient() }
    single<ExampleSentencesProvider> { TatoebaExamplesProvider(get()) }
    single { MyMemoryTranslationProvider(get(), get()) }
    single { GoogleTranslationProvider(get(), get()) }
    single<GoogleTranslation> { get<GoogleTranslationProvider>() }
    single<SentenceTranslationCache> { SentenceTranslationCacheImpl(get()) }
    single<SentenceTranslator> {
        val settings = get<SettingsRepository>()
        val local = get<LocalTranslation>().translator
        val engine = { settings.current.effectiveEngine(local != null) }
        val routed = RoutingSentenceTranslator(get<MyMemoryTranslationProvider>(), get<GoogleTranslationProvider>(), local, engine)
        // Stored translations carry the engine in their key so switching engines never mixes results.
        CachedSentenceTranslator(routed, get(), targetLanguage = {
            val native = settings.current.nativeLanguage
            when (engine()) {
                TranslationEngine.ARGOS -> "argos:$native"
                TranslationEngine.GOOGLE -> "google:$native"
                TranslationEngine.MYMEMORY -> native
            }
        })
    }
    single<TermTranslationProvider> {
        TranslationSuggestionProvider(WiktionaryTranslationProvider(get()), get<MyMemoryTranslationProvider>(), get<GoogleTranslationProvider>(), get(), get())
    }

    single { LearningTranslations(get(), get(), get()) }
    single<AnkiPackager> { AnkiPackagerImpl() }
    single { AnkiExportService(get(), get(), get(), get(), get(), get(), get()) }
    single { TermService(get(), get(), translationsWanted = get<LearningTranslations>()::request, chineseTables = getOrNull()) }
    single { ExampleRecordings(KtorRecordingFetcher(get()), get()) }
    single { WordRecordings(CommonsRecordingSource(get())) }
    single { SentenceAudio(get(), get(), get(), get()) }
    single { TranslationLanguageKeeper(get(), get(), get<DictionaryService>(), get(), get()) }
    single { LexiconService(get(), get(), getOrNull(), getOrNull()) }
    single { ChineseScriptService(get(), getOrNull(), get(), get()) }
    single { WordReadings(get(), get()) }
    single { ReadingService(get(), get(), get(), get(), get(), get<LexiconService>()) }
    single { TermPopupBuilder(get(), get(), get()) }
    single { BookService(get(), get()) }
    single { BookStatsService(get(), get(), get(), get()) }
    single { LanguageService(get(), get(), get()) }
    single { LearningLanguageService(get(), get(), get()) }
    single { DemoDataService(get(), get(), get(), get(), get()) }
    single<LearningStatsRepository> { LearningStatsRepositoryImpl(get()) }
    single { StatsService(get(), get()) }
    single<CourseRepository> { CourseRepositoryImpl(get()) }
    single<CoursePackStore> { CoursePackRepository(get()) }
    single<SampleCourseSource> { InstalledCoursePacks(get()) }
    single { CourseService(get(), get(), get(), get(), get()) }
    single { CoursePackService(get(), get()) }
    single<BookPackStore> { BookPackRepository(get()) }
    single { BookPackService(get(), get(), get(), get()) }
    single { BookDiscoveryService(listOf(GutenbergCatalog(get()), WikisourceCatalog(get()), GdlCatalog(get())), get(), get(), get()) }
    single<NewsFeedReader> { FeedNewsReader(get()) }
    single { NewsService(get(), get(), get(), get()) }
    single { LanguageSetupService(get(), get(), get(), get(), get(), get()) }
    single { ClearDataService(get(), get(), get(), get(), get(), get(), get()) }
    single<FlashcardRepository> { FlashcardRepositoryImpl(get()) }
    single<VocabularyLevelRepository> { VocabularyLevelRepositoryImpl(get()) }
    single { FlashcardService(get(), get(), get()) }
}
