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
    private val random: Random = Random.Default,
) : ViewModel() {

    private val pageNumber: Int? = (source as? PracticeSource.Page)?.page
    private val wholeBook = source is PracticeSource.Book

    private val _state = MutableStateFlow(PracticeUiState(pageNumber = pageNumber, setup = setup, wholeBook = wholeBook))
    val state: StateFlow<PracticeUiState> = _state.asStateFlow()

    private var language: Language? = null
    private var hintJob: Job? = null

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
            val translations = if (ExerciseType.TRANSLATION in setup.types) translateSentences(prepared.words, lang) else emptyMap()
            val alikes = attempt { alikesFor(prepared.words, lang) }.orEmpty()
            val exercises = PracticeBuilder.build(prepared.words, prepared.pool, examplesFor(prepared.words), random, setup, prepared.meanings, translations, alikes)
            _state.update {
                it.copy(
                    loading = false,
                    bookTitle = prepared.title,
                    wordCount = prepared.words.size,
                    languageCode = LanguageCodes.codeFor(lang.name),
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

    /** Checks what was typed. */
    fun submit() {
        val exercise = _state.value.current ?: return
        if (_state.value.answered || exercise.isChoice || _state.value.typed.isBlank()) return
        val result = when (PracticeChecker.check(exercise.answer, _state.value.typed)) {
            TypedResult.CORRECT -> PracticeResult.CORRECT
            TypedResult.ALMOST -> PracticeResult.ALMOST
            TypedResult.TYPO -> PracticeResult.TYPO
            TypedResult.WRONG -> PracticeResult.WRONG
        }
        record(exercise, result)
    }

    /** Gives up on the question: its answer is shown and it counts as missed. */
    fun skip() {
        val exercise = _state.value.current ?: return
        if (!_state.value.answered) record(exercise, PracticeResult.WRONG)
    }

    private fun record(exercise: Exercise, result: PracticeResult) = _state.update {
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
            _state.update { it.copy(finished = true, result = null, chosen = null, typed = "") }
            return
        }
        _state.update { it.copy(index = it.index + 1, result = null, chosen = null, typed = "", sentenceTranslation = null) }
        loadTranslation()
    }

    private companion object {
        val EXAMPLE_WAIT = 4.seconds

        /** How far in rank, as a share of a word's own, a word as common as it may be. */
        const val RANK_SPREAD = 0.3
        const val RANK_MARGIN = 5
        const val ALIKES = 12

        /** How long the sentences of translation questions may take to translate, all together. */
        val TRANSLATION_WAIT = 8.seconds
    }
}
