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

/** The skill a type of question trains; the practice setup groups the types by it. */
enum class PracticeSkill { READING, LISTENING, TRANSLATION }

/** How a question is answered. */
enum class PracticeResponse { CHOOSE, TYPE }

/** Where a question's sentence comes from. */
enum class PracticeContext {
    /** The sentence the word was read in. */
    ORIGINAL,

    /** A sentence from elsewhere that uses the word. */
    EXAMPLE,
}

/** A type of question, with the [skill] it trains and the ways it can be answered. */
enum class ExerciseType(val skill: PracticeSkill, val responses: Set<PracticeResponse>) {
    /** The sentence with the word blanked out: choose or type the word. */
    GAP(PracticeSkill.READING, setOf(PracticeResponse.CHOOSE, PracticeResponse.TYPE)),

    /** The sentence whole, the word marked: choose what the word means. Needs the word's translation. */
    MEANING(PracticeSkill.READING, setOf(PracticeResponse.CHOOSE)),

    /** The word is read aloud: choose its meaning, or the word itself when meanings are missing. */
    HEAR_WORD(PracticeSkill.LISTENING, setOf(PracticeResponse.CHOOSE)),

    /**
     * The sentence is read aloud, its text hidden, and the word's meaning given: choose the word
     * among the sentence's other words, or type it. Needs the word's translation.
     */
    HEAR_SENTENCE(PracticeSkill.LISTENING, setOf(PracticeResponse.CHOOSE, PracticeResponse.TYPE)),

    /** The sentence is read aloud: type the missing word, or all of a short sentence. */
    DICTATION(PracticeSkill.LISTENING, setOf(PracticeResponse.TYPE)),

    /** The sentence's translation over the sentence with the word blanked out: choose or type the word. Needs the translation. */
    TRANSLATION(PracticeSkill.TRANSLATION, setOf(PracticeResponse.CHOOSE, PracticeResponse.TYPE)),
}

/**
 * What a practice asks: the [types] of question, each answered in those of the [responses] it
 * allows, in sentences from the [contexts], [length] questions at most (as many as every type
 * makes for every word when null).
 */
data class PracticeSetup(
    val types: Set<ExerciseType> = setOf(ExerciseType.GAP, ExerciseType.HEAR_WORD, ExerciseType.DICTATION),
    val responses: Set<PracticeResponse> = PracticeResponse.entries.toSet(),
    val contexts: Set<PracticeContext> = PracticeContext.entries.toSet(),
    val length: Int? = DEFAULT_LENGTH,
) {
    /** The questions asked, in the order they come round: those answered by choosing first, then those typed. */
    val rotation: List<Pair<ExerciseType, PracticeResponse>> =
        PracticeResponse.entries.filter { it in responses }.flatMap { response -> ExerciseType.entries.filter { it in types && response in it.responses }.map { it to response } }

    /** The setup as text, for a route or the settings: `types=GAP,DICTATION;responses=CHOOSE;contexts=ORIGINAL;length=10`. */
    fun encode(): String = listOf(
        "types=" + ExerciseType.entries.filter { it in types }.joinToString(",") { it.name },
        "responses=" + PracticeResponse.entries.filter { it in responses }.joinToString(",") { it.name },
        "contexts=" + PracticeContext.entries.filter { it in contexts }.joinToString(",") { it.name },
        "length=" + (length?.toString() ?: ALL),
    ).joinToString(";")

    companion object {
        const val DEFAULT_LENGTH = 10
        private const val ALL = "all"

        /** A setup from [encode]'s text; parts missing or not understood keep their defaults, names not known are left out. */
        fun decode(text: String): PracticeSetup {
            val parts = text.split(';').mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }.toMap()
            fun names(key: String) = parts[key]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
            val defaults = PracticeSetup()
            return PracticeSetup(
                types = names("types")?.mapNotNull { name -> ExerciseType.entries.firstOrNull { it.name == name } }?.toSet() ?: defaults.types,
                responses = names("responses")?.mapNotNull { name -> PracticeResponse.entries.firstOrNull { it.name == name } }?.toSet() ?: defaults.responses,
                contexts = names("contexts")?.mapNotNull { name -> PracticeContext.entries.firstOrNull { it.name == name } }?.toSet() ?: defaults.contexts,
                length = when (val length = parts["length"]) {
                    null -> defaults.length
                    ALL -> null
                    else -> length.toIntOrNull()?.takeIf { it > 0 } ?: defaults.length
                },
            )
        }

        /**
         * The setup that stands for the kinds of question earlier versions named: GAP_CHOICE,
         * GAP_TYPED, NEW_CONTEXT, HEAR_CHOOSE and DICTATION; every kind when none is named.
         */
        fun ofKinds(names: Collection<String>): PracticeSetup {
            if (names.isEmpty()) return PracticeSetup()
            val types = names.mapNotNull {
                when (it) {
                    "GAP_CHOICE", "GAP_TYPED", "NEW_CONTEXT" -> ExerciseType.GAP
                    "HEAR_CHOOSE" -> ExerciseType.HEAR_WORD
                    "DICTATION" -> ExerciseType.DICTATION
                    else -> null
                }
            }.toSet()
            val responses = names.mapNotNull {
                when (it) {
                    "GAP_CHOICE", "NEW_CONTEXT", "HEAR_CHOOSE" -> PracticeResponse.CHOOSE
                    "GAP_TYPED", "DICTATION" -> PracticeResponse.TYPE
                    else -> null
                }
            }.toSet()
            return PracticeSetup(types = types, responses = responses)
        }
    }
}

