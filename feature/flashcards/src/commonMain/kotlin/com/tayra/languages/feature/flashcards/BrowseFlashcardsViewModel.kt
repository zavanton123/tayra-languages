package com.tayra.languages.feature.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.flashcards.AnkiImportException
import com.tayra.languages.core.domain.flashcards.AnkiImportService
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.flashcards.SentenceTranslations
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.components.LIST_PAGE_SIZES
import com.tayra.languages.core.ui.components.pageCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** One card as the browser lists it. */
data class BrowseRow(
    val termId: Long,
    val word: String,
    val translation: String,
    val sentence: String?,
    /** [sentence]'s translation as kept with the word; null while it has none. */
    val sentenceTranslation: String? = null,
    val state: CardState,
    val suspended: Boolean,
    val due: Instant,
    /** Waiting to be reviewed today: a card not new or suspended whose time comes before the study day ends. */
    val isDue: Boolean,
    val intervalDays: Int,
    val reps: Int,
    val lapses: Int,
    val lastReview: Instant?,
    /** When the word became one being learned and the card was made. */
    val created: Instant?,
    /** Made in the current study day. */
    val addedToday: Boolean = false,
    /** FSRS's difficulty, from 1 to 10, and stability in days; null before the first answer. */
    val difficulty: Double?,
    val stability: Double?,
    val retrievability: Double?,
    val languageCode: String?,
    val rightToLeft: Boolean,
    val languageId: Long = 0,
    /** Forgotten often enough to be a leech. */
    val isLeech: Boolean = false,
)

/** The groups of cards the browser can show, as the items of Anki's sidebar; [ADDED_TODAY] is opened from its figure only. */
enum class BrowseView { ALL, DUE, NEW, LEARNING, REVIEW, FORGOTTEN, LEECHES, SUSPENDED, ADDED_TODAY }

enum class BrowseSortField { WORD, STATE, DUE, INTERVAL, REVIEWS, LAPSES, LAST_REVIEW, CREATED }

data class BrowseSort(val field: BrowseSortField, val ascending: Boolean = true)

/** How many cards each [BrowseView] holds. */
data class BrowseCounts(val all: Int = 0, val due: Int = 0, val new: Int = 0, val learning: Int = 0, val review: Int = 0, val forgotten: Int = 0, val leeches: Int = 0, val suspended: Int = 0, val addedToday: Int = 0) {
    operator fun get(view: BrowseView): Int = when (view) {
        BrowseView.ALL -> all
        BrowseView.DUE -> due
        BrowseView.NEW -> new
        BrowseView.LEARNING -> learning
        BrowseView.REVIEW -> review
        BrowseView.FORGOTTEN -> forgotten
        BrowseView.LEECHES -> leeches
        BrowseView.SUSPENDED -> suspended
        BrowseView.ADDED_TODAY -> addedToday
    }
}

/** An import from Anki: under way, with how many words are done, finished, or failed with why. */
sealed interface AnkiImport {
    data class Working(val done: Int = 0, val total: Int = 0) : AnkiImport
    data class Done(val report: AnkiImportService.Report) : AnkiImport
    data class Failed(val reason: String?) : AnkiImport
}

/** The card opened for a closer look, with its review history. */
data class BrowseDetail(
    val row: BrowseRow,
    /** The sentence's translation is being made. */
    val translating: Boolean = false,
    val reviews: List<FlashcardReview> = emptyList(),
    val loading: Boolean = true,
    /** Where the card's word stands in its sentence, the part its front blanks out; null when it is not found. */
    val wordRange: IntRange? = null,
)

