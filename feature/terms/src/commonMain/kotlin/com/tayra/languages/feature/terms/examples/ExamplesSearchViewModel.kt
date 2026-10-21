package com.tayra.languages.feature.terms.examples

import com.tayra.languages.core.ui.i18n.tr
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.CorpusWords
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.ExampleSort
import com.tayra.languages.core.domain.service.ExampleSource
import com.tayra.languages.core.domain.service.ExampleSourceFeatures
import com.tayra.languages.core.domain.service.LocalSentenceTranslator
import com.tayra.languages.core.domain.service.SentenceTranslation
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.domain.service.effectiveEngine
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.theme.ReadingFont
import kotlin.math.roundToInt

data class ExamplesSearchUiState(
    val loading: Boolean = true,
    val language: Language? = null,
    val query: ExampleSearchQuery? = null,
    val results: List<ExampleSentence> = emptyList(),
    val total: Int? = null,
    val nextPage: String? = null,
    val searching: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    /** The term shown in the term pane: the searched one, or a word clicked in an example; null once closed. */
    val paneTerm: String? = null,
    /** The example the pane's term was clicked in, if any. */
    val paneSentence: String? = null,
    /** The words of each result with their statuses, by the result's text. */
    val words: Map<String, List<WordStatus>> = emptyMap(),
    /** Counts the statuses set on the results' words, so the pane can read its term again. */
    val termsChanged: Int = 0,
    /** The sources with examples in the language, the default first; Tatoeba alone until the others have answered. */
    val sources: List<ExampleSourceFeatures> = listOf(ExampleSourceFeatures.TATOEBA),
    /** Machine translations of the results, for a source that has none of its own, by the result's text. */
    val translations: Map<String, SentenceTranslation> = emptyMap(),
    /** Words often found near the searched one, each a search of its own. */
    val collocations: List<String> = emptyList(),
) {
    val hasMore: Boolean get() = nextPage != null

    /** What the chosen source can do. */
    val features: ExampleSourceFeatures
        get() = sources.firstOrNull { it.source == query?.source } ?: ExampleSourceFeatures.TATOEBA

    /** The other sources to try when Tatoeba found nothing: those that can search what was typed. */
    val fallbackSources: List<ExampleSource>
        get() {
            val text = query?.text?.trim().orEmpty()
            if (query?.source != ExampleSource.TATOEBA || searching || results.isNotEmpty() || text.isEmpty()) return emptyList()
            return sources.filter { it.source != ExampleSource.TATOEBA && (it.phrases || text.none(Char::isWhitespace)) }.map { it.source }
        }
}

