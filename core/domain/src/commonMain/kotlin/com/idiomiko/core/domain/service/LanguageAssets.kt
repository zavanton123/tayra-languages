package com.idiomiko.core.domain.service

import com.idiomiko.core.domain.books.BookPackService
import com.idiomiko.core.domain.books.BookPacks
import com.idiomiko.core.domain.courses.CoursePacks
import com.idiomiko.core.domain.dictionary.DictionaryPacks
import com.idiomiko.core.domain.dictionary.PackState
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Whether a large download may go now. */
interface NetworkStatus {
    /** True on Wi-Fi or another connection not paid for by the byte; true where that cannot be told. */
    fun unmetered(): Boolean

    /** Emits when the connection changes, so downloads held back for Wi-Fi can go. */
    val changes: Flow<Unit>
}

/** A device whose connection is never paid for by the byte, as far as the app can tell: the desktop and the browser. */
object AnyNetwork : NetworkStatus {
    override fun unmetered() = true
    override val changes: Flow<Unit> = emptyFlow()
}

/**
 * Keeps what the language being learned needs on the device, without asking: its course and book
 * packs and its dictionary on any connection, and a voice and the translation models into the
 * native language on an unmetered one (Wi-Fi). [ensure] is called when the app starts and when a
 * language is chosen; what is missing is fetched in the background, one download after another,
 * and tried again later when one fails or waits for Wi-Fi. Speech recognition stays a choice.
 */
class LanguageAssets(
    private val setup: LanguageSetupService,
    private val books: BookPackService,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val network: NetworkStatus = AnyNetwork,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val retryAfter: Duration = 15.minutes,
) {
    private class Request(val languageId: Long, val anyNetwork: Boolean)

    private val requests = Channel<Request>(Channel.UNLIMITED)
    @Volatile
    private var current: Long? = null

    private val _preparing = MutableStateFlow<Set<SetupKind>>(emptySet())

    /** The kinds of download under way or waiting for the language last asked for, for its features to say they are getting ready. */
    val preparing: StateFlow<Set<SetupKind>> = _preparing.asStateFlow()

    init {
        scope.launch { for (request in requests) fetch(request) }
        scope.launch { network.changes.collect { current?.let { requests.trySend(Request(it, anyNetwork = false)) } } }
    }

    /**
     * Fetches what [languageId] still lacks, in the background; [anyNetwork] lets the large downloads
     * go on a metered connection too, when the learner asked for one.
     */
    fun ensure(languageId: Long, anyNetwork: Boolean = false) {
        if (languageId <= 0) return
        current = languageId
        requests.trySend(Request(languageId, anyNetwork))
    }

    private suspend fun fetch(request: Request) {
        val id = request.languageId
        val code = languages.getById(id)?.name?.let(LanguageCodes::codeFor) ?: return
        val large = request.anyNetwork || network.unmetered()
        val missing = setup.missing(id)
        val voice = missing.filter { it.kind == SetupKind.VOICE }.let { voices -> voices.firstOrNull { it.recommended } ?: voices.firstOrNull() }
        val small = missing.filter { it.kind in SMALL }
        val big = listOfNotNull(voice) + missing.filter { it.kind == SetupKind.TRANSLATION }
        val wanted = small + if (large) big else emptyList()
        // Only what is on its way: a download waiting for Wi-Fi is not getting ready.
        if (id == current) _preparing.value = wanted.map { it.kind }.toSet()
        var failed = false
        books.refresh()
        for (pack in BookPacks.all.filter { it.languageCode == code }) {
            val state = books.packs.value.firstOrNull { it.pack.id == pack.id }?.state
            if (state is PackState.Installed || state is PackState.Downloading) continue
            books.download(pack)
            if (books.packs.value.firstOrNull { it.pack.id == pack.id }?.state is PackState.Failed) failed = true
        }
        for (item in wanted) {
            if (!setup.installNow(item)) failed = true
            if (id == current) _preparing.update { it - item.kind }
        }
        // What is left for Wi-Fi goes when the connection changes; a failure is tried again later.
        if (failed) scope.launch {
            delay(retryAfter)
            if (current == id) requests.trySend(Request(id, request.anyNetwork))
        }
    }

    /**
     * Whether [item], of the language being learned, is one the app keeps on the device, so the
     * downloads list offers no way to remove it: the courses, the dictionary and the translation
     * models, and the last voice that reads the language ([all] being the language's downloads).
     */
    fun required(item: SetupItem, all: List<SetupItem>): Boolean = when (item.kind) {
        SetupKind.COURSES, SetupKind.DICTIONARY, SetupKind.TRANSLATION -> true
        SetupKind.VOICE -> all.filter { it.kind == SetupKind.VOICE && it.status == SetupStatus.INSTALLED }
            // The voices of a model they share go with it, so they count as one.
            .map { if (it.sharedModel) "model:${it.engine}" else it.id }.distinct().size <= 1
        SetupKind.RECOGNITION -> false
    }

    /** Whether the course pack [packId] is one the language [code] keeps. */
    fun requiredCourses(packId: String, code: String?): Boolean = CoursePacks.forLanguage(code ?: return false).any { it.id == packId }

    /** Whether the book pack [packId] is one the language [code] keeps. */
    fun requiredBooks(packId: String, code: String?): Boolean = BookPacks.all.any { it.id == packId && it.languageCode == code }

    /** Whether the dictionary pack [packId] is the one the language [code] keeps for looking words up in the native language. */
    fun requiredDictionary(packId: Any, code: String?): Boolean =
        DictionaryPacks.find(code ?: return false, settings.current.nativeLanguage)?.id == packId

    private companion object {
        val SMALL = setOf(SetupKind.COURSES, SetupKind.DICTIONARY)
    }
}
