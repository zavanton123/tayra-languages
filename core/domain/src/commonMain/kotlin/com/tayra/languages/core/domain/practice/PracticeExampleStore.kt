package com.tayra.languages.core.domain.practice

import com.tayra.languages.core.domain.flashcards.Cloze
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.ExampleSort
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Looks up sentences from elsewhere that use a word being learned, in its own form or another,
 * and keeps a few of them, translated, so practice can ask the word in new contexts without the
 * network. Words are looked up as they start being learned, and again before a practice when
 * they have too few.
 */
class PracticeExampleStore(
    private val repository: PracticeExampleRepository,
    private val examples: ExampleSentencesProvider,
    private val forms: WordForms,
    private val terms: TermRepository,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val translator: SentenceTranslator? = null,
    private val clock: Clock = Clock.System,
) {
    private val queue = Channel<Long>(Channel.UNLIMITED)

    /** Queues the words; those not being learned, or with examples enough, are skipped when their turn comes. */
    fun request(termIds: Collection<Long>) {
        for (id in termIds) queue.trySend(id)
    }

    fun start(scope: CoroutineScope): Job = scope.launch {
        for (id in queue) {
            try {
                fill(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Left with what it has; looked up again before its next practice.
            }
        }
    }

    /** Before a practice: the words of [termIds] with fewer than [MIN] examples are looked up, as far as [wait] allows. */
    suspend fun topUp(termIds: Collection<Long>, wait: Duration) {
        val counts = repository.counts(termIds)
        val short = termIds.filter { (counts[it] ?: 0) < MIN }.take(TOP_UP_WORDS)
        withTimeoutOrNull(wait) {
            for (id in short) {
                try {
                    fill(id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Practice goes on with the examples there are.
                }
            }
        }
    }

    /** Looks up and keeps examples for the word, when it is being learned and has fewer than [MIN]; returns how many were added. */
    suspend fun fill(termId: Long): Int {
        val term = terms.getById(termId) ?: return 0
        if (!term.status.isLearning) return 0
        val have = repository.examples(listOf(termId))[termId].orEmpty()
        if (have.size >= MIN) return 0
        val language = languages.getById(term.languageId) ?: return 0
        val found = find(term, language, have.map { it.sentence }.toSet())
        if (found.isEmpty()) return 0
        repository.add(found.take(KEEP - have.size), clock.now())
        return found.size.coerceAtMost(KEEP - have.size)
    }

    private suspend fun find(term: Term, language: Language, known: Set<String>): List<StoredExample> {
        val native = settings.current.nativeLanguage.ifBlank { "en" }
        val wordForms = forms.of(term.displayText, language)
        val result = examples.search(ExampleSearchQuery(term.displayText, language, native, minWords = MIN_WORDS, maxWords = MAX_WORDS, sort = ExampleSort.RELEVANCE, limit = SEARCHED))
        val own = term.sentence?.trim()
        val usable = result.sentences.mapNotNull { example ->
            val text = example.text.trim()
            if (text.isEmpty() || text == own || text in known) return@mapNotNull null
            // The word's own form first, then any other of its forms.
            val range = Cloze.range(text, term.displayText) ?: wordForms.firstNotNullOfOrNull { Cloze.range(text, it) } ?: return@mapNotNull null
            val source = if (example.lesson != null) SOURCE_COURSE else SOURCE_TATOEBA
            StoredExample(term.id, text, example.translation?.trim()?.takeIf { it.isNotEmpty() }, text.substring(range), range.first, source, example.sourceUrl)
        }.distinctBy { it.sentence }
            .sortedWith(compareBy({ !it.form.equals(term.displayText, ignoreCase = true) }, { it.source != SOURCE_COURSE }))
            .take(KEEP)
        return usable.map { e -> if (e.translation != null || translator == null) e else e.copy(translation = translate(e.sentence, language)) }
    }

    private suspend fun translate(sentence: String, language: Language): String? = try {
        translator?.translate(sentence, language)?.trim()?.takeIf { it.isNotEmpty() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    companion object {
        /** A word with fewer examples than this is looked up. */
        const val MIN = 3

        /** At most this many are kept for a word. */
        const val KEEP = 5

        const val SOURCE_TATOEBA = "tatoeba"
        const val SOURCE_COURSE = "course"

        private const val MIN_WORDS = 4
        private const val MAX_WORDS = 10
        private const val SEARCHED = 12

        /** Words looked up before one practice, at most. */
        private const val TOP_UP_WORDS = 8
    }
}