data class BrowseUiState(
    val loading: Boolean = true,
    val languageName: String = "",
    val search: String = "",
    val view: BrowseView = BrowseView.ALL,
    val sort: BrowseSort = BrowseSort(BrowseSortField.DUE),
    /** The cards of the chosen view that match the search, in the sort order. */
    val rows: List<BrowseRow> = emptyList(),
    /** The page of [rows] shown, from 0, and how many cards a page holds. */
    val page: Int = 0,
    val pageSize: Int = LIST_PAGE_SIZES[1],
    val counts: BrowseCounts = BrowseCounts(),
    val selected: Set<Long> = emptySet(),
    val detail: BrowseDetail? = null,
    val now: Instant = Instant.DISTANT_PAST,
    /** A card to bring into view in the list, once; the one the browser was opened for. */
    val focus: Long? = null,
    /** Whether Anki packages can be imported here. */
    val canImportAnki: Boolean = false,
    val ankiImport: AnkiImport? = null,
    /** Whether cards forgotten often become leeches, which then have a group of their own. */
    val leeches: Boolean = true,
) {
    val filtered: Boolean get() = search.isNotBlank() || view != BrowseView.ALL

    val pageCount: Int get() = pageCount(rows.size, pageSize)

    /** The cards on the page shown. */
    val pageRows: List<BrowseRow> get() = rows.drop(page * pageSize).take(pageSize)
}

