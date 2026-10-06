package com.tayra.languages.feature.frequency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.frequency.FrequencyBand
import com.tayra.languages.core.domain.frequency.RankedWord
import com.tayra.languages.core.domain.frequency.WordFrequencyOverview
import com.tayra.languages.core.domain.frequency.WordFrequencyService
import com.tayra.languages.core.domain.frequency.WordKnowledge
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class WordFrequencyUiState(
    val loading: Boolean = true,
    val languageId: Long = 0,
    val languageName: String = "",
    /** Null when the app has no frequency list for the language. */
    val overview: WordFrequencyOverview? = null,
    val shown: Set<WordKnowledge> = WordKnowledge.entries.toSet(),
    val search: String = "",
    /** The word open in the term panel. */
    val selected: String? = null,
) {
    /** The bands with the words the filters and the search keep; bands left empty are dropped. */
    val visibleBands: List<FrequencyBand>
        get() {
            val bands = overview?.bands ?: return emptyList()
            val query = search.trim().lowercase()
            if (query.isEmpty() && shown.size == WordKnowledge.entries.size) return bands
            return bands.mapNotNull { band ->
                band.copy(words = band.words.filter { it.matches(query) }).takeIf { it.words.isNotEmpty() }
            }
        }

    private fun RankedWord.matches(query: String): Boolean =
        knowledge in shown && (query.isEmpty() || word.word.startsWith(query) || word.forms.any { it.startsWith(query) })
}

/** The most common words of the language being learned, with how far the reader has got with each. */
class WordFrequencyViewModel(
    service: WordFrequencyService,
    private val languages: LanguageRepository,
    settings: SettingsRepository,
    private val translations: WordTranslationService,
) : ViewModel() {
    private val shown = MutableStateFlow(WordKnowledge.entries.toSet())
    private val search = MutableStateFlow("")
    private val selected = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val overview = settings.settings.map { it.currentLanguageId }.distinctUntilChanged()
        .flatMapLatest { id -> service.observe(id).map { id to it } }

    val state: StateFlow<WordFrequencyUiState> = combine(overview, shown, search, selected) { (id, list), kinds, query, word ->
        WordFrequencyUiState(
            loading = false,
            languageId = id,
            languageName = list?.languageName ?: languages.getById(id)?.name.orEmpty(),
            overview = list,
            shown = kinds,
            search = query,
            selected = word,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordFrequencyUiState())

    /** Shows or hides the words of [knowledge]; hiding the last one shown shows them all again. */
    fun toggle(knowledge: WordKnowledge) {
        shown.value = (if (knowledge in shown.value) shown.value - knowledge else shown.value + knowledge).ifEmpty { WordKnowledge.entries.toSet() }
    }

    fun setSearch(query: String) { search.value = query }
    fun select(word: String?) { selected.value = word }

    /** A short translation of [word] for its tooltip. */
    suspend fun translate(word: String): String? {
        val language = languages.getById(state.value.languageId) ?: return null
        return translations.translate(language, word)
    }
}
