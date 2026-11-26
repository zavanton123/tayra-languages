package com.idiomiko.core.data.di

import com.idiomiko.core.data.repository.ListeningLogImpl
import com.idiomiko.core.data.repository.OwnFontRepositoryImpl
import com.idiomiko.core.domain.fonts.OwnFontRepository
import com.idiomiko.core.domain.stats.ListeningLog
import com.idiomiko.core.domain.ocr.NoTextRecognizer
import com.idiomiko.core.domain.ocr.TextRecognizer
import com.idiomiko.core.domain.courses.LessonSummaries
import org.koin.dsl.binds
import com.idiomiko.core.domain.account.AccountRepository
import com.idiomiko.core.domain.account.SyncRunner
import com.idiomiko.core.domain.account.LibrarySync
import com.idiomiko.core.domain.account.PackChoices
import com.idiomiko.core.data.network.createAccountHttpClient
import com.idiomiko.core.data.account.thisDevice
import com.idiomiko.core.data.account.defaultAccountServer
import com.idiomiko.core.data.account.AccountServer
import com.idiomiko.core.data.account.AccountClient
import com.idiomiko.core.data.account.AccountPlatform
import com.idiomiko.core.data.repository.PlaylistRepositoryImpl
import com.idiomiko.core.domain.playlists.ListeningTime
import com.idiomiko.core.domain.playlists.PlaylistRepository
import com.idiomiko.core.domain.playlists.PlaylistService
import com.idiomiko.core.domain.service.ChineseScriptExamples
import com.russhwolf.settings.Settings
import com.idiomiko.core.data.backup.BackupRepositoryImpl
import com.idiomiko.core.data.bookpack.BookPackRepository
import com.idiomiko.core.data.coursepack.CoursePackRepository
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.dictionary.DictionaryDatabaseProvider
import com.idiomiko.core.data.dictionary.DictionaryDownloader
import com.idiomiko.core.data.dictionary.DictionaryPackStorage
import com.idiomiko.core.data.dictionary.SettingsImportedDictionaryStore
import com.idiomiko.core.data.discover.AdamastorCatalog
import com.idiomiko.core.data.discover.AdlCatalog
import com.idiomiko.core.data.discover.AozoraCatalog
import com.idiomiko.core.data.discover.BenYehudaCatalog
import com.idiomiko.core.data.discover.ElejandriaCatalog
import com.idiomiko.core.data.discover.ElgCatalog
import com.idiomiko.core.data.discover.GanjoorCatalog
import com.idiomiko.core.data.discover.GdlCatalog
import com.idiomiko.core.data.discover.GutenbergCatalog
import com.idiomiko.core.data.discover.LiberLiberCatalog
import com.idiomiko.core.data.discover.LitteraturbankenCatalog
import com.idiomiko.core.data.discover.StoryWeaverCatalog
import com.idiomiko.core.data.discover.StorybooksCatalog
import com.idiomiko.core.data.discover.TextGridCatalog
import com.idiomiko.core.data.discover.WikisourceCatalog
import com.idiomiko.core.data.discover.WolneLekturyCatalog
import com.idiomiko.core.data.export.AnkiPackagerImpl
import com.idiomiko.core.data.files.pdfSupported
import com.idiomiko.core.data.mdict.DictionaryFiles
import com.idiomiko.core.data.network.CommonsRecordingSource
import com.idiomiko.core.data.network.GoogleTranslationProvider
import com.idiomiko.core.data.network.KtorRecordingFetcher
import com.idiomiko.core.data.network.LeipzigExamplesProvider
import com.idiomiko.core.data.network.MyMemoryTranslationProvider
import com.idiomiko.core.data.network.SourceRoutingExamples
import com.idiomiko.core.data.network.TatoebaExamplesProvider
import com.idiomiko.core.data.network.TranslationSuggestionProvider
import com.idiomiko.core.data.network.WiktApiClient
import com.idiomiko.core.data.network.WiktionaryTranslationProvider
import com.idiomiko.core.data.network.createHttpClient
import com.idiomiko.core.data.network.createPlainHttpClient
import com.idiomiko.core.data.news.FeedNewsReader
import com.idiomiko.core.data.news.WikiArticles
import com.idiomiko.core.data.repository.BookRepositoryImpl
import com.idiomiko.core.data.repository.VideoRepositoryImpl
import com.idiomiko.core.domain.media.VideoRepository
import com.idiomiko.core.domain.media.VideoService
import com.idiomiko.core.domain.media.VideoOpenRequests
import com.idiomiko.core.data.repository.CourseRepositoryImpl
import com.idiomiko.core.data.repository.DatabaseMaintenanceImpl
import com.idiomiko.core.data.repository.DictionaryRepositoryImpl
import com.idiomiko.core.data.repository.FlashcardRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.LearningStatsRepositoryImpl
import com.idiomiko.core.data.repository.SentenceTranslationCacheImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.data.repository.VocabularyLevelRepositoryImpl
import com.idiomiko.core.data.repository.WordsReadRepositoryImpl
import com.idiomiko.core.data.settings.SecureStore
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.data.sync.ServicePacks
import com.idiomiko.core.data.sync.SyncEngine
import com.idiomiko.core.data.sync.SyncedPacks
import com.idiomiko.core.data.sync.SyncTransport
import com.idiomiko.core.data.web.HttpWebPageReader
import com.idiomiko.core.data.web.HttpWebPictures
import com.idiomiko.core.data.web.webPagesSupported
import com.idiomiko.core.domain.backup.BackupRepository
import com.idiomiko.core.domain.books.BookPackService
import com.idiomiko.core.domain.books.BookPackStore
import com.idiomiko.core.domain.books.discover.BookDiscoveryService
import com.idiomiko.core.domain.courses.CoursePackService
import com.idiomiko.core.domain.courses.CoursePackStore
import com.idiomiko.core.domain.courses.CourseRepository
import com.idiomiko.core.domain.courses.CourseKnowledge
import com.idiomiko.core.domain.courses.CourseService
import com.idiomiko.core.domain.courses.InstalledCoursePacks
import com.idiomiko.core.domain.courses.SampleCourseSource
import com.idiomiko.core.domain.dictionary.DictionaryPackStore
import com.idiomiko.core.domain.dictionary.DictionaryPacks
import com.idiomiko.core.domain.dictionary.ImportedDictionaries
import com.idiomiko.core.domain.dictionary.OfflineDictionary
import com.idiomiko.core.domain.dictionary.OnlineWiktionary
import com.idiomiko.core.domain.export.AnkiDeckExportService
import com.idiomiko.core.domain.export.AnkiExportService
import com.idiomiko.core.domain.export.AnkiPackager
import com.idiomiko.core.domain.practice.PracticeMaterialLoader
import com.idiomiko.core.domain.practice.PracticeExampleRepository
import com.idiomiko.core.domain.practice.PracticeProgressRepository
import com.idiomiko.core.data.repository.PracticeProgressRepositoryImpl
import com.idiomiko.core.domain.practice.WordForms
import com.idiomiko.core.data.repository.PracticeExampleRepositoryImpl
import com.idiomiko.core.domain.flashcards.DictionaryFormLemmas
import com.idiomiko.core.domain.flashcards.FlashcardService
import com.idiomiko.core.domain.flashcards.PersonalScheduling
import com.idiomiko.core.domain.flashcards.FlashcardStatsService
import com.idiomiko.core.domain.flashcards.RetentionAdvisor
import com.idiomiko.core.domain.flashcards.ReadingWords
import com.idiomiko.core.domain.flashcards.SentenceTranslations
import com.idiomiko.core.domain.frequency.VocabularyLevelRepository
import com.idiomiko.core.domain.news.NewsFeedReader
import com.idiomiko.core.domain.news.NewsService
import com.idiomiko.core.domain.news.WikiReader
import com.idiomiko.core.domain.repository.BookRepository
import com.idiomiko.core.domain.repository.DatabaseMaintenance
import com.idiomiko.core.domain.repository.DictionaryRepository
import com.idiomiko.core.domain.repository.FlashcardRepository
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.repository.SentenceTranslationCache
import com.idiomiko.core.domain.repository.TermRepository
import com.idiomiko.core.domain.repository.WordsReadRepository
import com.idiomiko.core.domain.service.BookService
import com.idiomiko.core.domain.service.BookStatsService
import com.idiomiko.core.domain.service.CachedSentenceTranslator
import com.idiomiko.core.domain.service.ChineseScriptService
import com.idiomiko.core.domain.service.CutWordsMigration
import com.idiomiko.core.domain.service.EmptyExceptionsMigration
import com.idiomiko.core.domain.service.ClearDataService
import com.idiomiko.core.domain.service.DemoDataService
import com.idiomiko.core.domain.service.DictionaryService
import com.idiomiko.core.domain.service.ExampleRecordings
import com.idiomiko.core.domain.courses.CourseExamplesProvider
import com.idiomiko.core.domain.flashcards.CardSentences
import com.idiomiko.core.domain.service.CorpusWords
import com.idiomiko.core.domain.service.ExampleSentencesProvider
import com.idiomiko.core.domain.service.GoogleTranslation
import com.idiomiko.core.domain.service.LanguageRemovalService
import com.idiomiko.core.domain.language.LanguageCatalog
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.service.LanguageService
import com.idiomiko.core.domain.service.LanguageSetupService
import com.idiomiko.core.domain.service.LanguageAssets
import com.idiomiko.core.domain.service.NetworkStatus
import com.idiomiko.core.domain.service.AnyNetwork
import com.idiomiko.core.domain.service.LearningLanguageService
import com.idiomiko.core.domain.service.LearningTranslations
import com.idiomiko.core.domain.service.LexiconService
import com.idiomiko.core.domain.service.LocalTranslation
import com.idiomiko.core.domain.service.ReadingService
import com.idiomiko.core.domain.service.RoutingSentenceTranslator
import com.idiomiko.core.domain.service.SentenceAudio
import com.idiomiko.core.domain.service.SentenceTranslator
import com.idiomiko.core.domain.service.StatsService
import com.idiomiko.core.domain.service.TermPopupBuilder
import com.idiomiko.core.domain.service.TermService
import com.idiomiko.core.domain.service.TermTranslationProvider
import com.idiomiko.core.domain.service.TranslationEngine
import com.idiomiko.core.domain.service.TranslationLanguageKeeper
import com.idiomiko.core.domain.service.WordReadings
import com.idiomiko.core.domain.service.WordRecordings
import com.idiomiko.core.domain.service.WordTranslationService
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.service.engineFor
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.domain.stats.LearningStatsRepository
import com.idiomiko.core.domain.web.WebPageReader
import com.idiomiko.core.domain.web.WebPictures
import org.koin.core.module.Module
import org.koin.dsl.module
import com.idiomiko.core.domain.practice.PracticeExampleStore