/** Every flashcard of the language being learned: searched, sorted, looked into and managed, as in Anki's browser. */
class BrowseFlashcardsViewModel(
    private val service: FlashcardService,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val terms: TermRepository,
    private val ankiImport: AnkiImportService? = null,
    /** Keeps the sentences' translations with their words; null shows none, as in tests. */
    private val sentences: SentenceTranslations? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(
        BrowseUiState(
            canImportAnki = ankiImport?.available == true,
            pageSize = settings.current.flashcardBrowsePageSize.takeIf { it in LIST_PAGE_SIZES } ?: LIST_PAGE_SIZES[1],
        ),
    )
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    private var all: List<BrowseRow> = emptyList()

    private val languageId: Long? get() = settings.current.currentLanguageId.takeIf { it != 0L }

    init {
        load()
        followChanges()
        // The cards follow the language chosen in the top bar.
        viewModelScope.launch {
            settings.settings.map { it.currentLanguageId }.distinctUntilChanged().drop(1).collect {
                _state.update { it.copy(selected = emptySet(), detail = null, page = 0) }
                load()
            }
        }
    }

    /**
     * The cards follow what changes elsewhere, as a term edited over the browser or the card's details:
     * a burst of writes, such as sentences translated one after another, is taken in once it settles.
     */
    @OptIn(FlowPreview::class)
    private fun followChanges() {
        viewModelScope.launch {
            service.observeChanges().drop(1).debounce(CHANGES_SETTLE).collect { refresh() }
        }
    }

    /** Loads the cards; [open] names one to open and bring into view, as when the browser is opened for it. */
    fun load(open: Long? = null) {
        viewModelScope.launch {
            refresh()
            if (open != null && all.any { it.termId == open }) {
                open(open)
                _state.update { s -> s.copy(focus = open, page = s.rows.indexOfFirst { it.termId == open }.coerceAtLeast(0) / s.pageSize) }
            }
        }
    }

    /** Opens [termId]'s card alone, for its details over another screen; false when it has none. */
    suspend fun openAlone(termId: Long): Boolean {
        refresh()
        if (all.none { it.termId == termId }) return false
        open(termId)
        return true
    }

    /** The card asked to be brought into view is in view. */
    fun focusShown() {
        _state.update { it.copy(focus = null) }
    }

    private suspend fun refresh() {
        run {
            val browse = service.browse(languageId)
            val leechThreshold = settings.current.flashcardLeechThreshold
            val language = languageId?.let { languages.getById(it) }
            val byLanguage = browse.cards.map { it.term.languageId }.distinct().associateWith { id -> language?.takeIf { it.id == id } ?: languages.getById(id) }
            all = browse.cards.map { entry ->
                val card = entry.card
                val s = card.schedule
                val termLanguage = byLanguage[entry.term.languageId]
                BrowseRow(
                    termId = entry.term.id,
                    word = entry.term.displayText,
                    translation = entry.term.translation.orEmpty().trim(),
                    sentence = entry.term.sentence?.trim()?.takeIf { it.isNotEmpty() },
                    sentenceTranslation = entry.term.sentenceTranslation?.trim()?.takeIf { it.isNotEmpty() },
                    state = s.state,
                    suspended = card.suspended,
                    due = s.due,
                    isDue = !card.suspended && s.state != CardState.NEW && s.due < browse.endOfDay,
                    intervalDays = s.intervalDays,
                    reps = s.reps,
                    lapses = s.lapses,
                    lastReview = s.lastReview,
                    created = card.createdAt ?: entry.term.createdAt,
                    addedToday = (card.createdAt ?: entry.term.createdAt)?.let { it >= browse.startOfDay } == true,
                    difficulty = s.memory?.difficulty,
                    stability = s.memory?.stability,
                    retrievability = entry.retrievability,
                    languageCode = termLanguage?.let { LanguageCodes.codeFor(it.name) },
                    rightToLeft = termLanguage?.rightToLeft == true,
                    languageId = entry.term.languageId,
                    isLeech = FlashcardService.isLeech(s.lapses, leechThreshold),
                )
            }
            val ids = all.map { it.termId }.toSet()
            _state.update { s ->
                val detail = s.detail?.let { d -> all.firstOrNull { it.termId == d.row.termId }?.let { row -> d.copy(row = row) } }
                s.copy(loading = false, languageName = language?.name.orEmpty(), counts = counts(all), leeches = leechThreshold > 0, selected = s.selected intersect ids, detail = detail, now = browse.now)
            }
            publish()
            // The sentence may have changed, here or in the term's editor.
            _state.value.detail?.let { findWord(it.row.termId) }
        }
    }

    private fun counts(rows: List<BrowseRow>) = BrowseCounts(
        all = rows.size,
        due = rows.count { it.isDue },
        new = rows.count { it.state == CardState.NEW && !it.suspended },
        learning = rows.count { (it.state == CardState.LEARNING || it.state == CardState.RELEARNING) && !it.suspended },
        review = rows.count { it.state == CardState.REVIEW && !it.suspended },
        forgotten = rows.count { it.lapses > 0 },
        leeches = rows.count { it.isLeech },
        suspended = rows.count { it.suspended },
        addedToday = rows.count { it.addedToday },
    )

    private fun BrowseRow.inView(view: BrowseView): Boolean = when (view) {
        BrowseView.ALL -> true
        BrowseView.DUE -> isDue
        BrowseView.NEW -> state == CardState.NEW && !suspended
        BrowseView.LEARNING -> (state == CardState.LEARNING || state == CardState.RELEARNING) && !suspended
        BrowseView.REVIEW -> state == CardState.REVIEW && !suspended
        BrowseView.FORGOTTEN -> lapses > 0
        BrowseView.LEECHES -> isLeech
        BrowseView.SUSPENDED -> suspended
        BrowseView.ADDED_TODAY -> addedToday
    }

    /** Applies the view, the search and the sort to the cards, staying on the page shown while it still has cards. */
    private fun publish() {
        val s = _state.value
        val needle = s.search.trim().lowercase()
        val shown = all.filter { row ->
            row.inView(s.view) && (needle.isEmpty() || row.word.lowercase().contains(needle) || row.translation.lowercase().contains(needle) || row.sentence?.lowercase()?.contains(needle) == true)
        }
        _state.update { it.copy(rows = shown.sortedWith(comparator(s.sort)), page = it.page.coerceAtMost(pageCount(shown.size, it.pageSize) - 1)) }
    }

    private fun comparator(sort: BrowseSort): Comparator<BrowseRow> {
        val byField: Comparator<BrowseRow> = when (sort.field) {
            BrowseSortField.WORD -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.word }
            BrowseSortField.STATE -> compareBy<BrowseRow> { it.suspended }.thenBy { it.state.ordinal }
            // New cards have no date to come: they go after the cards that do, in the order they will be shown.
            BrowseSortField.DUE -> compareBy<BrowseRow> { it.state == CardState.NEW }.thenBy { it.due }
            BrowseSortField.INTERVAL -> compareBy { it.intervalDays }
            BrowseSortField.REVIEWS -> compareBy { it.reps }
            BrowseSortField.LAPSES -> compareBy { it.lapses }
            BrowseSortField.LAST_REVIEW -> compareBy(nullsFirst()) { it.lastReview }
            BrowseSortField.CREATED -> compareBy(nullsFirst()) { it.created }
        }
        val ordered = if (sort.ascending) byField else byField.reversed()
        return ordered.thenBy(String.CASE_INSENSITIVE_ORDER) { it.word }
    }

    fun setSearch(search: String) {
        _state.update { it.copy(search = search, page = 0) }
        publish()
    }

    fun setView(view: BrowseView) {
        _state.update { it.copy(view = view, page = 0) }
        publish()
    }

    fun goToPage(page: Int) {
        _state.update { it.copy(page = page.coerceIn(0, it.pageCount - 1)) }
    }

    /** Shows [size] cards a page from the first page on, and keeps the size for the next time. */
    fun setPageSize(size: Int) {
        _state.update { it.copy(pageSize = size, page = 0) }
        viewModelScope.launch { settings.update { it.copy(flashcardBrowsePageSize = size) } }
    }

    /** Sorts by [field]; choosing the field already sorted by turns the order round. */
    fun sortBy(field: BrowseSortField) {
        _state.update { s ->
            s.copy(page = 0, sort = if (s.sort.field == field) s.sort.copy(ascending = !s.sort.ascending) else BrowseSort(field, ascending = field == BrowseSortField.WORD || field == BrowseSortField.DUE || field == BrowseSortField.STATE))
        }
        publish()
    }

    fun toggleSelected(termId: Long) {
        _state.update { s -> s.copy(selected = if (termId in s.selected) s.selected - termId else s.selected + termId) }
    }

    /** Selects or clears every card of the view and search, on every page. */
    fun selectAllShown(selected: Boolean) {
        _state.update { s -> s.copy(selected = if (selected) s.selected + s.rows.map { it.termId } else s.selected - s.rows.map { it.termId }.toSet()) }
    }

    fun clearSelection() {
        _state.update { it.copy(selected = emptySet()) }
    }

    /** Opens the card for a closer look, with its review history. */
    fun open(termId: Long) {
        val row = all.firstOrNull { it.termId == termId } ?: return
        _state.update { it.copy(detail = BrowseDetail(row)) }
        viewModelScope.launch {
            val reviews = service.reviews(termId)
            _state.update { s -> if (s.detail?.row?.termId == termId) s.copy(detail = s.detail.copy(reviews = reviews, loading = false)) else s }
        }
        findWord(termId)
    }

    /** Marks the open card's word in its sentence, as it is now. */
    private fun findWord(termId: Long) {
        viewModelScope.launch {
            val term = terms.getById(termId)
            val range = term?.let { service.clozeRange(it) }
            _state.update { s -> if (s.detail?.row?.termId == termId) s.copy(detail = s.detail.copy(wordRange = range)) else s }
            // A sentence still without its translation gets one, kept with the word.
            val sentence = term?.sentence?.takeIf { it.isNotBlank() }
            if (sentence != null && term.sentenceTranslation.isNullOrBlank() && typingFor != termId) translate(termId, sentence, term.languageId)
        }
    }

    /** The card whose sentence or translation is being typed; no translation is made for it meanwhile. */
    private var typingFor: Long? = null

    /** [termId]'s sentence or translation is being typed, or the typing ended: then a translation it lacks is made. */
    fun editingText(termId: Long, editing: Boolean) {
        if (editing) {
            typingFor = termId
        } else if (typingFor == termId) {
            typingFor = null
            viewModelScope.launch { refresh() }
        }
    }

    /** The sentences a translation was asked for, each once: a translator that gives none is not asked again and again. */
    private val asked = mutableSetOf<Pair<Long, String>>()

    /** Makes and keeps the translation of [termId]'s [sentence], shown as translating meanwhile. */
    private suspend fun translate(termId: Long, sentence: String, languageId: Long) {
        val sentences = sentences ?: return
        if (!asked.add(termId to sentence)) return
        val language = languages.getById(languageId) ?: return
        _state.update { s -> if (s.detail?.row?.termId == termId) s.copy(detail = s.detail.copy(translating = true)) else s }
        sentences.make(termId, sentence, language)
        _state.update { s -> if (s.detail?.row?.termId == termId) s.copy(detail = s.detail.copy(translating = false)) else s }
        refresh()
    }

    fun closeDetail() {
        _state.update { it.copy(detail = null) }
    }

    /** Gives the card's term a new example sentence, the one its front blanks the word out of; blank removes it. */
    fun saveSentence(termId: Long, sentence: String, translation: String? = null) {
        viewModelScope.launch {
            // A new sentence leaves its old translation behind; one that came with it is kept, or one is made.
            terms.updateSentence(termId, sentence.trim().ifEmpty { null })
            translation?.trim()?.takeIf { it.isNotEmpty() }?.let { terms.updateSentenceTranslation(termId, it) }
            refresh()
        }
    }

    /** Writes the translation of the card's sentence by hand; left blank, a new one is made. */
    fun saveSentenceTranslation(termId: Long, translation: String) {
        viewModelScope.launch {
            val written = translation.trim().ifEmpty { null }
            terms.updateSentenceTranslation(termId, written)
            if (written == null) asked.removeAll { it.first == termId }
            refresh()
        }
    }

    /** Makes [form], a word of the card's sentence, the one the card hides. */
    fun chooseHiddenWord(termId: Long, form: String) {
        viewModelScope.launch {
            terms.updateCardForm(termId, form)
            refresh()
        }
    }

    /** Puts the cards aside, or brings them back into the reviews. */
    fun setSuspended(termIds: Collection<Long>, suspended: Boolean) {
        if (termIds.isEmpty()) return
        viewModelScope.launch {
            service.setSuspended(termIds, suspended)
            refresh()
        }
    }

    /** Deletes the cards with their words and review history. */
    fun delete(termIds: Collection<Long>) {
        if (termIds.isEmpty()) return
        viewModelScope.launch {
            service.delete(termIds)
            _state.update { it.copy(selected = it.selected - termIds.toSet()) }
            refresh()
        }
    }

    /** Makes the cards new again, their review schedules forgotten. */
    fun reset(termIds: Collection<Long>) {
        if (termIds.isEmpty()) return
        viewModelScope.launch {
            service.restart(termIds)
            val open = _state.value.detail?.row?.termId
            refresh()
            // The history shown is gone with the reset.
            if (open != null && open in termIds) open(open)
        }
    }

    /** Imports the cards of the Anki package in [bytes], then says what was done. */
    fun importAnki(bytes: ByteArray) {
        val importer = ankiImport ?: return
        if (_state.value.ankiImport is AnkiImport.Working) return
        _state.update { it.copy(ankiImport = AnkiImport.Working()) }
        viewModelScope.launch {
            val result = try {
                AnkiImport.Done(importer.import(bytes) { done, total -> _state.update { it.copy(ankiImport = AnkiImport.Working(done, total)) } })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // An unexpected failure is told as one, without its technical message.
                AnkiImport.Failed((e as? AnkiImportException)?.message)
            }
            _state.update { it.copy(ankiImport = result) }
            refresh()
        }
    }

    fun closeAnkiImport() {
        _state.update { it.copy(ankiImport = null) }
    }

    private companion object {
        /** How long writes elsewhere settle before the cards are read again. */
        val CHANGES_SETTLE = 400.milliseconds
    }
}
