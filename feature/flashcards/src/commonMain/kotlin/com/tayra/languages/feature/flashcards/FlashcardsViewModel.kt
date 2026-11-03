package com.tayra.languages.feature.flashcards

import kotlinx.coroutines.flow.first
import com.tayra.languages.core.domain.repository.BookRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.flashcards.AnswerMode
import com.tayra.languages.core.domain.flashcards.BacklogOrder
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.DueCounts
import com.tayra.languages.core.domain.flashcards.ExtraStudy
import com.tayra.languages.core.domain.flashcards.Flashcard
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.flashcards.SentenceTranslations
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.practice.PracticeChecker
import com.tayra.languages.core.domain.practice.SpeechRecognition
import com.tayra.languages.core.domain.practice.SpeechRetry
import com.tayra.languages.core.domain.practice.SpokenAnswer
import com.tayra.languages.core.domain.practice.listenForAnswer
import com.tayra.languages.core.domain.practice.TypedResult
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
import kotlin.time.TimeSource

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
) {
    /** What is read aloud when the answer is shown. */
    val spoken: String get() = sentence ?: word

    /** What fills the blank, as the sentence has it; null for a card without a sentence, whose front shows the word itself. */
    val hidden: String? get() = sentence?.let { s -> wordRange?.let { s.substring(it) } ?: word }
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
    /** How cards are answered, as the settings ask. */
    val answerMode: AnswerMode = AnswerMode.SHOW,
    /** The ways of answering this device offers: speaking where it can hear speech at all. */
    val modes: List<AnswerMode> = listOf(AnswerMode.SHOW, AnswerMode.TYPE),
    /** Whether speech in the card's language can be heard now; null until that is known. */
    val speakReady: Boolean? = null,
    /** The microphone could not be used this session, or the card on show is typed instead: words are typed. */
    val speakingRefused: Boolean = false,
    val typeInstead: Boolean = false,
    val listening: Boolean = false,
    /** What was said is being made out. */
    val recognising: Boolean = false,
    /** Why the sentence is to be said again, when nothing came of the last try. */
    val retry: SpeechRetry? = null,
    /** What was heard for the card on show. */
    val heard: String = "",
    /** Whether the hidden word was heard; null until something was. */
    val spokenResult: TypedResult? = null,
    /** What was typed for the card on show. */
    val typed: String = "",
    /** How what was typed compares with the hidden word; null until it is checked. */
    val typedResult: TypedResult? = null,
    /** The answer the check suggests, which Enter gives; null when the answer was only shown. */
    val suggested: Rating? = null,
    /** The extra pass being gone through past the day's limits; null for the day's own cards. */
    val pass: ExtraPass? = null,
    /** The order today's reviews take as far more are due than usual; null on a day like any other. */
    val backlog: BacklogOrder? = null,
    /** The card the last answer made a leech; null when it made none. */
    val leech: LeechNote? = null,
) {
    /**
     * How the card on show is answered: by showing it when it has no blank to fill, and by typing
     * the word when it is to be said but cannot be heard.
     */
    val cardMode: AnswerMode
        get() = when {
            card?.hidden == null -> AnswerMode.SHOW
            answerMode == AnswerMode.SPEAK && (speakReady == false || speakingRefused || typeInstead) -> AnswerMode.TYPE
            else -> answerMode
        }

    /** The card's word is to be said and has not been yet. */
    val speaking: Boolean get() = cardMode == AnswerMode.SPEAK && !revealed && card != null && preparingAudio == null

    /** The hidden word is being typed: the keys are the field's. */
    val typing: Boolean get() = cardMode == AnswerMode.TYPE && !revealed && card != null && preparingAudio == null
}

/** A card that just became a leech: its word, the times it was forgotten, and whether it was suspended for it. */
data class LeechNote(val word: String, val lapses: Int, val suspended: Boolean)

/** An extra pass past the day's limits: what it goes over, its cards in order, and how many are behind. */
data class ExtraPass(val study: ExtraStudy, val termIds: List<Long>, val done: Int = 0) {
    val current: Long? get() = termIds.getOrNull(done)
}

/** How much of the speech the session waits for is made: [ready] of [total], which is 0 while that is still being found out. */
data class AudioPreparation(val ready: Int, val total: Int)

