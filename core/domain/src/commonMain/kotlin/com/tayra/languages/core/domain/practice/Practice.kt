package com.tayra.languages.core.domain.practice

import kotlin.random.Random

/** A word being learned, where it stands in a sentence of the page. */
data class PracticeWord(
    val text: String,
    val translation: String?,
    val sentence: String,
    /** Where the word stands in [sentence]. */
    val range: IntRange,
)

/** A sentence from elsewhere that uses a word of the page. */
data class PracticeExample(val sentence: String, val range: IntRange, val translation: String?)

enum class ExerciseKind {
    /** The page's sentence with the word blanked out; pick the word. */
    GAP_CHOICE,

    /** The page's sentence with the word blanked out; type the word. */
    GAP_TYPED,

    /** A sentence from elsewhere with the word blanked out; pick the word. */
    NEW_CONTEXT,

    /** The word is read aloud; pick its meaning, or the word itself when meanings are missing. */
    HEAR_CHOOSE,

    /** The sentence is read aloud; type the missing word, or all of a short sentence. */
    DICTATION,
}

/** One question of a page's practice. */
data class Exercise(
    val kind: ExerciseKind,
    /** The word practised, as written in the sentence. */
    val word: String,
    val wordTranslation: String?,
    val sentence: String,
    /** Where the word stands in [sentence]; blanked out until answered. */
    val range: IntRange,
    /** The translation of a sentence from elsewhere; a page sentence's is looked up when shown. */
    val sentenceTranslation: String? = null,
    /** The choices, the right one among them; empty when the answer is typed. */
    val options: List<String> = emptyList(),
    /** The right choice, or what is to be typed. */
    val answer: String,
    /** What is read aloud, for the listening kinds. */
    val spoken: String? = null,
    /** The answer is the whole sentence, not one word. */
    val wholeSentence: Boolean = false,
) {
    val isChoice: Boolean get() = options.isNotEmpty()
    val isListening: Boolean get() = spoken != null
}

/** Builds the questions for a page from the words being learned on it. */
object PracticeBuilder {
    const val MAX_QUESTIONS = 10
    private const val OPTIONS = 4

    /** Sentences of at most this many words are typed whole in a dictation. */
    private const val SHORT_SENTENCE_WORDS = 6

    private val ROTATION = listOf(ExerciseKind.GAP_CHOICE, ExerciseKind.HEAR_CHOOSE, ExerciseKind.GAP_TYPED, ExerciseKind.DICTATION)

    /**
     * About two questions a word, ten at most, going round the words so that a word's questions
     * are apart, and round the kinds so that each comes up. Words with an [examples] sentence get
     * a question in that new context at the end. [pageWords] are the other words of the page,
     * for wrong choices.
     */
    fun build(
        words: List<PracticeWord>,
        pageWords: List<String>,
        examples: Map<String, PracticeExample> = emptyMap(),
        random: Random = Random.Default,
        maxQuestions: Int = MAX_QUESTIONS,
    ): List<Exercise> {
        val targets = words.distinctBy { it.text.lowercase() }.shuffled(random)
        if (targets.isEmpty()) return emptyList()
        val inNewContext = targets.filter { it.text.lowercase() in examples }.take(2)
        val rotated = (maxOf(targets.size * 2, 5)).coerceAtMost(targets.size * ROTATION.size).coerceAtMost(maxQuestions - inNewContext.size)
        val exercises = mutableListOf<Exercise>()
        for (k in 0 until rotated) {
            val word = targets[k % targets.size]
            // Each round starts one kind further on, so a word meets a different kind every time.
            val kind = ROTATION[(k % targets.size + k / targets.size) % ROTATION.size]
            exercises += exercise(kind, word, targets, pageWords, random)
        }
        for (word in inNewContext) {
            val example = examples.getValue(word.text.lowercase())
            val options = wordChoices(word, targets, pageWords, random)
            exercises += Exercise(
                kind = ExerciseKind.NEW_CONTEXT,
                word = example.sentence.substring(example.range),
                wordTranslation = word.translation,
                sentence = example.sentence,
                range = example.range,
                sentenceTranslation = example.translation,
                options = options,
                answer = word.text,
            )
        }
        return exercises
    }

