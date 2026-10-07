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
) {
    /** The statuses ticked in the filter panel; none means every learning status and known. */
    val chosenStatuses: Set<TermStatus> get() = filter.statuses.orEmpty()

    val ageFiltered: Boolean get() = filter.minAgeDays != null || filter.maxAgeDays != null

    /** How many kinds of filter are on, for the badge on the Filters button. */
    val activeFilterCount: Int get() = listOf(chosenStatuses.isNotEmpty(), ageFiltered).count { it }

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
) : ViewModel() {

    private val filter = MutableStateFlow(
        TermListFilter(
            // A list of given terms shows them whatever their language; otherwise the language being learned.
            languageId = settings.current.currentLanguageId.takeIf { it != 0L && initialTermIds == null },
            minStatus = if (initialTermIds != null) TermStatus.UNKNOWN else TermStatus.NEW_1,
            termIds = initialTermIds,
        ),
    )
    private val sort = MutableStateFlow(TermListSort())
    private val page = MutableStateFlow(0)
    private val pageSize = MutableStateFlow(PAGE_SIZE)
    private val selected = MutableStateFlow<Set<Long>>(emptySet())
    private val message = MutableStateFlow<String?>(null)
    private val exporting = MutableStateFlow<String?>(null)
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
        combine(message, filtersVisible, exporting) { m, v, e -> Triple(m, v, e) },
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
            exporting = export,
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

    fun clearFilters() = updateFilter { TermListFilter(languageId = it.languageId, minStatus = TermStatus.NEW_1) }

    fun toggleFilters() { filtersVisible.value = !filtersVisible.value }

    /** Ticks or unticks a status; with none ticked the list shows every status but ignored. */
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

    fun applyBulkUpdate(update: BulkTermUpdate) = viewModelScope.launch {
        try {
            val ids = selected.value.toList()
            termService.applyBulkUpdate(update.copy(termIds = ids))
            selected.value = emptySet()
            message.value = trPlural(ids.size, "Updated {0} term", "Updated {0} terms")
        } catch (e: TermValidationException) {
            message.value = tr("Error: {0}", tr(e.message.orEmpty()))
        }
    }

    /** Packages the selected terms, or every listed term when none is selected, for Anki. */
    fun exportAnki() = viewModelScope.launch {
        if (exporting.value != null) return@launch
        val ids = selected.value.toList().ifEmpty { terms.list(filter.value, sort.value, 0, 1_000_000).items.map { it.id } }
        if (ids.isEmpty()) {
            message.value = tr("Nothing to export")
            return@launch
        }
        exporting.value = tr("{0} of {1}", 0, ids.size)
        try {
            val export = anki.export(ids) { done, total -> exporting.value = tr("{0} of {1}", done, total) }
            if (export == null) message.value = if (ids.size == 1) tr("This word is already in Anki") else trPlural(ids.size, "All {0} words are already in Anki", "All {0} words are already in Anki")
            else events.send(TermsListEvent.AnkiReady(export))
        } catch (e: Exception) {
            message.value = tr("Could not export: {0}", e.message)
        } finally {
            exporting.value = null
        }
    }

    /** The package was saved: its words are not exported again. */
    fun ankiSaved(export: AnkiExportService.Export) = viewModelScope.launch {
        anki.markExported(export)
        val count = export.termIds.size
        val exported = trPlural(count, "Exported {0} word to Anki", "Exported {0} words to Anki")
        message.value = if (export.skipped > 0) exported + ", " + tr("{0} already in Anki left out", export.skipped) else exported
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