class ExamplesSearchViewModel(
    private val languageId: Long,
    private val initialText: String,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val provider: ExampleSentencesProvider,
    /** Lets words of the results be saved; without it the results are only shown. */
    private val exampleTerms: ExampleTerms? = null,
    /** Translates the results of a source without translations; without it they stay untranslated. */
    private val translator: SentenceTranslator? = null,
    private val localTranslator: LocalSentenceTranslator? = null,
    /** The words found near the searched one, where the corpus can be reached. */
    private val corpusWords: CorpusWords? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(ExamplesSearchUiState())
    val state: StateFlow<ExamplesSearchUiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    private var filterJob: Job? = null
    private var collocationsJob: Job? = null
    /** The results' translations, dropped by the next search. */
    private val translations = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))
    /** Whether the on-device translator could be readied: asked once, and again after a failure by the next search. */
    private var localReady: Boolean? = null
    private val localLock = Mutex()

    init {
        viewModelScope.launch {
            val language = languages.getById(languageId)
            if (language == null) {
                _state.update { it.copy(loading = false, error = tr("Language not found")) }
                return@launch
            }
            val query = defaultFilters(ExampleSearchQuery(text = initialText, language = language, targetLanguage = settings.current.nativeLanguage.ifBlank { "en" }))
            _state.update { it.copy(loading = false, language = language, query = query, paneTerm = initialText.trim().ifEmpty { null }) }
            search()
            // Leipzig's corpus list may take a few seconds; Tatoeba's results do not wait for it.
            launch {
                val sources = provider.sources(language)
                if (sources.isNotEmpty()) _state.update { it.copy(sources = sources) }
            }
        }
    }

    fun updateQuery(transform: (ExampleSearchQuery) -> ExampleSearchQuery) {
        _state.update { s -> s.query?.let { s.copy(query = transform(it)) } ?: s }
    }

    /** Applies a filter change and re-runs the search after a short pause, so typed numbers settle first. */
    fun updateFilters(transform: (ExampleSearchQuery) -> ExampleSearchQuery) {
        updateQuery(transform)
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(FILTER_DEBOUNCE_MS)
            search()
        }
    }

    fun resetFilters() {
        updateQuery(::defaultFilters)
        search()
    }

    private fun defaultFilters(query: ExampleSearchQuery) =
        query.copy(minWords = 1, maxWords = 6, sort = ExampleSort.RANDOM, limit = 10, hasAudio = null)

    /** Searches [source] instead, without the filters it lacks. */
    fun setSource(source: ExampleSource) {
        val s = _state.value
        val query = s.query ?: return
        if (query.source == source) return
        val features = s.sources.firstOrNull { it.source == source } ?: return
        updateQuery {
            it.copy(
                source = source,
                sort = if (it.sort in features.sorts) it.sort else features.sorts.firstOrNull() ?: ExampleSort.RANDOM,
                hasAudio = if (features.audio) it.hasAudio else null,
            )
        }
        search()
    }

    /** Searches [word] in the same source and filters, as a collocation chip does. */
    fun searchFor(word: String) {
        updateQuery { it.copy(text = word) }
        search()
    }

    fun search() {
        val query = _state.value.query ?: return
        if (query.text.isBlank()) return
        filterJob?.cancel()
        searchJob?.cancel()
        collocationsJob?.cancel()
        translations.coroutineContext.cancelChildren()
        if (localReady == false) localReady = null
        val keepCollocations = query.source == ExampleSource.LEIPZIG && collocationsFor == query.text.trim()
        if (!keepCollocations) collocationsFor = null
        _state.update {
            it.copy(
                searching = true, error = null, results = emptyList(), total = null, nextPage = null, paneTerm = query.text.trim(), paneSentence = null,
                translations = it.translations.filterValues { t -> t is SentenceTranslation.Done },
                collocations = if (keepCollocations) it.collocations else emptyList(),
            )
        }
        searchJob = viewModelScope.launch {
            val result = provider.search(query)
            _state.update { it.copy(searching = false, results = result.sentences, total = result.total, nextPage = result.nextPage) }
            loadLearning()
        }
        if (query.source == ExampleSource.LEIPZIG && !keepCollocations) loadCollocations(query)
    }

    /** The word the shown collocations are for. */
    private var collocationsFor: String? = null

    private fun loadCollocations(query: ExampleSearchQuery) {
        val words = corpusWords ?: return
        val text = query.text.trim()
        collocationsJob = viewModelScope.launch {
            val found = words.cooccurrences(text, query.language, COLLOCATIONS)
            collocationsFor = text
            _state.update { it.copy(collocations = found) }
        }
    }

    /**
     * Translates a result of a source without translations, once; the screen asks as each one
     * comes into view. The on-device translator is readied first, as the reader does.
     */
    fun translate(sentence: String) {
        val translator = translator ?: return
        val s = _state.value
        val language = s.language ?: return
        if (s.features.translations || sentence in s.translations) return
        _state.update { it.copy(translations = it.translations + (sentence to SentenceTranslation.Loading)) }
        val prefs = settings.current
        val engine = prefs.effectiveEngine(localTranslator != null)
        translations.launch {
            val ready = engine != TranslationEngine.ARGOS || localTranslator == null || prepareLocal(language, prefs.nativeLanguage)
            val result = if (ready) translator.translate(sentence, language) else null
            _state.update {
                if (it.translations[sentence] != SentenceTranslation.Loading) it
                else it.copy(translations = it.translations + (sentence to (result?.let { t -> SentenceTranslation.Done(t, engine) } ?: SentenceTranslation.Unavailable)))
            }
        }
    }

    private suspend fun prepareLocal(language: Language, nativeLanguage: String): Boolean = localLock.withLock {
        val local = localTranslator ?: return true
        localReady?.let { return it }
        val source = LanguageCodes.codeFor(language.name)
        val target = LanguageCatalog.nativeOption(nativeLanguage)
        val ready = source != null && try {
            local.prepare(source, target.code, language.name, target.name)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
        ready.also { localReady = it }
    }

    /** Looks the saved terms of the results up again, after one of them may have changed. */
    fun refreshTerms() {
        viewModelScope.launch { loadLearning() }
    }

    private var shown = false

    /** The screen came back to the front: terms may have changed on a screen opened from here. The first time, it is still loading. */
    fun onResumed() {
        if (shown && !_state.value.loading) refreshTerms()
        shown = true
    }

    // ---- the examples' text, set as the reader's (one setting for both)

    fun setReadingFont(font: ReadingFont) = updateSettings { it.copy(readingFont = font.id) }
    fun setFontScale(scale: Float) = updateSettings { it.copy(readingFontScale = ((scale * 10).roundToInt() / 10f).coerceIn(0.6f, 2.5f)) }
    fun setLineHeight(height: Float) = updateSettings { it.copy(readingLineHeight = ((height * 10).roundToInt() / 10f).coerceIn(1.0f, 3.0f)) }

    private fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    /** A word of a result changed status: the results are recoloured and the pane told, since its term or family may be that word. */
    private suspend fun wordChanged() {
        loadLearning()
        _state.update { it.copy(termsChanged = it.termsChanged + 1) }
    }

    private suspend fun loadLearning() {
        val helper = exampleTerms ?: return
        val current = _state.value
        val language = current.language ?: return
        val found = helper.statuses(current.results.map { it.text }, language)
        _state.update { it.copy(words = found) }
    }

    /** The status shortcuts, as the reader uses them. */
    val hotkeys: Map<HotkeyAction, Hotkey?> get() = settings.current.hotkeys

    /** A status shortcut on a word of a result. */
    fun setStatus(word: String, sentence: String?, status: TermStatus) {
        val helper = exampleTerms ?: return
        val language = _state.value.language ?: return
        viewModelScope.launch {
            helper.setStatus(word, sentence, language, status)
            wordChanged()
        }
    }

    fun shiftStatus(word: String, sentence: String?, delta: Int) {
        val helper = exampleTerms ?: return
        val language = _state.value.language ?: return
        viewModelScope.launch {
            helper.shiftStatus(word, sentence, language, delta)
            wordChanged()
        }
    }

    /** A right click on a word of a result: saved as the reader does, with the result as its sentence. */
    fun markWord(word: String, sentence: String) {
        val helper = exampleTerms ?: return
        val language = _state.value.language ?: return
        viewModelScope.launch {
            helper.toggle(word, sentence, language)
            wordChanged()
        }
    }

    /** Shows the pane for [word]; [sentence] is the example it was clicked in. */
    fun openTerm(word: String, sentence: String? = null) {
        word.trim().takeIf { it.isNotEmpty() }?.let { text -> _state.update { it.copy(paneTerm = text, paneSentence = sentence) } }
    }

    fun closePane() = _state.update { it.copy(paneTerm = null, paneSentence = null) }

    fun loadMore() {
        val s = _state.value
        val next = s.nextPage ?: return
        if (s.loadingMore || s.searching) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            val result = provider.nextPage(next, s.query?.targetLanguage ?: "en")
            _state.update { it.copy(loadingMore = false, results = it.results + result.sentences, nextPage = result.nextPage) }
            loadLearning()
        }
    }

    private companion object {
        const val FILTER_DEBOUNCE_MS = 400L
        const val COLLOCATIONS = 8
    }
}