    private fun exercise(kind: ExerciseKind, word: PracticeWord, targets: List<PracticeWord>, pageWords: List<String>, random: Random): Exercise {
        val base = Exercise(kind, word.text, word.translation, word.sentence, word.range, answer = word.text)
        return when (kind) {
            ExerciseKind.GAP_CHOICE, ExerciseKind.NEW_CONTEXT -> base.copy(options = wordChoices(word, targets, pageWords, random))
            ExerciseKind.GAP_TYPED -> base
            ExerciseKind.HEAR_CHOOSE -> {
                val meaning = word.translation?.trim().orEmpty()
                val others = targets.filter { it !== word }.mapNotNull { it.translation?.trim()?.takeIf(String::isNotEmpty) }
                    .distinctBy { it.lowercase() }.filter { !it.equals(meaning, ignoreCase = true) }
                if (meaning.isNotEmpty() && others.size >= OPTIONS - 1) {
                    base.copy(options = (others.shuffled(random).take(OPTIONS - 1) + meaning).shuffled(random), answer = meaning, spoken = word.text)
                } else {
                    base.copy(options = wordChoices(word, targets, pageWords, random), spoken = word.text)
                }
            }
            ExerciseKind.DICTATION -> {
                val short = word.sentence.split(Regex("""\s+""")).count { part -> part.any { it.isLetterOrDigit() } } <= SHORT_SENTENCE_WORDS
                if (short) base.copy(answer = word.sentence, spoken = word.sentence, wholeSentence = true) else base.copy(spoken = word.sentence)
            }
        }
    }

    /** The word among other words of the page: the other words being learned first, then any others. */
    private fun wordChoices(word: PracticeWord, targets: List<PracticeWord>, pageWords: List<String>, random: Random): List<String> {
        val own = word.text.lowercase()
        val learning = targets.map { it.text }.filter { it.lowercase() != own }.shuffled(random)
        val others = pageWords.filter { other -> other.length > 1 && other.any { it.isLetter() } }.shuffled(random)
        val wrong = (learning + others).distinctBy { it.lowercase() }.filter { it.lowercase() != own }.take(OPTIONS - 1)
        return (wrong + word.text).shuffled(random)
    }
}

/** How a typed answer compares with what was expected. */
enum class TypedResult {
    CORRECT,

    /** Right but for accents: counted as right, with the spelling shown. */
    ALMOST,
    WRONG,
}

/** Compares typed answers: case, punctuation and extra spaces do not count; accents nearly do not. */
object PracticeChecker {
    fun check(expected: String, typed: String): TypedResult {
        val wanted = normalize(expected)
        val given = normalize(typed)
        return when {
            given.isEmpty() -> TypedResult.WRONG
            given == wanted -> TypedResult.CORRECT
            unaccented(given) == unaccented(wanted) -> TypedResult.ALMOST
            else -> TypedResult.WRONG
        }
    }

    private fun normalize(text: String): String =
        text.lowercase().map { if (it.isLetterOrDigit() || it == '\'' || it == '’') it else ' ' }.joinToString("")
            .replace('’', '\'').trim().replace(Regex("""\s+"""), " ")

    /** Plain letters with their accented forms. */
    private val ACCENTS: Map<Char, Char> = listOf(
        'a' to "áàâãäåāăą", 'c' to "çćč", 'd' to "ď", 'e' to "éèêëēęě", 'i' to "íìîïī", 'l' to "ł", 'n' to "ñńň",
        'o' to "óòôõöōő", 'r' to "ř", 's' to "śš", 't' to "ť", 'u' to "úùûüūůű", 'y' to "ýÿ", 'z' to "źżž",
    ).flatMap { (plain, accented) -> accented.map { it to plain } }.toMap()

    private fun unaccented(text: String): String = text.map { ACCENTS[it] ?: it }.joinToString("")
}
