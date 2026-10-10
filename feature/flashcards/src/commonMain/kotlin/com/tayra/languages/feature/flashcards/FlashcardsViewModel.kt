package com.tayra.languages.feature.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.DueCounts
import com.tayra.languages.core.domain.flashcards.Flashcard
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** What a card shows: the same content as the cards exported to Anki. */
data class CardContent(
    val termId: Long,
    val word: String,
    /** The sentence the word was read in; null for a word saved without one. */
    val sentence: String? = null,
    /** Where the word stands in [sentence], to blank it out on the front. */
    val wordRange: IntRange? = null,
    val sentenceTranslation: String? = null,
    val translation: String = "",
    val partOfSpeech: String = "",
    val romanization: String = "",
    val languageCode: String? = null,
    val rightToLeft: Boolean = false,
    val state: CardState = CardState.NEW,
) {
    /** What is read aloud when the answer is shown. */
    val spoken: String get() = sentence ?: word
}

data class FlashcardsUiState(
    val loading: Boolean = true,
    val languageName: String = "",
    val card: CardContent? = null,
    val revealed: Boolean = false,
    val counts: DueCounts = DueCounts(),
    /** With no card to show: when the next card being learned comes back today. */
    val nextLearningAt: Instant? = null,
    val canUndo: Boolean = false,
    /** Cards answered since the screen opened. */
    val answered: Int = 0,
)

sealed interface FlashcardsEvent {
    /** Read [text] aloud; the answer was just shown. */
    data class Speak(val text: String, val languageCode: String?) : FlashcardsEvent

    /** The card on show is going away: whatever is being read aloud stops. */
    data object StopSpeaking : FlashcardsEvent
}

class FlashcardsViewModel(
    private val service: FlashcardService,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val translator: SentenceTranslator,
    private val dictionary: OfflineDictionary,
    private val words: WordTranslationService,
) : ViewModel() {

    private val _state = MutableStateFlow(FlashcardsUiState())
    val state: StateFlow<FlashcardsUiState> = _state.asStateFlow()
    val events = UiEvents<FlashcardsEvent>()

    private var current: Flashcard? = null
    private var lastAnswer: FlashcardService.Answered? = null
    private var extrasJob: Job? = null
    private var waitingJob: Job? = null

    private val languageId: Long? get() = settings.current.currentLanguageId.takeIf { it != 0L }

    init {
        load()
        // The cards follow the language chosen in the top bar.
        viewModelScope.launch {
            settings.settings.map { it.currentLanguageId }.distinctUntilChanged().drop(1).collect {
                lastAnswer = null
                _state.update { it.copy(canUndo = false) }
                load()
            }
        }
    }

    /** Shows the next card, or that there is none; [prefer] names a term whose card comes first. */
    fun load(prefer: Long? = null) {
        extrasJob?.cancel()
        waitingJob?.cancel()
        viewModelScope.launch {
            val session = service.next(languageId, prefer)
            val language = languageId?.let { languages.getById(it) } ?: session.term?.let { languages.getById(it.languageId) }
            current = session.card
            val term = session.term
            val wordRange = term?.let { service.clozeRange(it) }
            _state.update {
                it.copy(
                    loading = false,
                    languageName = language?.name.orEmpty(),
                    card = if (session.card != null && term != null) content(term, language, wordRange, session.card!!.schedule.state) else null,
                    revealed = false,
                    counts = session.counts,
                    nextLearningAt = session.nextLearningAt,
                    canUndo = lastAnswer != null,
                )
            }
            if (term != null && language != null) loadExtras(term, language)
            // Nothing to show while a card being learned waits: look again as its time nears.
            if (session.card == null && session.nextLearningAt != null) {
                waitingJob = viewModelScope.launch {
                    delay(15.seconds)
                    load()
                }
            }
        }
    }

    private fun content(term: Term, language: Language?, wordRange: IntRange?, state: CardState): CardContent {
        val word = term.displayText
        val sentence = term.sentence?.trim()?.takeIf { it.isNotEmpty() }
        return CardContent(
            termId = term.id,
            word = word,
            sentence = sentence,
            wordRange = wordRange,
            translation = term.translation.orEmpty().trim(),
            romanization = term.romanization.orEmpty().trim(),
            languageCode = language?.let { LanguageCodes.codeFor(it.name) },
            rightToLeft = language?.rightToLeft == true,
            state = state,
        )
    }

    /** The sentence's translation, the part of speech and, for a term saved without one, a translation: looked up after the card is on screen. */
    private fun loadExtras(term: Term, language: Language) {
        val sentence = term.sentence?.trim()?.takeIf { it.isNotEmpty() }
        val code = LanguageCodes.codeFor(language.name)
        fun updateCard(change: (CardContent) -> CardContent) = _state.update { s -> if (s.card?.termId == term.id) s.copy(card = change(s.card)) else s }
        extrasJob = viewModelScope.launch {
            launch {
                val translated = sentence?.let { attempt { translator.translate(it, language) } }
                if (!translated.isNullOrBlank()) updateCard { it.copy(sentenceTranslation = translated) }
            }
            launch {
                val native = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }
                val pos = attempt {
                    val pack = DictionaryPacks.find(code, native)?.id ?: return@attempt null
                    if (!dictionary.isAvailable(pack)) return@attempt null
                    dictionary.lookup(pack, term.displayText).entries.firstOrNull()?.pos
                }
                if (!pos.isNullOrBlank()) updateCard { it.copy(partOfSpeech = pos.replaceFirstChar { c -> c.uppercase() }) }
            }
            if (term.translation.isNullOrBlank()) launch {
                val found = attempt { words.translate(language, term.displayText) }
                if (!found.isNullOrBlank()) updateCard { it.copy(translation = found) }
            }
        }
    }

    private suspend fun <T> attempt(block: suspend () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    fun reveal() {
        val card = _state.value.card ?: return
        if (_state.value.revealed) return
        _state.update { it.copy(revealed = true) }
        if (settings.current.flashcardAutoplay) events.trySend(FlashcardsEvent.Speak(card.spoken, card.languageCode))
    }

    fun answer(rating: Rating) {
        val card = current ?: return
        if (!_state.value.revealed) return
        current = null
        events.trySend(FlashcardsEvent.StopSpeaking)
        viewModelScope.launch {
            lastAnswer = service.answer(card, rating)
            _state.update { it.copy(answered = it.answered + 1) }
            load()
        }
    }

    /** Takes the last answer back and shows that card again. */
    fun undo() {
        val answered = lastAnswer ?: return
        lastAnswer = null
        events.trySend(FlashcardsEvent.StopSpeaking)
        viewModelScope.launch {
            service.undo(answered)
            _state.update { it.copy(answered = (it.answered - 1).coerceAtLeast(0)) }
            load(prefer = answered.before.termId)
        }
    }

    /** Puts the card on show aside until it is resumed from its term. */
    fun suspendCard() {
        val card = current ?: return
        current = null
        events.trySend(FlashcardsEvent.StopSpeaking)
        viewModelScope.launch {
            service.setSuspended(card.termId, true)
            load()
        }
    }
}