/** Platform-specific bindings: the database driver factories and [Settings]. */
expect val platformDataModule: Module

val dataModule: Module = module {
    includes(platformDataModule)

    single { DatabaseProvider(get()) }
    single { SettingsRepositoryImpl(get<Settings>(), get<SecureStore>()) }
    single<SettingsRepository> { get<SettingsRepositoryImpl>() }
    single<BackupRepository> { BackupRepositoryImpl(get(), get(), get()) }
    single {
        val provider = get<DatabaseProvider>()
        val platform = getOrNull<AccountPlatform>()
        AccountClient(
            platform?.http ?: createAccountHttpClient(),
            AccountServer { defaultAccountServer() },
            platform?.store ?: get<SecureStore>(),
            device = { thisDevice(provider) },
            sharedSignIn = platform?.sharedSignIn ?: false,
        )
    } binds arrayOf(AccountRepository::class, SyncTransport::class)
    single<LibrarySync> {
        SyncEngine(get(), get(), get<Settings>(), addLanguage = { code ->
            LanguageCatalog.targetLanguages.firstOrNull { LanguageCodes.codeFor(it) == code }?.let { get<LanguageService>().loadPredefined(it, withTutorial = false) }
        }, userSettings = get(), packs = get(), installer = ServicePacks(get(), get()), openLesson = { course, lesson -> get<CourseService>().openLesson(course, lesson) })
    }
    single { SyncedPacks(get<Settings>()) }
    single<PackChoices> { get<SyncedPacks>() }
    single { SyncRunner(get()) }
    single<LanguageRepository> { LanguageRepositoryImpl(get()) }
    single<BookRepository> { BookRepositoryImpl(get()) }
    single<VideoRepository> { VideoRepositoryImpl(get()) }
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
    single { DictionaryService(get(), get(), get(), DictionaryFiles()) }
    single { WordTranslationService(get(), get<DictionaryService>(), get(), get(), sentences = getOrNull(), store = getOrNull()) }
    single<OfflineDictionary> { get<DictionaryService>() }

    single { createHttpClient() }
    // The Leipzig service sends no CORS headers, so a browser cannot ask it.
    if (webPagesSupported) {
        single { LeipzigExamplesProvider(get()) }
        single<CorpusWords> { get<LeipzigExamplesProvider>() }
    }
    single { CourseExamplesProvider(get(), get(), get()) }
    single<ExampleSentencesProvider> {
        ChineseScriptExamples(SourceRoutingExamples(TatoebaExamplesProvider(get()), getOrNull<LeipzigExamplesProvider>(), get<CourseExamplesProvider>()))
    }
    single { MyMemoryTranslationProvider(get(), get()) }
    single { GoogleTranslationProvider(get(), get()) }
    single<GoogleTranslation> { get<GoogleTranslationProvider>() }
    single<SentenceTranslationCache> { SentenceTranslationCacheImpl(get()) }
    single<SentenceTranslator> {
        val settings = get<SettingsRepository>()
        val local = get<LocalTranslation>().translator
        val engine: suspend (Language) -> TranslationEngine = { settings.current.engineFor(it, local) }
        val routed = RoutingSentenceTranslator(get<MyMemoryTranslationProvider>(), get<GoogleTranslationProvider>(), local, engine)
        // Stored translations carry the engine in their key so switching engines never mixes results.
        CachedSentenceTranslator(routed, get(), targetLanguage = { language ->
            val native = settings.current.nativeLanguage
            when (engine(language)) {
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
    single { AnkiDeckExportService(get(), get(), get(), get(), get(), get()) }
    single { PracticeMaterialLoader(get(), get(), get(), get()) }
    single<PracticeExampleRepository> { PracticeExampleRepositoryImpl(get()) }
    single<PracticeProgressRepository> { PracticeProgressRepositoryImpl(get()) }
    single { WordForms(getOrNull(), get()) }
    single { PracticeExampleStore(get(), get(), get(), get(), get(), get(), get()) }
    single { TermService(get(), get(), translationsWanted = get<LearningTranslations>()::request, chineseTables = getOrNull(), learningStarted = { get<PracticeExampleStore>().request(it); get<CardSentences>().request(it) }) }
    single { CardSentences(get(), get(), get(), get()) }
    single { ExampleRecordings(KtorRecordingFetcher(get()), get()) }
    single { WordRecordings(CommonsRecordingSource(get())) }
    // In a browser the requests go without the app's User-Agent, which would need a preflight WiktApi refuses.
    single<OnlineWiktionary> { if (webPagesSupported) WiktApiClient(get(), get()) else WiktApiClient(createPlainHttpClient(), get(), failureIsMiss = true) }
    single { SentenceAudio(get(), get(), get(), get()) }
    single { TranslationLanguageKeeper(get(), get(), get<DictionaryService>(), get(), get(), get()) }
    single { LexiconService(get(), get(), getOrNull(), getOrNull()) }
    single { ChineseScriptService(get(), getOrNull(), get(), get()) }
    single {
        val dictionaries = get<DictionaryService>()
        val lexicons = get<LexiconService>()
        // The installed packs' words are the lexicon's too, which decide what a word is cut into.
        CutWordsMigration(get(), get(), get()) { language -> dictionaries.refresh(); lexicons.prepare(language) }
    }
    single { EmptyExceptionsMigration(get(), get()) }
    single { WordReadings(get(), get()) }
    single { ReadingService(get(), get(), get(), get(), get(), get<LexiconService>()) }
    single { TermPopupBuilder(get(), get(), get()) }
    single { BookService(get(), get()) }
    single { VideoService(get(), get()) }
    single { VideoOpenRequests() }
    single { BookStatsService(get(), get(), get(), get()) }
    single { LanguageService(get(), get(), get()) }
    single { LearningLanguageService(get(), get(), get(), get()) }
    single { DemoDataService(get(), get(), get(), get(), get()) }
    single<LearningStatsRepository> { LearningStatsRepositoryImpl(get()) }
    single<ListeningLog> { ListeningLogImpl(get()) }
    single<OwnFontRepository> { OwnFontRepositoryImpl(get()) }
    single { StatsService(get(), get()) }
    single<CourseRepository> { CourseRepositoryImpl(get()) }
    single<CoursePackStore> { CoursePackRepository(get()) }
    single<SampleCourseSource> { InstalledCoursePacks(get()) }
    single { CourseService(get(), get(), get(), get(), get()) }
    single { CourseKnowledge(get(), get()) }
    single { LessonSummaries(get(), get(), get()) }
    single { CoursePackService(get(), get(), get()) }
    single<PlaylistRepository> { PlaylistRepositoryImpl(get()) }
    single { ListeningTime(get()) }
    single { PlaylistService(get(), get(), get(), get()) }
    single<BookPackStore> { BookPackRepository(get()) }
    single { BookPackService(get(), get(), get(), get(), get()) }
    single { BookDiscoveryService(listOfNotNull(GutenbergCatalog(get()), WikisourceCatalog(get()), GdlCatalog(get()), StorybooksCatalog(get()), StoryWeaverCatalog(get()).takeIf { pdfSupported }, WolneLekturyCatalog(get()), AozoraCatalog(get()), LitteraturbankenCatalog(get()), BenYehudaCatalog(get()), AdlCatalog(get()), GanjoorCatalog(get()), AdamastorCatalog(get()), ElgCatalog(get()), TextGridCatalog(get()), LiberLiberCatalog(get()), ElejandriaCatalog(get())), get(), get(), get()) }
    single<NewsFeedReader> { FeedNewsReader(get()) }
    // A browser may fetch only the sites that allow it, so there the book form offers no web pages.
    if (webPagesSupported) single<WebPageReader> { HttpWebPageReader(get()) }
    single<WikiReader> { WikiArticles(get()) }
    single<WebPictures> { HttpWebPictures(createPlainHttpClient()) }
    single { NewsService(get(), get(), get(), get(), get()) }
    single { LanguageSetupService(get(), get(), get(), get(), get(), get(), getOrNull()) }
    single { LanguageAssets(get(), get(), get(), get(), getOrNull<NetworkStatus>() ?: AnyNetwork, getOrNull<TextRecognizer>() ?: NoTextRecognizer, frequency = getOrNull()) }
    single { ClearDataService(get(), get(), get(), get(), get(), get(), get()) }
    single { LanguageRemovalService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<FlashcardRepository> { FlashcardRepositoryImpl(get()) }
    single<VocabularyLevelRepository> { VocabularyLevelRepositoryImpl(get()) }
    single { DictionaryFormLemmas(get(), get(), get()) }
    single { SentenceTranslations(get(), get(), get()) }
    single { ReadingWords(get(), get(), get(), get(), get()) }
    single { FlashcardService(get(), get(), get(), formLemmas = get<DictionaryFormLemmas>()::lemmas, wordsAhead = get<ReadingWords>()::termIds) }
    single { PersonalScheduling(get(), get()) }
    single { FlashcardStatsService(get(), get()) }
    single { RetentionAdvisor(get(), get()) }
}
