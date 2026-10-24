package com.tayra.languages.feature.terms.list

import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.i18n.tr
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermListFilter
import com.tayra.languages.core.domain.repository.TermListPage
import com.tayra.languages.core.domain.repository.TermListSort
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.repository.TermSortField
import com.tayra.languages.core.domain.service.BulkTermUpdate
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.export.AnkiConnection
import com.tayra.languages.core.domain.export.AnkiExportService
import com.tayra.languages.core.domain.service.TermValidationException
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CancellationException

data class TermsListUiState(
    val loading: Boolean = true,
    val filter: TermListFilter = TermListFilter(minStatus = TermStatus.NEW_1),
    val sort: TermListSort = TermListSort(),
    val page: Int = 0,
    val pageSize: Int = 50,
    val terms: List<Term> = emptyList(),
    val totalCount: Int = 0,
    val languages: List<Language> = emptyList(),
    val selected: Set<Long> = emptySet(),
    val message: String? = null,
    val filtersVisible: Boolean = false,
    /** Counts for the current language filter, ignoring the other filters. */
    val totalTerms: Int = 0,
    val learningCount: Int = 0,
    val knownCount: Int = 0,
    /** Progress of an Anki export under way, e.g. "3 of 12", or null. */
    val exporting: String? = null,
    /** Whether that export goes straight into the running Anki rather than to a file. */
    val sendingToAnki: Boolean = false,
) {
    /** The statuses ticked in the filter panel; none means every learning status and known. */
    val chosenStatuses: Set<TermStatus> get() = filter.statuses.orEmpty()

    val ageFiltered: Boolean get() = filter.minAgeDays != null || filter.maxAgeDays != null

    /** All four learning statuses are ticked, shown as one chip. */
    val allLearning: Boolean get() = chosenStatuses.containsAll(TermStatus.selectable.filter { it.isLearning })

    /** How many kinds of filter are on, for the badge on the Filters button: the statuses are one, when the terms were added another. */
    val activeFilterCount: Int get() = (if (chosenStatuses.isNotEmpty()) 1 else 0) + (if (ageFiltered) 1 else 0)

    val pageCount: Int get() = if (totalCount == 0) 1 else (totalCount + pageSize - 1) / pageSize
    fun languageName(id: Long): String = languages.firstOrNull { it.id == id }?.name ?: ""
}

sealed interface TermsListEvent {
    class AnkiReady(val export: AnkiExportService.Export) : TermsListEvent
}

