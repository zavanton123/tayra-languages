package com.tayra.languages.feature.reading.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.practice.Exercise
import com.tayra.languages.core.domain.practice.ExerciseType
import com.tayra.languages.core.domain.practice.PracticeBuilder
import com.tayra.languages.core.domain.practice.PracticeChecker
import com.tayra.languages.core.domain.practice.PracticeExample
import com.tayra.languages.core.domain.practice.PracticeMaterial
import com.tayra.languages.core.domain.practice.PracticeMaterialLoader
import com.tayra.languages.core.domain.practice.PracticeSource
import com.tayra.languages.core.domain.practice.PracticeWord
import com.tayra.languages.core.domain.practice.TypedResult
import com.tayra.languages.core.domain.service.SentenceTranslator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import com.tayra.languages.core.domain.practice.PracticeSetup
import com.tayra.languages.core.domain.practice.PracticeContext
import com.tayra.languages.core.domain.practice.forPractice
import com.tayra.languages.core.domain.practice.PracticeExampleStore
import com.tayra.languages.core.domain.practice.PracticeExampleRepository
import com.tayra.languages.core.domain.practice.WordForms
import com.tayra.languages.core.domain.practice.WordFamily
import kotlin.time.Clock
import com.tayra.languages.core.domain.practice.WordProgress
import com.tayra.languages.core.domain.practice.PracticeProgressRepository
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Mutex
import com.tayra.languages.core.domain.practice.Heard
import com.tayra.languages.core.domain.practice.PracticeResponse
import com.tayra.languages.core.domain.practice.SpeechRecognition

/** How a question was answered. */
enum class PracticeResult { CORRECT, ALMOST, TYPO, WRONG }

data class PracticeUiState(
    val loading: Boolean = true,
    val bookTitle: String = "",
    /** The page practised, or null for words chosen in the vocabulary. */
    val pageNumber: Int? = 1,
    /** How many chosen words the questions are about. */
    val wordCount: Int = 0,
    /** What is asked. */
    val setup: PracticeSetup = PracticeSetup(),
    val languageCode: String? = null,
    val rightToLeft: Boolean = false,
    val exercises: List<Exercise> = emptyList(),
    val index: Int = 0,
    val typed: String = "",
    val chosen: String? = null,
    /** Null until the question on show is answered. */
    val result: PracticeResult? = null,
    /** The words matched so far in a matching question. */
    val matched: Set<String> = emptySet(),
    /** Wrong pairs tried in a matching question. */
    val wrongPairs: Int = 0,
    /** The translation of the sentence on show, once known. */
    val sentenceTranslation: String? = null,
    val correct: Int = 0,
    /** The words answered wrongly or skipped, in the order they came up. */
    val missed: List<String> = emptyList(),
    /** The practised words behind [missed], lower-cased, to practise them again. */
    val missedTargets: Set<String> = emptySet(),
    val finished: Boolean = false,
    /** Practising the whole book or lesson rather than a page of it. */
    val wholeBook: Boolean = false,
    /** Hearing an answer said aloud. */
    val listening: Boolean = false,
    /** Speaking is over and what was said is being made out. */
    val recognising: Boolean = false,
    /** The last try at saying the answer made out nothing. */
    val nothingHeard: Boolean = false,
    /** The microphone could not be used, so answers to be said are typed instead. */
    val speakingRefused: Boolean = false,
) {
    val current: Exercise? get() = if (finished) null else exercises.getOrNull(index)
    val answered: Boolean get() = result != null
    val fromVocabulary: Boolean get() = pageNumber == null && !wholeBook
}

/**
 * A short practice of words being learned: those on a page of a book, built from the page's own
 * sentences, or words chosen in the vocabulary, built from the sentences they were read in; in
 * both cases with sentences from elsewhere when they can be fetched. It only asks: answers
 * change no word's status and no flashcard.
 */
