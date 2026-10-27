package com.tayra.languages.feature.flashcards

import kotlinx.coroutines.flow.first
import com.tayra.languages.core.domain.repository.BookRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.dictionary.OnlineWiktionary
import com.tayra.languages.core.domain.dictionary.WiktionaryCodes
import com.tayra.languages.core.domain.dictionary.WordHint
import com.tayra.languages.core.domain.dictionary.WordHints
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.DueCounts
import com.tayra.languages.core.domain.flashcards.Flashcard
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SentenceAudioState
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
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
import kotlin.time.Duration
import kotlin.time.Instant

/** What a card shows: the same content as the cards exported to Anki. */
data class CardContent(
    val termId: Long,
    val languageId: Long = 0,
    val word: String,
    /** The sentence the word was read in; null for a word saved without one. */
    val sentence: String? = null,
    /** Where the word stands in [sentence], to blank it out on the front. */
    val wordRange: IntRange? = null,
    val sentenceTranslation: String? = null,
    /** The sentence's translation is still being looked up; its place on the card is kept meanwhile. */
    val translating: Boolean = false,
    val translation: String = "",
    val partOfSpeech: String = "",
    val romanization: String = "",
    val languageCode: String? = null,
    val rightToLeft: Boolean = false,
    val state: CardState = CardState.NEW,
    /** Wiktionary's gender, plural and key verb forms of the word, for the back; empty until they come, or offline. */
    val hints: List<WordHint> = emptyList(),
    /** The Wiktionary entry [hints] come from, linked beside them as their licence asks. */
    val hintsSource: String? = null,
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
    /** Whether an answered card lies behind the one on show, to go back to. */
    val canUndo: Boolean = false,
    /** Cards answered since the screen opened. */
    val answered: Int = 0,
    /** Only Again and Good are offered, as the settings ask. */
    val twoButtons: Boolean = false,
    /** How long each answer would put the card away for, shown on its button. */
    val waits: Map<Rating, CardWait> = emptyMap(),
    /** With no card to show: the text read last and not finished, a book or a lesson, to go back to. */
    val reading: ReadingToContinue? = null,
    /** The session's speech still being made before its first card, so that no card waits for its sound; null once it may start. */
    val preparingAudio: AudioPreparation? = null,
)

/** How much of the speech the session waits for is made: [ready] of [total], which is 0 while that is still being found out. */
data class AudioPreparation(val ready: Int, val total: Int)

/** A text to go back to after the cards: its id and title. */
data class ReadingToContinue(val bookId: Long, val title: String)