class TermsListViewModel(
    initialTermIds: List<Long>?,
    private val terms: TermRepository,
    languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val termService: TermService,
    private val anki: AnkiExportService,
    /** A running Anki to add notes to directly; only the desktop has one. */
    private val ankiConnection: AnkiConnection? = null,
) : ViewModel() {

    private val filter = MutableStateFlow(
        TermListFilter(
            // A list of given terms shows them whatever their language or status; otherwise the words being learned in the language chosen.
            languageId = settings.current.currentLanguageId.takeIf { it != 0L && initialTermIds == null },
            minStatus = if (initialTermIds != null) TermStatus.UNKNOWN else TermStatus.NEW_1,
            statuses = if (initialTermIds != null) null else LEARNING_STATUSES,
            termIds = initialTermIds,
        ),
    )
    private val sort = MutableStateFlow(TermListSort())
    private val page = MutableStateFlow(0)
    private val pageSize = MutableStateFlow(PAGE_SIZE)
    private val selected = MutableStateFlow<Set<Long>>(emptySet())
    private val message = MutableStateFlow<String?>(null)
    private val exporting = MutableStateFlow<String?>(null)
    private val sendingToAnki = MutableStateFlow(false)
    private val filtersVisible = MutableStateFlow(initialTermIds != null)
    val events = UiEvents<TermsListEvent>()

    init {
        // The list follows the language chosen in the top bar.
        if (initialTermIds == null) {
            viewModelScope.launch {
                settings.settings.map { it.currentLanguageId }.distinctUntilChanged().collect { id ->
                    if (filter.value.languageId != id.takeIf { it != 0L }) updateFilter { it.copy(languageId = id.takeIf { it != 0L }) }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pageFlow = combine(filter, sort, page, pageSize) { f, s, p, size -> Base(TermListPage(emptyList(), 0), f, s, p, size) }
        .flatMapLatest { base -> terms.observeList(base.filter, base.sort, base.page * base.pageSize, base.pageSize).map { base.copy(page_ = it) } }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun count(min: TermStatus, max: TermStatus) = filter
        .map { it.languageId }
        .distinctUntilChanged()
        .flatMapLatest { languageId -> terms.observeList(TermListFilter(languageId = languageId, minStatus = min, maxStatus = max), TermListSort(), 0, 1) }
        .map { it.totalCount }

    private val counts = combine(
        count(TermStatus.NEW_1, TermStatus.WELL_KNOWN),
        count(TermStatus.NEW_1, TermStatus.LEARNING_4),
        count(TermStatus.WELL_KNOWN, TermStatus.WELL_KNOWN),
    ) { total, learning, known -> Counts(total, learning, known) }

    val state: StateFlow<TermsListUiState> = combine(
        pageFlow,
        languages.observeAll(),
        selected,
        combine(message, filtersVisible, exporting, sendingToAnki) { m, v, e, s -> Triple(m, v, e?.let { it to s }) },
        counts,
    ) { base, languageList, sel, (msg, visible, export), c ->
        TermsListUiState(
            loading = false,
            filter = base.filter,
            sort = base.sort,
            page = base.page,
            pageSize = base.pageSize,
            terms = base.page_.items,
            totalCount = base.page_.totalCount,
            languages = languageList,
            selected = sel,
            message = msg,
            exporting = export?.first,
            sendingToAnki = export?.second == true,
            filtersVisible = visible,
            totalTerms = c.total,
            learningCount = c.learning,
            knownCount = c.known,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TermsListUiState())

    private data class Base(val page_: TermListPage, val filter: TermListFilter, val sort: TermListSort, val page: Int, val pageSize: Int)

    private data class Counts(val total: Int, val learning: Int, val known: Int)

    fun updateFilter(transform: (TermListFilter) -> TermListFilter) {
        filter.value = transform(filter.value)
        page.value = 0
        selected.value = emptySet()
    }

    /** Takes every filter off: the list shows the words being learned and the known ones. */
    fun clearFilters() = updateFilter { TermListFilter(languageId = it.languageId, minStatus = TermStatus.NEW_1) }

    /** Puts the filters back as the screen opens: the words being learned (statuses 1 to 4). */
    fun resetFilters() = updateFilter { TermListFilter(languageId = it.languageId, minStatus = TermStatus.NEW_1, statuses = LEARNING_STATUSES) }

    fun toggleFilters() { filtersVisible.value = !filtersVisible.value }

    /** Ticks or unticks a status; with none ticked the list shows every status but ignored. */
    /** Unticks all four learning statuses, from their one chip. */
    fun removeLearning() = updateFilter { filter -> filter.copy(statuses = filter.statuses.orEmpty().filterNot { it.isLearning }.toSet().ifEmpty { null }) }

    /** Shows the words being learned, at any level, or stops showing them: Learning's filter without the levels. */
    fun toggleLearning() = updateFilter { filter ->
        val chosen = filter.statuses.orEmpty()
        filter.copy(statuses = (if (chosen.any { it.isLearning }) chosen.filterNot { it.isLearning }.toSet() else chosen + LEARNING_STATUSES).ifEmpty { null })
    }

    fun toggleStatus(status: TermStatus) = updateFilter {
        val chosen = it.statuses.orEmpty().let { set -> if (status in set) set - status else set + status }
        it.copy(statuses = chosen.ifEmpty { null })
    }

    /** Terms added between [fromDays] and [toDays] days ago; null leaves that end open. */
    fun setAddedRange(fromDays: Int?, toDays: Int?) = updateFilter { it.copy(minAgeDays = fromDays, maxAgeDays = toDays) }

    fun setSort(value: TermListSort) {
        sort.value = value
        page.value = 0
    }

    fun setPageSize(size: Int) {
        pageSize.value = size
        page.value = 0
    }

    fun delete(termId: Long) = viewModelScope.launch {
        termService.deleteAll(setOf(termId))
        selected.value = selected.value - termId
    }

    fun sortBy(field: TermSortField) {
        val current = sort.value
        sort.value = if (current.field == field) current.copy(ascending = !current.ascending) else TermListSort(field, ascending = true)
        page.value = 0
    }

    /** A phone's list goes on as it is scrolled: the next terms join those shown, all on the one page. */
    fun showMore() {
        val s = state.value
        if (s.terms.size >= s.totalCount) return
        page.value = 0
        pageSize.value = s.terms.size + PAGE_SIZE
    }

    fun goToPage(index: Int) {
        page.value = index.coerceIn(0, state.value.pageCount - 1)
    }

    fun toggleSelected(id: Long) {
        selected.value = if (id in selected.value) selected.value - id else selected.value + id
    }

    fun selectAllVisible(select: Boolean) {
        selected.value = if (select) state.value.terms.map { it.id }.toSet() else emptySet()
    }

    fun dismissMessage() { message.value = null }

    fun setStatus(termId: Long, status: TermStatus) = viewModelScope.launch { termService.setStatus(listOf(termId), status) }

    fun deleteSelected() = viewModelScope.launch {
        termService.deleteAll(selected.value)
        selected.value = emptySet()
    }

    /**
     * Applies [update] to the selected terms. With [keepLearningLevels], as when the levels are not shown,
     * setting Learning leaves the words already being learned at their level and changes the rest.
     */
    fun applyBulkUpdate(update: BulkTermUpdate, keepLearningLevels: Boolean = false) = viewModelScope.launch {
        try {
            val ids = selected.value.toList()
            val learning = if (keepLearningLevels && update.status?.isLearning == true) terms.getByIds(ids).filter { it.status.isLearning }.map { it.id }.toSet() else emptySet()
            if (ids.size > learning.size) termService.applyBulkUpdate(update.copy(termIds = ids.filterNot { it in learning }))
            if (learning.isNotEmpty()) termService.applyBulkUpdate(update.copy(termIds = learning.toList(), status = null))
            selected.value = emptySet()
            message.value = trPlural(ids.size, "Updated {0} term", "Updated {0} terms")
        } catch (e: TermValidationException) {
            message.value = tr("Error: {0}", tr(e.message.orEmpty()))
        }
    }

    /**
     * Puts the selected terms, or every listed term when none is selected, into Anki: straight into
     * a running Anki through AnkiConnect when there is one (only the desktop can reach it), or else
     * as a package for the reader to save and import.
     */
    fun exportAnki() = viewModelScope.launch {
        if (exporting.value != null) return@launch
        val ids = selected.value.toList().ifEmpty { terms.list(filter.value, sort.value, 0, 1_000_000).items.map { it.id } }
        if (ids.isEmpty()) {
            message.value = tr("Nothing to export")
            return@launch
        }
        // Taken while Anki is looked for, so a second click starts nothing.
        exporting.value = tr("{0} of {1}", 0, ids.size)
        val running = ankiConnection?.takeIf { withTimeoutOrNull(ANKI_LOOKUP) { it.version() } != null }
        if (running != null) send(ids, running) else pack(ids)
    }

    private suspend fun pack(ids: List<Long>) {
        try {
            val export = anki.export(ids) { done, total -> exporting.value = tr("{0} of {1}", done, total) }
            if (export == null) message.value = alreadyInAnki(ids.size)
            else events.send(TermsListEvent.AnkiReady(export))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            message.value = tr("Could not export: {0}", e.message)
        } finally {
            exporting.value = null
        }
    }

    private suspend fun send(ids: List<Long>, connection: AnkiConnection) {
        sendingToAnki.value = true
        try {
            val sent = anki.send(ids, connection) { done, total -> exporting.value = tr("{0} of {1}", done, total) }
            message.value = if (sent == null) {
                alreadyInAnki(ids.size)
            } else {
                listOfNotNull(
                    trPlural(sent.added, "Added {0} card to Anki", "Added {0} cards to Anki"),
                    (sent.duplicates + sent.skipped).takeIf { it > 0 }?.let { tr("{0} already in Anki left out", it) },
                    sent.failed.firstOrNull()?.let { tr("Anki refused some: {0}", it) },
                ).joinToString(", ")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            message.value = tr("Could not send to Anki: {0}", e.message)
        } finally {
            exporting.value = null
            sendingToAnki.value = false
        }
    }

    private fun alreadyInAnki(count: Int) =
        if (count == 1) tr("This word is already in Anki") else trPlural(count, "All {0} words are already in Anki", "All {0} words are already in Anki")

    /** The package was saved: its words are not exported again. */
    fun ankiSaved(export: AnkiExportService.Export) = viewModelScope.launch {
        anki.markExported(export)
        val count = export.termIds.size
        val exported = trPlural(count, "Exported {0} word to Anki", "Exported {0} words to Anki")
        message.value = if (export.skipped > 0) exported + ", " + tr("{0} already in Anki left out", export.skipped) else exported
    }

    companion object {
        const val PAGE_SIZE = 50

        /** How long a running Anki has to answer before the words go into a file instead. */
        val ANKI_LOOKUP = 2.seconds

        /** The statuses ticked when the vocabulary opens: the words being learned. */
        val LEARNING_STATUSES: Set<TermStatus> = TermStatus.selectable.filter { it.isLearning }.toSet()
    }
}