class PracticeViewModel(
    private val source: PracticeSource,
    private val setup: PracticeSetup,
    private val material: PracticeMaterialLoader,
    private val exampleStore: PracticeExampleStore,
    private val keptExamples: PracticeExampleRepository,
    private val translator: SentenceTranslator,
    /** Words' forms and frequency, for wrong choices as common as the right one; none without it. */
    private val wordForms: WordForms? = null,
    /** Each word's practice level: read for a setup that adapts to it, moved by every answer. */
    private val progress: PracticeProgressRepository? = null,
    /** Hears answers said aloud; without it, or for a language it cannot hear, they are typed. */
    private val speech: SpeechRecognition? = null,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val pageNumber: Int? = (source as? PracticeSource.Page)?.page
    private val wholeBook = source is PracticeSource.Book

    private val _state = MutableStateFlow(PracticeUiState(pageNumber = pageNumber, setup = setup, wholeBook = wholeBook))
    val state: StateFlow<PracticeUiState> = _state.asStateFlow()

    private var language: Language? = null
    private val recording = Mutex()
    private var hintJob: Job? = null
    private var listenJob: Job? = null

    init {
        start()
    }

    /** Builds a fresh set of questions and starts from the first. */
    fun start() = start(only = null)

    /** Practises again only the words missed in the round just finished. */
    fun startMissed() = start(only = _state.value.missedTargets.takeIf { it.isNotEmpty() })

    private fun start(only: Set<String>?) {
        _state.update { PracticeUiState(pageNumber = pageNumber, bookTitle = it.bookTitle, setup = setup, wholeBook = wholeBook) }
        viewModelScope.launch {
            val all = attempt { material.load(source) }
            val prepared = if (all == null || only == null) all else PracticeMaterial(all.language, all.title, all.words.filter { it.text.lowercase() in only }.ifEmpty { all.words }, all.pool, all.meanings)
            if (prepared == null) {
                _state.update { it.copy(loading = false) }
                return@launch
            }
            val lang = prepared.language
            language = lang
            val code = LanguageCodes.codeFor(lang.name)
            // Answers to be said are typed where nothing here can hear them.
            val canSpeak = code != null && speech != null && attempt { speech.isAvailable(code) } == true
            val asked = if (canSpeak) setup else setup.withoutSpeaking()
            // Translation and building questions are asked through the sentence's translation; form questions give it as their clue.
            val translations = if (asked.types.any { it in TRANSLATED }) translateSentences(prepared.words, lang) else emptyMap()
            val families = if (ExerciseType.FORM in asked.types || ExerciseType.SOUND_ALIKE in asked.types) attempt { familiesOf(prepared.words, lang) }.orEmpty() else emptyMap()
            val alikes = attempt { alikesFor(prepared.words, lang) }.orEmpty()
            val levels = if (asked.adaptive) attempt { levelsOf(prepared.words) }.orEmpty() else emptyMap()
            val exercises = PracticeBuilder.build(prepared.words, prepared.pool, examplesFor(prepared.words), random, asked, prepared.meanings, translations, alikes, families, levels)
            _state.update {
                it.copy(
                    loading = false,
                    bookTitle = prepared.title,
                    wordCount = prepared.words.size,
                    setup = asked,
                    languageCode = code,
                    rightToLeft = lang.rightToLeft,
                    exercises = exercises,
                )
            }
            loadTranslation()
        }
    }

    /**
     * The sentences from elsewhere kept for the words, when the setup asks for some; words short of
     * them are looked up first, for as long as [EXAMPLE_WAIT] allows.
     */
    private suspend fun examplesFor(words: List<PracticeWord>): Map<String, List<PracticeExample>> {
        if (PracticeContext.EXAMPLE !in setup.contexts) return emptyMap()
        attempt { exampleStore.topUp(words.mapNotNull { it.termId }, EXAMPLE_WAIT) }
        return attempt { keptExamples.forPractice(words) }.orEmpty()
    }

    /**
     * For each word, words about as common in the language and looking like it (the same first
     * letter or the same ending), none of them a form of it: wrong choices that take knowing it.
     */
    private suspend fun alikesFor(words: List<PracticeWord>, language: Language): Map<String, List<String>> {
        val forms = wordForms ?: return emptyMap()
        return words.mapNotNull { word ->
            val rank = forms.rank(word.text, language) ?: return@mapNotNull null
            val own = forms.of(word.text, language)
            val text = word.text.lowercase()
            val near = forms.ranked(language, (rank * (1 - RANK_SPREAD)).toInt().coerceAtLeast(1), (rank * (1 + RANK_SPREAD)).toInt() + RANK_MARGIN)
                .filter { other -> other.lowercase() !in own && (other.first().lowercaseChar() == text.first() || other.lowercase().takeLast(2) == text.takeLast(2)) }
            word.key to near.take(ALIKES)
        }.toMap()
    }

    private suspend fun levelsOf(words: List<PracticeWord>): Map<String, Int> {
        val kept = progress?.progress(words.mapNotNull { it.termId }) ?: return emptyMap()
        return words.mapNotNull { word -> kept[word.termId]?.let { word.key to it.level } }.toMap()
    }

    private suspend fun familiesOf(words: List<PracticeWord>, language: Language): Map<String, WordFamily> {
        val forms = wordForms ?: return emptyMap()
        return words.mapNotNull { word -> forms.family(word.text, language)?.let { word.key to it } }.toMap()
    }

    /** The translations of the words' sentences, for questions asked through them; those not ready in time are left out. */
    private suspend fun translateSentences(words: List<PracticeWord>, language: Language): Map<String, String> {
        val translated = mutableMapOf<String, String>()
        withTimeoutOrNull(TRANSLATION_WAIT) {
            for (sentence in words.map { it.sentence }.distinct()) {
                attempt { translator.translate(sentence, language) }?.trim()?.takeIf { it.isNotEmpty() }?.let { translated[sentence] = it }
            }
        }
        return translated
    }

    private suspend fun <T> attempt(block: suspend () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun loadTranslation() {
        hintJob?.cancel()
        val exercise = _state.value.current ?: return
        val lang = language ?: return
        _state.update { it.copy(sentenceTranslation = exercise.sentenceTranslation) }
        if (exercise.sentenceTranslation != null) return
        hintJob = viewModelScope.launch {
            val translated = attempt { translator.translate(exercise.sentence, lang) }?.trim()?.takeIf { it.isNotEmpty() }
            _state.update { if (it.current === exercise) it.copy(sentenceTranslation = translated) else it }
        }
    }

    fun setTyped(text: String) {
        if (!_state.value.answered) _state.update { it.copy(typed = text) }
    }

    /** Answers a question with choices. */
    fun choose(option: String) {
        val exercise = _state.value.current ?: return
        if (_state.value.answered || option !in exercise.options) return
        _state.update { it.copy(chosen = option) }
        record(exercise, if (option == exercise.answer) PracticeResult.CORRECT else PracticeResult.WRONG)
    }

    /**
     * Pairs [word] with [meaning] in a matching question: a right pair stays matched, a wrong one
     * counts against it; with every word matched the question is answered, right if no pair was
     * wrong. Returns whether the pair was right.
     */
    fun match(word: String, meaning: String): Boolean {
        val exercise = _state.value.current ?: return false
        if (_state.value.answered || exercise.type != ExerciseType.MATCHING || word in _state.value.matched) return false
        val right = exercise.pairs.any { it.first == word && it.second == meaning }
        _state.update { if (right) it.copy(matched = it.matched + word) else it.copy(wrongPairs = it.wrongPairs + 1) }
        if (right && _state.value.matched.size == exercise.pairs.size) record(exercise, if (_state.value.wrongPairs == 0) PracticeResult.CORRECT else PracticeResult.ALMOST)
        return right
    }

    /** Listens to the answer said aloud and checks it; nothing made out leaves the question to try again. */
    fun listen() {
        val exercise = _state.value.current ?: return
        val code = _state.value.languageCode
        if (_state.value.answered || _state.value.listening || exercise.response != PracticeResponse.SPEAK || code == null) return
        _state.update { it.copy(listening = true, nothingHeard = false, typed = "") }
        listenJob = viewModelScope.launch {
            val heard = attempt { speech?.listen(code, exercise.saying ?: exercise.sentence) { _state.update { if (it.current === exercise) it.copy(recognising = true) else it } } } ?: Heard.Refused
            _state.update { it.copy(recognising = false) }
            if (_state.value.current !== exercise) return@launch
            when (heard) {
                is Heard.Words -> {
                    _state.update { it.copy(listening = false, typed = heard.text) }
                    // The phrase is checked, the missing word in it where there is one.
                    val word = if (exercise.wholeSentence) null else exercise.answer
                    record(exercise, PracticeChecker.checkSpoken(heard.text, word, exercise.saying ?: exercise.answer).result)
                }
                Heard.Nothing -> _state.update { it.copy(listening = false, nothingHeard = true) }
                Heard.Refused -> refuseSpeaking()
            }
        }
    }

    /** The microphone may not be used: asked again it would be refused again, so the rest of the answers are typed. */
    fun refuseSpeaking() = _state.update { s ->
        s.copy(
            listening = false,
            speakingRefused = true,
            exercises = s.exercises.mapIndexed { i, e -> if (i >= s.index && e.response == PracticeResponse.SPEAK) e.copy(response = PracticeResponse.TYPE) else e },
        )
    }

    /** Stops listening; what was heard so far is checked. */
    fun stopListening() {
        if (_state.value.listening) speech?.stop()
    }

    /** Types the answer to the question on show instead of saying it. */
    fun typeInstead() {
        val s = _state.value
        val exercise = s.current ?: return
        if (s.answered || exercise.response != PracticeResponse.SPEAK) return
        if (s.listening) {
            listenJob?.cancel()
            speech?.stop()
        }
        _state.update { it.copy(listening = false, nothingHeard = false, typed = "", exercises = it.exercises.toMutableList().also { list -> list[it.index] = exercise.copy(response = PracticeResponse.TYPE) }) }
    }

    /** Checks what was typed. */
    fun submit() {
        val exercise = _state.value.current ?: return
        if (_state.value.answered || exercise.isChoice || exercise.response == PracticeResponse.SPEAK || _state.value.typed.isBlank()) return
        record(exercise, PracticeChecker.check(exercise.answer, _state.value.typed).result)
    }

    private val TypedResult.result: PracticeResult
        get() = when (this) {
            TypedResult.CORRECT -> PracticeResult.CORRECT
            TypedResult.ALMOST -> PracticeResult.ALMOST
            TypedResult.TYPO -> PracticeResult.TYPO
            TypedResult.WRONG -> PracticeResult.WRONG
        }

    /** Gives up on the question: its answer is shown and it counts as missed. */
    fun skip() {
        val exercise = _state.value.current ?: return
        if (_state.value.answered) return
        if (_state.value.listening) {
            listenJob?.cancel()
            speech?.stop()
            _state.update { it.copy(listening = false) }
        }
        if (exercise.type == ExerciseType.MATCHING) _state.update { it.copy(matched = exercise.pairs.map { pair -> pair.first }.toSet()) }
        record(exercise, PracticeResult.WRONG)
    }

    private fun record(exercise: Exercise, result: PracticeResult) {
        remember(exercise, result)
        tally(exercise, result)
    }

    /** Moves the word's practice level by the answer; a matching round, on several words at once, moves none. */
    private fun remember(exercise: Exercise, result: PracticeResult) {
        val termId = exercise.termId ?: return
        val repository = progress ?: return
        if (exercise.type == ExerciseType.MATCHING) return
        val answer = when (result) {
            PracticeResult.CORRECT, PracticeResult.ALMOST -> WordProgress.Answer.RIGHT
            PracticeResult.TYPO -> WordProgress.Answer.SLIP
            PracticeResult.WRONG -> WordProgress.Answer.WRONG
        }
        // One at a time: each answer moves the progress the one before it left.
        viewModelScope.launch { recording.withLock { attempt { repository.record(termId, answer, Clock.System.now()) } } }
    }

    private fun tally(exercise: Exercise, result: PracticeResult) = _state.update {
        val right = result != PracticeResult.WRONG
        it.copy(
            result = result,
            correct = it.correct + if (right) 1 else 0,
            missed = if (right || exercise.practised in it.missed) it.missed else it.missed + exercise.practised,
            // A sentence from elsewhere may use another form of the word; the word practised is the one missed.
            missedTargets = if (right) it.missedTargets else it.missedTargets + exercise.practised.lowercase(),
        )
    }

    /** Moves on from an answered question. */
    fun next() {
        val s = _state.value
        if (!s.answered) return
        if (s.index + 1 >= s.exercises.size) {
            _state.update { it.copy(finished = true, result = null, chosen = null, typed = "", nothingHeard = false) }
            return
        }
        _state.update { it.copy(index = it.index + 1, result = null, chosen = null, typed = "", sentenceTranslation = null, matched = emptySet(), wrongPairs = 0, nothingHeard = false) }
        loadTranslation()
    }

    private companion object {
        val EXAMPLE_WAIT = 4.seconds

        /** How far in rank, as a share of a word's own, a word as common as it may be. */
        const val RANK_SPREAD = 0.3
        const val RANK_MARGIN = 5
        const val ALIKES = 12

        /** The questions that need the sentence's translation. */
        val TRANSLATED = setOf(ExerciseType.BUILD, ExerciseType.FORM)

        /** How long the sentences of translation questions may take to translate, all together. */
        val TRANSLATION_WAIT = 8.seconds
    }
}