/** One question of a page's practice. */
data class Exercise(
    val type: ExerciseType,
    val response: PracticeResponse,
    val context: PracticeContext,
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
    const val MAX_QUESTIONS = PracticeSetup.DEFAULT_LENGTH
    private const val OPTIONS = 4

    /** Sentences of at most this many words are typed whole in a dictation. */
    private const val SHORT_SENTENCE_WORDS = 6

    /** Sentences from elsewhere asked at most, each for a different word. */
    private const val NEW_CONTEXTS = 2

    /** How many questions [words] words get with [kinds] kinds of question, before any sentence from elsewhere: about two a word, at most [length]. */
    fun questionCount(words: Int, kinds: Int, length: Int? = MAX_QUESTIONS): Int =
        if (words == 0 || kinds == 0) 0 else (if (length == null) words * kinds else maxOf(words * 2, 5).coerceAtMost(words * kinds).coerceAtMost(length))

    /**
     * About two questions a word, [PracticeSetup.length] at most, going round the words so that a
     * word's questions are apart, and round the setup's questions so that each comes up. Where
     * gap questions may use sentences from elsewhere, words with an [examples] sentence get a gap
     * question in that new context at the end. [pageWords] are the other words of the page, for
     * wrong choices.
     */
    fun build(
        words: List<PracticeWord>,
        pageWords: List<String>,
        examples: Map<String, PracticeExample> = emptyMap(),
        random: Random = Random.Default,
        setup: PracticeSetup = PracticeSetup(),
        /** Meanings of other words, for wrong choices of meaning beside those of the [words]. */
        meanings: List<String> = emptyList(),
        /** The translations of the words' sentences, by sentence, for the questions asked through them. */
        translations: Map<String, String> = emptyMap(),
    ): List<Exercise> {
        val targets = words.distinctBy { it.text.lowercase() }.shuffled(random)
        val rotation = setup.rotation
        if (targets.isEmpty() || rotation.isEmpty()) return emptyList()
        val original = PracticeContext.ORIGINAL in setup.contexts
        // A sentence from elsewhere is asked as a gap, chosen where choosing is asked for, else typed.
        val gapResponse = rotation.firstOrNull { it.first == ExerciseType.GAP }?.second
        val inNewContext = if (gapResponse != null && PracticeContext.EXAMPLE in setup.contexts) {
            targets.filter { it.text.lowercase() in examples }.take(if (original) NEW_CONTEXTS else setup.length ?: targets.size)
        } else {
            emptyList()
        }
        val room = setup.length?.let { it - inNewContext.size }
        val rotated = if (original) questionCount(targets.size, rotation.size, room).coerceAtLeast(0) else 0
        val exercises = mutableListOf<Exercise>()
        for (k in 0 until rotated) {
            val word = targets[k % targets.size]
            // Each round starts one question further on, so a word meets a different one every time;
            // a question the word cannot be asked gives way to the next one that it can.
            val start = (k % targets.size + k / targets.size) % rotation.size
            exercises += (0 until rotation.size).firstNotNullOfOrNull { step ->
                val (type, response) = rotation[(start + step) % rotation.size]
                exercise(type, response, word, targets, pageWords, meanings, translations, random)
            } ?: continue
        }
        for (word in inNewContext) {
            val example = examples.getValue(word.text.lowercase())
            exercises += Exercise(
                type = ExerciseType.GAP,
                response = gapResponse!!,
                context = PracticeContext.EXAMPLE,
                word = example.sentence.substring(example.range),
                wordTranslation = word.translation,
                sentence = example.sentence,
                range = example.range,
                sentenceTranslation = example.translation,
                options = if (gapResponse == PracticeResponse.CHOOSE) wordChoices(word, targets, pageWords, random) else emptyList(),
                answer = word.text,
            )
        }
        return exercises
    }

    /** The question of [type] on [word], or null when the word cannot be asked it. */
    private fun exercise(
        type: ExerciseType,
        response: PracticeResponse,
        word: PracticeWord,
        targets: List<PracticeWord>,
        pageWords: List<String>,
        meanings: List<String>,
        translations: Map<String, String>,
        random: Random,
    ): Exercise? {
        val base = Exercise(type, response, PracticeContext.ORIGINAL, word.text, word.translation, word.sentence, word.range, answer = word.text)
        return when (type) {
            ExerciseType.GAP -> if (response == PracticeResponse.CHOOSE) base.copy(options = wordChoices(word, targets, pageWords, random)) else base
            ExerciseType.MEANING -> {
                val meaning = word.translation?.trim().orEmpty()
                val wrong = meaningChoices(meaning, targets.filter { it !== word }.mapNotNull { it.translation }, meanings, random)
                if (meaning.isEmpty() || wrong.size < OPTIONS - 1) null else base.copy(options = (wrong + meaning).shuffled(random), answer = meaning)
            }
            ExerciseType.HEAR_WORD -> {
                val meaning = word.translation?.trim().orEmpty()
                val others = targets.filter { it !== word }.mapNotNull { it.translation?.trim()?.takeIf(String::isNotEmpty) }
                    .distinctBy { it.lowercase() }.filter { !it.equals(meaning, ignoreCase = true) }
                if (meaning.isNotEmpty() && others.size >= OPTIONS - 1) {
                    base.copy(options = (others.shuffled(random).take(OPTIONS - 1) + meaning).shuffled(random), answer = meaning, spoken = word.text)
                } else {
                    base.copy(options = wordChoices(word, targets, pageWords, random), spoken = word.text)
                }
            }
            ExerciseType.HEAR_SENTENCE -> {
                if (word.translation.isNullOrBlank()) return null
                val options = if (response == PracticeResponse.CHOOSE) sentenceChoices(word, targets, pageWords, random) else emptyList()
                base.copy(options = options, spoken = word.sentence)
            }
            ExerciseType.TRANSLATION -> {
                val translation = translations[word.sentence]?.trim()?.takeIf { it.isNotEmpty() } ?: return null
                base.copy(sentenceTranslation = translation, options = if (response == PracticeResponse.CHOOSE) wordChoices(word, targets, pageWords, random) else emptyList())
            }
            ExerciseType.DICTATION -> {
                val short = word.sentence.split(Regex("""\s+""")).count { part -> part.any { it.isLetterOrDigit() } } <= SHORT_SENTENCE_WORDS
                if (short) base.copy(answer = word.sentence, spoken = word.sentence, wholeSentence = true) else base.copy(spoken = word.sentence)
            }
        }
    }

    /**
     * Three wrong meanings for [meaning]: those of the other words [practised] first, then from
     * [more]; none twice, and none sharing a word with the right one, which could read as right
     * too ("wallet" beside "my wallet" is left out, as is "home" beside "at home").
     */
    private fun meaningChoices(meaning: String, practised: List<String>, more: List<String>, random: Random): List<String> {
        val own = contentWords(meaning)
        val fits = { text: String -> text.isNotEmpty() && !text.equals(meaning, ignoreCase = true) && contentWords(text).none { it in own } }
        return (practised.map { it.trim() }.filter(fits).shuffled(random) + more.map { it.trim() }.filter(fits).shuffled(random))
            .distinctBy { it.lowercase() }.take(OPTIONS - 1)
    }

    /** A meaning's words of three letters or more, lower-cased: what two meanings share when they are alike. */
    private fun contentWords(text: String): Set<String> =
        text.lowercase().split(Regex("""[^\p{L}\p{N}]+""")).filter { it.length >= 3 }.toSet()

    /**
     * The word among other words of its own sentence, which was heard whole: telling them apart
     * takes understanding what was heard. Too short a sentence is filled up from the page.
     */
    private fun sentenceChoices(word: PracticeWord, targets: List<PracticeWord>, pageWords: List<String>, random: Random): List<String> {
        val own = word.text.lowercase()
        val heard = word.sentence.split(Regex("""[^\p{L}\p{N}'’-]+""")).filter { part -> part.length > 1 && part.any { it.isLetter() } }
            .distinctBy { it.lowercase() }.filter { it.lowercase() != own }.shuffled(random).take(OPTIONS - 1)
        val more = wordChoices(word, targets, pageWords, random).filter { it.lowercase() != own && heard.none { h -> h.equals(it, ignoreCase = true) } }
        return ((heard + more).take(OPTIONS - 1) + word.text).shuffled(random)
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
