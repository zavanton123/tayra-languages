package com.tayra.languages.feature.terms.list

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
import com.tayra.languages.core.domain.service.TermImportService
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermValidationException
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.term.Csv
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
) {
    /** The single status the list is narrowed to, or null when a range is shown. */
    val statusChoice: TermStatus?
        get() = when {
            filter.includeIgnored && filter.minStatus > filter.maxStatus -> TermStatus.IGNORED
            filter.minStatus == filter.maxStatus -> filter.minStatus
            else -> null
        }

    val pageCount: Int get() = if (totalCount == 0) 1 else (totalCount + pageSize - 1) / pageSize
    fun languageName(id: Long): String = languages.firstOrNull { it.id == id }?.name ?: ""
}

sealed interface TermsListEvent {
    data class ExportReady(val csv: String) : TermsListEvent
}

class TermsListViewModel(
    initialTermIds: List<Long>?,
    private val terms: TermRepository,
    languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val termService: TermService,
) : ViewModel() {

    private val filter = MutableStateFlow(
        TermListFilter(
            languageId = settings.current.currentLanguageId.takeIf { it != 0L },
            minStatus = if (initialTermIds != null) TermStatus.UNKNOWN else TermStatus.NEW_1,
            termIds = initialTermIds,
        ),
    )
    private val sort = MutableStateFlow(TermListSort())
    private val page = MutableStateFlow(0)
    private val pageSize = MutableStateFlow(PAGE_SIZE)
    private val selected = MutableStateFlow<Set<Long>>(emptySet())
    private val message = MutableStateFlow<String?>(null)
    private val filtersVisible = MutableStateFlow(initialTermIds != null)
    val events = UiEvents<TermsListEvent>()

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
        count(TermStatus.NEW_1, TermStatus.LEARNED),
        count(TermStatus.WELL_KNOWN, TermStatus.WELL_KNOWN),
    ) { total, learning, known -> Counts(total, learning, known) }

    val state: StateFlow<TermsListUiState> = combine(
        pageFlow,
        languages.observeAll(),
        selected,
        combine(message, filtersVisible) { m, v -> m to v },
        counts,
    ) { base, languageList, sel, (msg, visible), c ->
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

    fun clearFilters() = updateFilter { TermListFilter(minStatus = TermStatus.NEW_1) }

    fun toggleFilters() { filtersVisible.value = !filtersVisible.value }

    fun setLanguage(languageId: Long?) {
        updateFilter { it.copy(languageId = languageId) }
        viewModelScope.launch { settings.update { it.copy(currentLanguageId = languageId ?: 0) } }
    }

    /** Narrows the list to one status, or shows the full learning range for null. */
    fun setStatusChoice(status: TermStatus?) = updateFilter {
        when (status) {
            null -> it.copy(minStatus = TermStatus.NEW_1, maxStatus = TermStatus.WELL_KNOWN, includeIgnored = false)
            // An empty range plus the ignored flag yields ignored terms only.
            TermStatus.IGNORED -> it.copy(minStatus = TermStatus.WELL_KNOWN, maxStatus = TermStatus.NEW_1, includeIgnored = true)
            else -> it.copy(minStatus = status, maxStatus = status, includeIgnored = false)
        }
    }

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
            termService.applyBulkUpdate(update.copy(termIds = selected.value.toList()))
            selected.value = emptySet()
            message.value = "Updated ${update.termIds.size.coerceAtLeast(selected.value.size)} term(s)"
        } catch (e: TermValidationException) {
            message.value = "Error: ${e.message}"
        }
    }

    fun exportCsv() = viewModelScope.launch {
        val all = terms.list(filter.value, sort.value, 0, 1_000_000).items
        val languageNames = state.value.languages.associate { it.id to it.name }
        val rows = listOf(TermImportService.EXPORT_HEADERS) + all.map { term ->
            listOf(
                term.displayText,
                term.parents.joinToString(", ") { it.displayText },
                term.translation.orEmpty(),
                languageNames[term.languageId].orEmpty(),
                term.status.value.toString(),
                if (term.syncStatus) "y" else "",
                term.romanization.orEmpty(),
            )
        }
        events.send(TermsListEvent.ExportReady(Csv.format(rows)))
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