/** How long until a card comes back: in [days] once it is a review, else as a [duration] in its steps. */
data class CardWait(val days: Int?, val duration: Duration)

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
    /** Speech made ahead of time; null where there is none to make, as in tests. */
    private val audio: SentenceAudio? = null,
    /** Wiktionary online, for the hints on the back of a card. */
    private val wiktionary: OnlineWiktionary? = null,
    /** The texts, for the one to go on reading once the cards are done; null leaves that out. */
    private val books: BookRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(FlashcardsUiState())
    val state: StateFlow<FlashcardsUiState> = _state.asStateFlow()
    val events = UiEvents<FlashcardsEvent>()

    private var current: Flashcard? = null
    /** The answers given this session, the latest last; going back takes them back one at a time. */
    private val answers = ArrayDeque<FlashcardService.Answered>()
    private var extrasJob: Job? = null
    private var waitingJob: Job? = null
    private var speechJob: Job? = null

    private val languageId: Long? get() = settings.current.currentLanguageId.takeIf { it != 0L }

    val hotkeys: Map<HotkeyAction, Hotkey?> get() = settings.current.hotkeys

    init {
        // Until it is known whether any speech is missing, no card shows, so none flashes up before the wait.
        if (audio != null) _state.update { it.copy(preparingAudio = AudioPreparation(0, 0)) }
        load()
        prepareSpeech()
        // The cards follow the language chosen in the top bar.
        viewModelScope.launch {
            settings.settings.map { it.currentLanguageId }.distinctUntilChanged().drop(1).collect {
                answers.clear()
                _state.update { it.copy(canUndo = false, preparingAudio = if (audio != null) AudioPreparation(0, 0) else null) }
                load()
                prepareSpeech()
            }
        }
    }

    /**
     * Has the local speech engine make the audio of every card in the session, one after another in
     * the order they come up: the sentences read when an answer is shown first, then the words. The
     * session waits for what is not made yet, with its progress shown, so that no card waits for its
     * sound; [startWithoutWaiting] lets it start sooner, the rest made while the cards are reviewed.
     * Nothing is made for the system voice.
     */
    private fun prepareSpeech() {
        val audio = audio ?: return
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            val terms = service.sessionTerms(languageId)
            val groups = terms.groupBy { it.languageId }.mapNotNull { (id, group) ->
                val code = languages.getById(id)?.let { LanguageCodes.codeFor(it.name) } ?: return@mapNotNull null
                val spoken = group.map { term -> term.sentence?.trim()?.takeIf { it.isNotEmpty() } ?: term.displayText }
                code to spoken + group.map { it.displayText }
            }
            val missing = groups.flatMap { (code, texts) -> audio.missing(texts, code) }.toSet()
            _state.update { it.copy(preparingAudio = if (missing.isEmpty()) null else AudioPreparation(0, missing.size)) }
            val progress = launch {
                audio.states.collect { states ->
                    val ready = missing.count { states[it] == SentenceAudioState.READY }
                    _state.update { s -> s.preparingAudio?.let { s.copy(preparingAudio = it.copy(ready = ready)) } ?: s }
                }
            }
            try {
                groups.forEach { (code, texts) -> audio.prepare(texts, code) }
            } finally {
                progress.cancel()
                _state.update { it.copy(preparingAudio = null) }
            }
        }
    }

    /** Starts the session before its speech is all made; the rest is made while the cards are reviewed. */
    fun startWithoutWaiting() {
        _state.update { it.copy(preparingAudio = null) }
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
            val reading = if (session.card == null) lastReading() else null
            _state.update {
                it.copy(
                    loading = false,
                    languageName = language?.name.orEmpty(),
                    card = if (session.card != null && term != null) content(term, language, wordRange, session.card!!.schedule.state) else null,
                    revealed = false,
                    counts = session.counts,
                    nextLearningAt = session.nextLearningAt,
                    canUndo = answers.isNotEmpty(),
                    twoButtons = settings.current.flashcardTwoButtons,
                    waits = session.outcomes.mapValues { (_, after) -> CardWait(after.intervalDays.takeIf { after.state.isReview }, after.due - session.shownAt) },
                    reading = reading,
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
            languageId = term.languageId,
            word = word,
            sentence = sentence,
            translating = sentence != null,
            wordRange = wordRange,
            translation = term.translation.orEmpty().trim(),
            romanization = term.romanization.orEmpty().trim(),
            languageCode = language?.let { LanguageCodes.codeFor(it.name) },
            rightToLeft = language?.rightToLeft == true,
            state = state,
        )
    }

    /**
     * The sentence's translation, the part of speech, Wiktionary's hints and, for a term saved without
     * one, a translation: looked up after the card is on screen, which never waits for them.
     */
    /** The text of the language read most recently and not finished: a book of the library or a course lesson. */
    private suspend fun lastReading(): ReadingToContinue? {
        val repository = books ?: return null
        val id = languageId ?: return null
        val library = repository.observeBooks(archived = false).first()
            .filter { it.languageId == id && it.lastOpened != null && !it.isCompleted }
            .map { Triple(it.id, it.title, it.lastOpened!!) }
        // Lesson texts are kept out of the library list; their language and title are their book's.
        val lessons = repository.observeLessonBooks().first().values
            .filter { it.lastOpened != null && !it.isCompleted }
            .mapNotNull { lesson -> repository.getBook(lesson.bookId)?.takeIf { it.languageId == id }?.let { Triple(lesson.bookId, it.title, lesson.lastOpened!!) } }
        return (library + lessons).maxByOrNull { it.third }?.let { ReadingToContinue(it.first, it.second) }
    }

    private fun loadExtras(term: Term, language: Language) {
        val sentence = term.sentence?.trim()?.takeIf { it.isNotEmpty() }
        val code = LanguageCodes.codeFor(language.name)
        fun updateCard(change: (CardContent) -> CardContent) = _state.update { s -> if (s.card?.termId == term.id) s.copy(card = change(s.card)) else s }
        extrasJob = viewModelScope.launch {
            launch {
                val translated = sentence?.let { attempt { translator.translate(it, language) } }
                // An example chosen for the card meanwhile has a translation of its own.
                updateCard { if (it.sentence == sentence) it.copy(sentenceTranslation = translated?.takeIf(String::isNotBlank), translating = false) else it }
                translateAhead(term, language)
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
            if (wiktionary != null && code != null) launch {
                val native = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }
                val page = attempt { wiktionary.page(term.displayText, code, native) } ?: return@launch
                val hints = WordHints.of(page)
                if (hints.isNotEmpty()) updateCard { it.copy(hints = hints, hintsSource = WiktionaryCodes.entryUrl(page, language.name)) }
            }
            if (term.translation.isNullOrBlank()) launch {
                val found = attempt { words.translate(language, term.displayText) }
                if (!found.isNullOrBlank()) updateCard { it.copy(translation = found) }
            }
        }
    }

    /**
     * Looks up the translations of the sentences of the cards coming up after [term]'s, a few at a
     * time, so that theirs are at hand when they come; the translator keeps them.
     */
    private suspend fun translateAhead(term: Term, language: Language) {
        val session = service.sessionTerms(languageId)
        val after = session.indexOfFirst { it.id == term.id }.takeIf { it >= 0 } ?: return
        session.drop(after + 1).filter { it.languageId == language.id }
            .mapNotNull { it.sentence?.trim()?.takeIf(String::isNotEmpty) }
            .take(TRANSLATE_AHEAD)
            .forEach { attempt { translator.translate(it, language) } }
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
            service.answer(card, rating)?.let { answers.addLast(it) }
            _state.update { it.copy(answered = it.answered + 1) }
            load()
        }
    }

    /**
     * Gives the card on show [sentence], an example chosen for it, with its [translation], or a
     * machine one when it has none; the card stays answered and its schedule as it was.
     */
    fun replaceSentence(sentence: String, translation: String?) {
        val card = _state.value.card ?: return
        val text = sentence.trim()
        if (text.isEmpty() || text == card.sentence) return
        fun updateCard(change: (CardContent) -> CardContent) = _state.update { s -> if (s.card?.termId == card.termId) s.copy(card = change(s.card)) else s }
        viewModelScope.launch {
            val range = service.replaceSentence(card.termId, text)
            val given = translation?.takeIf(String::isNotBlank)
            updateCard { it.copy(sentence = text, wordRange = range, sentenceTranslation = given, translating = given == null) }
            audio?.let { launch { attempt { it.prepare(listOf(text), card.languageCode) } } }
            if (translation.isNullOrBlank()) {
                val translated = languages.getById(card.languageId)?.let { language -> attempt { translator.translate(text, language) } }
                updateCard { if (it.sentence == text) it.copy(sentenceTranslation = translated?.takeIf(String::isNotBlank), translating = false) else it }
            }
        }
    }

    /** Goes back to the previous card: its answer is taken back and it is shown again, to answer anew. */
    fun undo() {
        val answered = answers.removeLastOrNull() ?: return
        events.trySend(FlashcardsEvent.StopSpeaking)
        viewModelScope.launch {
            service.undo(answered)
            _state.update { it.copy(answered = (it.answered - 1).coerceAtLeast(0)) }
            load(prefer = answered.before.termId)
        }
    }

    /** Deletes the card on show with its word; answers given to it this session can no longer be taken back. */
    fun deleteCard() {
        val card = current ?: return
        current = null
        events.trySend(FlashcardsEvent.StopSpeaking)
        answers.removeAll { it.before.termId == card.termId }
        viewModelScope.launch {
            service.delete(listOf(card.termId))
            load()
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

/** How many cards ahead have their sentences translated before they come. */
private const val TRANSLATE_AHEAD = 3