/** A text to go back to after the cards, by its id. */
data class ReadingToContinue(val bookId: Long)

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
    /** The texts, for the one to go on reading once the cards are done; null leaves that out. */
    private val books: BookRepository? = null,
    /** Keeps the sentences' translations with their words; null translates them each time, as in tests. */
    private val sentences: SentenceTranslations? = null,
    /** Hears the sentence said aloud; null where nothing can, and the cards cannot be answered by speaking. */
    private val speech: SpeechRecognition? = null,
) : ViewModel(), StudyMoreCounts {

    private val _state = MutableStateFlow(FlashcardsUiState())
    val state: StateFlow<FlashcardsUiState> = _state.asStateFlow()
    val events = UiEvents<FlashcardsEvent>()

    private var current: Flashcard? = null
    /** When the card on show came up, to time the answer to it. */
    private var shownAt: TimeSource.Monotonic.ValueTimeMark? = null

    /** The extra pass under way, kept here as well as in the state so a load reads the one it acts on. */
    private var pass: ExtraPass? = null

    /** The answers given this session, the latest last; going back takes them back one at a time. */
    private val answers = ArrayDeque<FlashcardService.Answered>()
    private var extrasJob: Job? = null
    private var waitingJob: Job? = null
    private var speechJob: Job? = null
    private var listenJob: Job? = null
    /** Whether each language can be heard, found out once a session, as finding out can take a moment. */
    private val hearable = mutableMapOf<String, Boolean>()

    private val languageId: Long? get() = settings.current.currentLanguageId.takeIf { it != 0L }

    val hotkeys: Map<HotkeyAction, Hotkey?> get() = settings.current.hotkeys

    init {
        // Until it is known whether any speech is missing, no card shows, so none flashes up before the wait.
        if (audio != null) _state.update { it.copy(preparingAudio = AudioPreparation(0, 0)) }
        if (speech != null) _state.update { it.copy(modes = AnswerMode.entries) }
        load()
        prepareSpeech()
        // The cards follow the language chosen in the top bar.
        viewModelScope.launch {
            settings.settings.map { it.currentLanguageId }.distinctUntilChanged().drop(1).collect {
                answers.clear()
                pass = null
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

    /** Shows the card on screen again as it is now, its answer still showing if it was: after its details were changed. */
    fun reloadCard() {
        val shown = _state.value.card?.termId
        load(prefer = shown, keepAnswer = _state.value.revealed)
    }

    /**
     * Shows the next card, or that there is none; [prefer] names a term whose card comes first, and
     * [keepAnswer] leaves its answer showing when it is the card already on screen.
     */
    fun load(prefer: Long? = null, keepAnswer: Boolean = false) {
        extrasJob?.cancel()
        waitingJob?.cancel()
        stopHearing()
        val shownBefore = _state.value.card?.termId
        viewModelScope.launch {
            val mode = settings.current.flashcardAnswerMode.takeIf { it in _state.value.modes } ?: AnswerMode.SHOW
            val session = nextSession(prefer)
            val language = languageId?.let { languages.getById(it) } ?: session.term?.let { languages.getById(it.languageId) }
            current = session.card
            val term = session.term
            val wordRange = term?.let { service.clozeRange(it) }
            val reading = if (session.card == null) lastReading() else null
            val same = keepAnswer && session.card != null && session.card!!.termId == shownBefore
            if (!same) shownAt = session.card?.let { TimeSource.Monotonic.markNow() }
            _state.update {
                it.copy(
                    loading = false,
                    answerMode = mode,
                    // What was typed stays with the card it was typed for.
                    typed = if (same) it.typed else "",
                    typedResult = if (same) it.typedResult else null,
                    suggested = if (same) it.suggested else null,
                    heard = if (same) it.heard else "",
                    spokenResult = if (same) it.spokenResult else null,
                    typeInstead = same && it.typeInstead,
                    retry = null,
                    languageName = language?.name.orEmpty(),
                    card = if (session.card != null && term != null) content(term, language, wordRange, session.card!!.schedule.state) else null,
                    revealed = same && it.revealed,
                    counts = session.counts,
                    nextLearningAt = session.nextLearningAt,
                    canUndo = answers.isNotEmpty(),
                    twoButtons = settings.current.flashcardTwoButtons,
                    waits = session.outcomes.mapValues { (_, after) -> CardWait(after.intervalDays.takeIf { after.state.isReview }, after.due - session.shownAt) },
                    reading = reading,
                    pass = pass,
                    backlog = session.backlog,
                )
            }
            if (term != null && language != null) loadExtras(term, language)
            findOutHearing(_state.value.card?.languageCode)
            // Nothing to show while a card being learned waits: look again as its time nears.
            if (session.card == null && session.nextLearningAt != null) {
                waitingJob = viewModelScope.launch {
                    delay(15.seconds)
                    load()
                }
            }
        }
    }

    /**
     * The card to show: [prefer]'s, else the extra pass's next, skipping its cards suspended or
     * deleted meanwhile, else the day's next once the pass is over.
     */
    private suspend fun nextSession(prefer: Long?): FlashcardService.Session {
        if (prefer != null) return service.next(languageId, prefer)
        while (true) {
            val going = pass ?: return service.next(languageId)
            val id = going.current ?: run { pass = null; return service.next(languageId) }
            val session = service.next(languageId, id)
            if (session.card?.termId == id) return session
            pass = going.copy(done = going.done + 1)
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
            sentenceTranslation = term.sentenceTranslation?.takeIf { sentence != null && it.isNotBlank() },
            translating = sentence != null && term.sentenceTranslation.isNullOrBlank(),
            wordRange = wordRange,
            translation = term.translation.orEmpty().trim(),
            romanization = term.romanization.orEmpty().trim(),
            languageCode = language?.let { LanguageCodes.codeFor(it.name) },
            rightToLeft = language?.rightToLeft == true,
            state = state,
        )
    }

    /**
     * The sentence's translation, the part of speech and, for a term saved without
     * one, a translation: looked up after the card is on screen, which never waits for them.
     */
    /** The text of the language read most recently and not finished: a book of the library or a course lesson. */
    private suspend fun lastReading(): ReadingToContinue? {
        val repository = books ?: return null
        val id = languageId ?: return null
        val library = repository.observeBooks(archived = false).first()
            .filter { it.languageId == id && it.lastOpened != null && !it.isCompleted }
            .map { it.id to it.lastOpened!! }
        // Lesson texts are kept out of the library list; their language is their book's.
        val lessons = repository.observeLessonBooks().first().values
            .filter { it.lastOpened != null && !it.isCompleted }
            .mapNotNull { lesson -> repository.getBook(lesson.bookId)?.takeIf { it.languageId == id }?.let { lesson.bookId to lesson.lastOpened!! } }
        return (library + lessons).maxByOrNull { it.second }?.let { ReadingToContinue(it.first) }
    }

    private fun loadExtras(term: Term, language: Language) {
        val sentence = term.sentence?.trim()?.takeIf { it.isNotEmpty() }
        val code = LanguageCodes.codeFor(language.name)
        fun updateCard(change: (CardContent) -> CardContent) = _state.update { s -> if (s.card?.termId == term.id) s.copy(card = change(s.card)) else s }
        extrasJob = viewModelScope.launch {
            launch {
                // A translation kept with the word is shown as it is; one made now is kept for the next time.
                val translated = if (!term.sentenceTranslation.isNullOrBlank()) term.sentenceTranslation else sentence?.let { translate(term.id, it, language) }
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
        session.drop(after + 1).filter { it.languageId == language.id && it.sentenceTranslation.isNullOrBlank() }
            .mapNotNull { next -> next.sentence?.takeIf(String::isNotBlank)?.let { next.id to it } }
            .take(TRANSLATE_AHEAD)
            .forEach { (termId, sentence) -> translate(termId, sentence, language) }
    }

    /** Translates [termId]'s [sentence], keeping the translation with the word where it can be kept. */
    private suspend fun translate(termId: Long, sentence: String, language: Language): String? =
        attempt { sentences?.make(termId, sentence, language) ?: translator.translate(sentence.trim(), language) }

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
        if (_state.value.listening) stopHearing()
        // Shown without an answer given, the card was not known.
        _state.update { it.copy(revealed = true, suggested = it.suggested ?: if (it.cardMode != AnswerMode.SHOW) Rating.AGAIN else null) }
        if (settings.current.flashcardAutoplay) events.trySend(FlashcardsEvent.Speak(card.spoken, card.languageCode))
    }

    fun answer(rating: Rating) {
        val card = current ?: return
        if (!_state.value.revealed) return
        current = null
        val word = _state.value.card?.word.orEmpty()
        events.trySend(FlashcardsEvent.StopSpeaking)
        viewModelScope.launch {
            val answeredBy = when {
                _state.value.spokenResult != null -> AnswerMode.SPEAK
                _state.value.typedResult != null -> AnswerMode.TYPE
                else -> AnswerMode.SHOW
            }
            val answered = service.answer(card, rating, answeredBy, shownAt?.elapsedNow())?.also { answers.addLast(it) }
            passOn(card.termId)
            val leech = answered?.takeIf { it.leech }?.let { LeechNote(word, it.lapses, it.suspended) }
            _state.update { it.copy(answered = it.answered + 1, leech = leech) }
            load()
        }
    }

    /** The pass moves past [termId]'s card when it was the one it had on show. */
    private fun passOn(termId: Long) {
        pass?.let { going -> if (going.current == termId) pass = going.copy(done = going.done + 1) }
    }

    /** Adds [count] new cards to today's limit and shows the next card. */
    fun addNewToday(count: Int) {
        viewModelScope.launch {
            service.addNewToday(languageId, count)
            load()
        }
    }

    /** Goes over [study]'s cards before the day's own; a pass with none leaves the session as it was. */
    fun startPass(study: ExtraStudy) {
        viewModelScope.launch {
            val ids = service.extraCards(languageId, study)
            if (ids.isEmpty()) return@launch
            pass = ExtraPass(study, ids)
            load()
        }
    }

    /** Leaves the extra pass for the day's own cards. */
    fun endPass() {
        if (pass == null) return
        pass = null
        load()
    }

    /** How many cards [study] would go over now. */
    override suspend fun extraCount(study: ExtraStudy): Int = service.extraCards(languageId, study).size

    /** How many new cards wait beyond today's limit. */
    override suspend fun newBeyondToday(): Int = service.newBeyondToday(languageId)

    /** Answers cards [mode]'s way from now on, here and in the settings; a card being answered another way starts afresh. */
    fun setAnswerMode(mode: AnswerMode) {
        if (mode !in _state.value.modes) return
        stopHearing()
        viewModelScope.launch { settings.update { it.copy(flashcardAnswerMode = mode) } }
        _state.update { if (it.revealed) it.copy(answerMode = mode) else it.copy(answerMode = mode, typed = "", typedResult = null, suggested = null, typeInstead = false) }
    }

    /** Finds out whether speech in the language with [code] can be heard, unless it is known already or not wanted. */
    private fun findOutHearing(code: String?) {
        val recogniser = speech ?: return
        if (code == null) return _state.update { it.copy(speakReady = false) }
        hearable[code]?.let { known -> return _state.update { it.copy(speakReady = known) } }
        _state.update { it.copy(speakReady = null) }
        viewModelScope.launch {
            val known = attempt { recogniser.isAvailable(code) } == true
            hearable[code] = known
            _state.update { if (it.card?.languageCode == code) it.copy(speakReady = known) else it }
        }
    }

    /**
     * Listens to the card's sentence said aloud, its word filled in, and shows the answer once
     * something was heard; whether the word was heard suggests the rating. Nothing made out leaves
     * the card to say again.
     */
    fun listen() {
        val s = _state.value
        val card = s.card ?: return
        val hidden = card.hidden ?: return
        val code = card.languageCode ?: return
        if (!s.speaking || s.listening || s.speakReady == false) return
        val mutedBefore = s.retry == SpeechRetry.MUTED
        events.trySend(FlashcardsEvent.StopSpeaking)
        _state.update { it.copy(listening = true, retry = null) }
        listenJob = viewModelScope.launch {
            val said = listenForAnswer(speech, code, card.sentence, mutedBefore, longPauses = true) {
                _state.update { if (it.card?.termId == card.termId) it.copy(recognising = true) else it }
            }
            if (_state.value.card?.termId != card.termId) return@launch
            _state.update { it.copy(listening = false, recognising = false) }
            when (said) {
                is SpokenAnswer.Said -> {
                    // Only the hidden word counts; the rest of the sentence is shown word by word, as it was heard.
                    val result = PracticeChecker.checkSpoken(said.text, hidden)
                    _state.update { it.copy(heard = said.text, spokenResult = result, suggested = suggestion(result, it.twoButtons)) }
                    reveal()
                }
                is SpokenAnswer.Again -> _state.update { it.copy(retry = said.why) }
                SpokenAnswer.GivenUp -> refuseSpeaking()
            }
        }
    }

    /** Stops listening; what was heard so far is checked. */
    fun stopListening() {
        if (_state.value.listening) speech?.stop()
    }

    /** The microphone may not be used: asked again it would be refused again, so the words are typed for the rest of the session. */
    fun refuseSpeaking() {
        stopHearing()
        _state.update { it.copy(speakingRefused = true) }
    }

    /** Types the card's word instead of saying it. */
    fun typeInstead() {
        stopHearing()
        _state.update { it.copy(typeInstead = true) }
    }

    private fun stopHearing() {
        if (listenJob?.isActive == true) {
            listenJob?.cancel()
            speech?.stop()
        }
        _state.update { it.copy(listening = false, recognising = false, retry = null) }
    }

    fun type(text: String) {
        if (!_state.value.revealed) _state.update { it.copy(typed = text) }
    }

    /** Checks what was typed against the hidden word and shows the answer, with the rating it earns suggested; nothing typed only shows it. */
    fun check() {
        val s = _state.value
        val hidden = s.card?.hidden ?: return
        if (s.revealed) return
        if (s.typed.isNotBlank()) {
            val result = PracticeChecker.check(hidden, s.typed)
            _state.update { it.copy(typedResult = result, suggested = suggestion(result, it.twoButtons)) }
        }
        reveal()
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
            val given = translation?.takeIf(String::isNotBlank)
            val range = service.replaceSentence(card.termId, text, given)
            updateCard { it.copy(sentence = text, wordRange = range, sentenceTranslation = given, translating = given == null) }
            audio?.let { launch { attempt { it.prepare(listOf(text), card.languageCode) } } }
            if (translation.isNullOrBlank()) {
                val translated = languages.getById(card.languageId)?.let { language -> translate(card.termId, text, language) }
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
            // Back in the pass too, when the answer taken back was its last.
            pass?.let { going -> if (going.done > 0 && going.termIds[going.done - 1] == answered.before.termId) pass = going.copy(done = going.done - 1) }
            _state.update { it.copy(answered = (it.answered - 1).coerceAtLeast(0), leech = null) }
            load(prefer = answered.before.termId)
        }
    }

    /** Deletes the card on show with its word; answers given to it this session can no longer be taken back. */
    fun deleteCard() {
        val card = current ?: return
        current = null
        events.trySend(FlashcardsEvent.StopSpeaking)
        answers.removeAll { it.before.termId == card.termId }
        passOn(card.termId)
        viewModelScope.launch {
            service.delete(listOf(card.termId))
            load()
        }
    }

    /** Leaves the card on show until the next study day, its schedule as it is. */
    fun skipCard() {
        val card = current ?: return
        current = null
        events.trySend(FlashcardsEvent.StopSpeaking)
        passOn(card.termId)
        viewModelScope.launch {
            service.skip(listOf(card.termId))
            load()
        }
    }

    /** Puts the card on show aside until it is resumed from its term. */
    fun suspendCard() {
        val card = current ?: return
        current = null
        events.trySend(FlashcardsEvent.StopSpeaking)
        passOn(card.termId)
        viewModelScope.launch {
            service.setSuspended(card.termId, true)
            load()
        }
    }
}

/** The answer a checked one earns: right is Good, right but for an accent or a slip Hard (Good with two buttons), wrong Again. */
internal fun suggestion(result: TypedResult, twoButtons: Boolean): Rating = when (result) {
    TypedResult.CORRECT -> Rating.GOOD
    TypedResult.ALMOST, TypedResult.TYPO -> if (twoButtons) Rating.GOOD else Rating.HARD
    TypedResult.WRONG -> Rating.AGAIN
}

/** How many cards ahead have their sentences translated before they come. */
private const val TRANSLATE_AHEAD = 3
