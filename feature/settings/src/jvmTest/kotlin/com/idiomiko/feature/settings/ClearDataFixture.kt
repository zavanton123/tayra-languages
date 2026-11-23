package com.idiomiko.feature.settings

import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.BookRepositoryImpl
import com.idiomiko.core.data.repository.CourseRepositoryImpl
import com.idiomiko.core.data.repository.DatabaseMaintenanceImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.courses.Course
import com.idiomiko.core.domain.courses.CoursePack
import com.idiomiko.core.domain.courses.CoursePackService
import com.idiomiko.core.domain.courses.CoursePackStore
import com.idiomiko.core.domain.courses.CourseService
import com.idiomiko.core.domain.courses.InstalledCoursePacks
import com.idiomiko.core.domain.dictionary.DictionaryEntry
import com.idiomiko.core.domain.dictionary.DictionaryId
import com.idiomiko.core.domain.dictionary.DictionaryPack
import com.idiomiko.core.domain.dictionary.DictionaryPackStore
import com.idiomiko.core.domain.repository.DictionaryRepository
import com.idiomiko.core.domain.service.BookService
import com.idiomiko.core.domain.service.ClearDataService
import com.idiomiko.core.domain.service.DemoDataService
import com.idiomiko.core.domain.service.DictionaryService
import com.idiomiko.core.domain.service.LanguageService
import com.idiomiko.core.domain.service.LocalSpeech
import com.idiomiko.core.domain.service.LocalTranslation
import com.idiomiko.core.domain.service.SpeechAudioCache

/** Clearing a device that has nothing downloaded: only the library in [provider] goes. */
internal fun clearDataService(provider: DatabaseProvider, settings: SettingsRepositoryImpl): ClearDataService {
    val languages = LanguageRepositoryImpl(provider)
    val books = BookRepositoryImpl(provider)
    val bookService = BookService(books, languages)
    return ClearDataService(
        DemoDataService(DatabaseMaintenanceImpl(provider), languages, books, LanguageService(languages, bookService, settings), settings),
        CoursePackService(NoCoursePacks, CourseService(books, languages, bookService, CourseRepositoryImpl(provider), InstalledCoursePacks(NoCoursePacks))),
        DictionaryService(NoDictionaries, NoDictionaries),
        LocalSpeech(emptyList()),
        LocalTranslation(null),
        NoAudio,
        settings,
    )
}

private object NoCoursePacks : CoursePackStore {
    override suspend fun installedSize(pack: CoursePack): Long? = null
    override suspend fun install(pack: CoursePack, onProgress: (Float?) -> Unit) {}
    override suspend fun remove(pack: CoursePack) {}
    override suspend fun courseIds(pack: CoursePack): Set<String> = emptySet()
    override suspend fun courses(pack: CoursePack): List<Course> = emptyList()
}

private object NoDictionaries : DictionaryPackStore, DictionaryRepository {
    override suspend fun installedSize(pack: DictionaryPack): Long? = null
    override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) {}
    override suspend fun remove(pack: DictionaryPack) {}
    override suspend fun isAvailable(dictionary: DictionaryId) = false
    override suspend fun entries(dictionary: DictionaryId, wordLc: String) = emptyList<DictionaryEntry>()
    override suspend fun lemmas(dictionary: DictionaryId, formLc: String) = emptyList<String>()
    override suspend fun close(dictionary: DictionaryId) {}
}

private object NoAudio : SpeechAudioCache {
    override suspend fun read(key: String): ByteArray? = null
    override suspend fun write(key: String, audio: ByteArray) {}
    override suspend fun clear() {}
}
