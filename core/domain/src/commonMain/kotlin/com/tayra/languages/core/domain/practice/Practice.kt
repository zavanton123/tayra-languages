package com.tayra.languages.core.domain.practice

import kotlin.random.Random

/** A word being learned, where it stands in a sentence of the page. */
data class PracticeWord(
    val text: String,
    val translation: String?,
    val sentence: String,
    /** Where the word stands in [sentence]. */
    val range: IntRange,
    /** The saved term, for the sentences kept for it; null for a word not saved. */
    val termId: Long? = null,
) {
    /** The word as words are told apart: lowercase. */
    val key: String get() = text.lowercase()
}

/** A sentence from elsewhere that uses a word of the page; [credit] names a source that asks to be named. */
data class PracticeExample(val sentence: String, val range: IntRange, val translation: String?, val credit: String? = null)

/** The skill a type of question trains; the practice setup groups the types by it. */
enum class PracticeSkill { READING, LISTENING, TRANSLATION, WORD_FORMS }

/** A word's family: the form a dictionary lists it under, and all its forms, lowercase. */
data class WordFamily(val lemma: String, val forms: Set<String>)

/** How a question is answered: choosing among four, typing it, or saying it. */
enum class PracticeResponse { CHOOSE, TYPE, SPEAK }

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
    GAP(PracticeSkill.READING, setOf(PracticeResponse.CHOOSE, PracticeResponse.TYPE, PracticeResponse.SPEAK)),

    /** The sentence whole, the word marked: choose what the word means. Needs the word's translation. */
    MEANING(PracticeSkill.READING, setOf(PracticeResponse.CHOOSE)),

    /** Up to five words and their meanings, to match up; asked once, at the start, of words with meanings that cannot be taken for each other. */
    MATCHING(PracticeSkill.READING, setOf(PracticeResponse.CHOOSE)),

    /** The word is read aloud: choose its meaning, or the word itself when meanings are missing. */
    HEAR_WORD(PracticeSkill.LISTENING, setOf(PracticeResponse.CHOOSE)),

    /**
     * The sentence is read aloud, its text hidden, and the word's meaning given: choose the word
     * among the sentence's other words, or type it. Needs the word's translation.
     */
    HEAR_SENTENCE(PracticeSkill.LISTENING, setOf(PracticeResponse.CHOOSE, PracticeResponse.TYPE, PracticeResponse.SPEAK)),

    /** The sentence is read aloud: type the missing word, or all of a short sentence. */
    DICTATION(PracticeSkill.LISTENING, setOf(PracticeResponse.TYPE)),

    /** The word is read aloud alone: pick it among words spelled nearly like it (its other forms, or others). Needs three such words. */
    SOUND_ALIKE(PracticeSkill.LISTENING, setOf(PracticeResponse.CHOOSE)),

    /** The sentence is read aloud: say it back; right when most of its words are heard. */
    SHADOW(PracticeSkill.LISTENING, setOf(PracticeResponse.SPEAK)),

    /** The sentence's translation over the sentence with the word blanked out: choose or type the word. Needs the translation. */
    TRANSLATION(PracticeSkill.TRANSLATION, setOf(PracticeResponse.CHOOSE, PracticeResponse.TYPE, PracticeResponse.SPEAK)),

    /** The sentence's translation, and the sentence's words shuffled: put them in order. Needs the translation and a sentence of 3 to 8 words. */
    BUILD(PracticeSkill.TRANSLATION, setOf(PracticeResponse.CHOOSE)),

    /**
     * The sentence with the word blanked out and its translation as the clue: choose, build or type
     * the form of the word that fits. Needs three other forms of the word and the translation.
     */
    FORM(PracticeSkill.WORD_FORMS, setOf(PracticeResponse.CHOOSE, PracticeResponse.TYPE)),
}

/**
 * What a practice asks: the [types] of question, each answered in those of the [responses] it
 * allows, in sentences from the [contexts], [length] questions at most (as many as every type
 * makes for every word when null).
 */
data class PracticeSetup(
    val types: Set<ExerciseType> = setOf(ExerciseType.GAP, ExerciseType.HEAR_WORD, ExerciseType.DICTATION),
    val responses: Set<PracticeResponse> = setOf(PracticeResponse.CHOOSE, PracticeResponse.TYPE),
    val contexts: Set<PracticeContext> = PracticeContext.entries.toSet(),
    val length: Int? = DEFAULT_LENGTH,
    /** Each word is answered the ways its practice level calls for, among the [responses]; otherwise they are mixed. */
    val adaptive: Boolean = false,
    /** Which of the words are practised. */
    val words: WordChoice = WordChoice.ALL,
) {
    /**
     * This setup where nothing can be said aloud: answers said are typed instead, and sentences
     * repeated after the voice are written down from it.
     */
    fun withoutSpeaking(): PracticeSetup =
        if (PracticeResponse.SPEAK !in responses && ExerciseType.SHADOW !in types) this
        else copy(
            responses = (responses - PracticeResponse.SPEAK).ifEmpty { setOf(PracticeResponse.TYPE) },
            types = (types - ExerciseType.SHADOW).ifEmpty { setOf(ExerciseType.DICTATION) },
        )

    /** The questions asked, in the order they come round: those answered by choosing first, then those typed. */
    val rotation: List<Pair<ExerciseType, PracticeResponse>> =
        PracticeResponse.entries.filter { it in responses }.flatMap { response -> ExerciseType.entries.filter { it in types && response in it.responses }.map { it to response } }

    /** The setup as text, for a route or the settings: `types=GAP,DICTATION;responses=CHOOSE;contexts=ORIGINAL;length=10`. */
    fun encode(): String = listOf(
        "types=" + ExerciseType.entries.filter { it in types }.joinToString(",") { it.name },
        "responses=" + PracticeResponse.entries.filter { it in responses }.joinToString(",") { it.name },
        "contexts=" + PracticeContext.entries.filter { it in contexts }.joinToString(",") { it.name },
        "length=" + (length?.toString() ?: ALL),
        "adaptive=" + (if (adaptive) "1" else "0"),
        "words=" + words.name,
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
                // Likewise for one kept from before true or false was dropped.
                types = names("types")?.mapNotNull { name -> ExerciseType.entries.firstOrNull { it.name == name } }?.toSet()?.takeIf { it.isNotEmpty() } ?: defaults.types,
                // A setup kept from before letter tiles were dropped may have had no other way of answering.
                responses = names("responses")?.mapNotNull { name -> PracticeResponse.entries.firstOrNull { it.name == name } }?.toSet()?.takeIf { it.isNotEmpty() } ?: defaults.responses,
                contexts = names("contexts")?.mapNotNull { name -> PracticeContext.entries.firstOrNull { it.name == name } }?.toSet() ?: defaults.contexts,
                length = when (val length = parts["length"]) {
                    null -> defaults.length
                    ALL -> null
                    else -> length.toIntOrNull()?.takeIf { it > 0 } ?: defaults.length
                },
                adaptive = parts["adaptive"]?.let { it == "1" } ?: defaults.adaptive,
                words = parts["words"]?.let { name -> WordChoice.entries.firstOrNull { it.name == name } } ?: defaults.words,
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
    /** The letters the answer is built from, shuffled, with a couple that do not belong; empty unless built from tiles. */
    val tiles: List<String> = emptyList(),
    /** The right choice, or what is to be typed. */
    val answer: String,
    /** What is read aloud, for the listening kinds. */
    val spoken: String? = null,
    /** The answer is the whole sentence, not one word. */
    val wholeSentence: Boolean = false,
    /** The word practised as it was saved or read; a sentence from elsewhere may use another form of it, [word]. */
    val practised: String = word,
    /** The form the word is listed under, when a question asks for the right form of it. */
    val lemma: String? = null,
    /** Words and their meanings to match up, for a matching question. */
    val pairs: List<Pair<String, String>> = emptyList(),
    /** The source of a sentence from elsewhere, when it asks to be named. */
    val credit: String? = null,
    /** The saved term practised, whose progress the answer moves; null for a word not saved. */
    val termId: Long? = null,
) {
    val isChoice: Boolean get() = options.isNotEmpty()
    val isListening: Boolean get() = spoken != null

    /** The sentence uses another form of the word practised. */
    val otherForm: Boolean get() = !word.equals(practised, ignoreCase = true)
}

/** Words to offer beside the right one: the page's other words, and words about as common as each word and looking like it. */
internal class WrongChoices(val pageWords: List<String>, val alikes: Map<String, List<String>> = emptyMap(), val families: Map<String, WordFamily> = emptyMap()) {
    /** The same, without [word] among the page's words. */
    fun without(word: String) = WrongChoices(pageWords.filter { !it.equals(word, ignoreCase = true) }, alikes, families)
}

/** Builds the questions for a page from the words being learned on it. */
object PracticeBuilder {
    const val MAX_QUESTIONS = PracticeSetup.DEFAULT_LENGTH
    private const val OPTIONS = 4

    /** How many letters apart a word heard alone may be from the words offered beside it. */
    private const val SOUND_ALIKE_DISTANCE = 3

    /** Pairs in a matching question: at most this many, and at least [MIN_PAIRS]. */
    private const val PAIRS = 5
    private const val MIN_PAIRS = 4

    /** Sentences of at most this many words are typed whole in a dictation. */
    private const val SHORT_SENTENCE_WORDS = 8

    /** The types a sentence from elsewhere can be asked in: those about the sentence, not the word alone or its meaning. */
    private val EXAMPLE_TYPES = setOf(ExerciseType.GAP, ExerciseType.HEAR_SENTENCE, ExerciseType.TRANSLATION, ExerciseType.BUILD, ExerciseType.DICTATION, ExerciseType.SHADOW)

    /** Sentences built from their words have this many words at least, and [MAX_BUILT] at most. */
    private const val MIN_BUILT = 3
    private const val MAX_BUILT = 8

    /** How many questions [words] words get with [kinds] kinds of question, before any sentence from elsewhere: about two a word, at most [length]. */
    fun questionCount(words: Int, kinds: Int, length: Int? = MAX_QUESTIONS): Int =
        if (words == 0 || kinds == 0) 0 else (if (length == null) words * kinds else maxOf(words * 2, 5).coerceAtMost(words * kinds).coerceAtMost(length))

    /**
     * About two questions a word, [PracticeSetup.length] at most, going round the words so that a
     * word's questions are apart, and round the setup's questions so that each comes up. Where the
     * setup takes sentences from elsewhere, a word's second round asks it in one of its [examples]
     * (by the word's key), each used once, perhaps in another form of the word; with sentences
     * from elsewhere only, only words that have some are asked. [pageWords] are the other words of
     * the page, for wrong choices.
     */
    fun build(
        words: List<PracticeWord>,
        pageWords: List<String>,
        examples: Map<String, List<PracticeExample>> = emptyMap(),
        random: Random = Random.Default,
        setup: PracticeSetup = PracticeSetup(),
        /** Meanings of other words, for wrong choices of meaning beside those of the [words]. */
        meanings: List<String> = emptyList(),
        /** The translations of the words' sentences, by sentence, for the questions asked through them. */
        translations: Map<String, String> = emptyMap(),
        /** Words about as common as each word and looking like it, by the word's key, for wrong choices. */
        alikes: Map<String, List<String>> = emptyMap(),
        /** Each word's family, by the word's key, for the questions on its forms. */
        families: Map<String, WordFamily> = emptyMap(),
        /** Each word's practice level, by the word's key, for a setup that adapts to it; a word not in it is at the first. */
        levels: Map<String, Int> = emptyMap(),
    ): List<Exercise> {
        val choices = WrongChoices(pageWords, alikes, families)
        val targets = words.distinctBy { it.key }.shuffled(random)
        // Matching is one question on several words, asked first; the rest go round the words one by one.
        val matching = if (ExerciseType.MATCHING in setup.types) matching(targets, random) else null
        val rotation = setup.rotation.filter { it.first != ExerciseType.MATCHING }
        val original = PracticeContext.ORIGINAL in setup.contexts
        val elsewhere = if (PracticeContext.EXAMPLE in setup.contexts) examples else emptyMap()
        val asked = if (original) targets else targets.filter { elsewhere[it.key].orEmpty().isNotEmpty() }
        if (asked.isEmpty() || rotation.isEmpty()) return listOfNotNull(matching)
        val used = mutableMapOf<String, Int>()
        val exercises = listOfNotNull(matching).toMutableList()
        for (k in 0 until questionCount(asked.size, rotation.size, setup.length?.minus(exercises.size))) {
            val word = asked[k % asked.size]
            // Each round starts one question further on, so a word meets a different one every time;
            // a question the word cannot be asked gives way to the next one that it can.
            val start = (k % asked.size + k / asked.size) % rotation.size
            val all = (0 until rotation.size).map { rotation[(start + it) % rotation.size] }
            // Adapting, the word is answered the ways its level calls for, where the setup has them; otherwise any way.
            val wanted = if (setup.adaptive) WordProgress.LEVELS[(levels[word.key] ?: 0).coerceIn(0, WordProgress.TOP)].responses else null
            val steps = wanted?.let { ways -> all.filter { it.second in ways }.ifEmpty { all } } ?: all
            val kept = elsewhere[word.key].orEmpty()
            val next = used[word.key] ?: 0
            val inExample = next < kept.size && (!original || (k / asked.size) % 2 == 1)
            val exercise = if (inExample) {
                steps.firstNotNullOfOrNull { (type, response) -> exampleExercise(type, response, word, kept[next], targets, choices, meanings, random) }
                    ?.also { used[word.key] = next + 1 }
            } else {
                null
            } ?: if (original) steps.firstNotNullOfOrNull { (type, response) -> exercise(type, response, word, targets, choices, meanings, translations, random) } else null
            exercises += exercise ?: continue
        }
        return exercises
    }

    /**
     * A matching question on up to [PAIRS] of [words] with meanings, none sharing a word with
     * another's, so no meaning could be taken for another; null with fewer than [MIN_PAIRS].
     */
    fun matching(words: List<PracticeWord>, random: Random = Random.Default): Exercise? {
        val picked = mutableListOf<PracticeWord>()
        for (word in words.shuffled(random)) {
            val meaning = word.translation?.trim().orEmpty()
            if (meaning.isEmpty()) continue
            if (picked.any { other -> other.key == word.key || other.translation!!.trim().equals(meaning, ignoreCase = true) || contentWords(other.translation).any { it in contentWords(meaning) } }) continue
            picked += word
            if (picked.size == PAIRS) break
        }
        if (picked.size < MIN_PAIRS) return null
        val first = picked.first()
        return Exercise(
            ExerciseType.MATCHING, PracticeResponse.CHOOSE, PracticeContext.ORIGINAL, first.text, first.translation, first.sentence, first.range,
            answer = "", pairs = picked.map { it.text to it.translation!!.trim() },
        )
    }

    /** The question of [type] on [word] in [example], a sentence from elsewhere that may use another form of it. */
    private fun exampleExercise(
        type: ExerciseType,
        response: PracticeResponse,
        word: PracticeWord,
        example: PracticeExample,
        targets: List<PracticeWord>,
        pool: WrongChoices,
        meanings: List<String>,
        random: Random,
    ): Exercise? {
        if (type !in EXAMPLE_TYPES) return null
        val form = PracticeWord(example.sentence.substring(example.range), word.translation, example.sentence, example.range, word.termId)
        val translations = example.translation?.let { mapOf(example.sentence to it) }.orEmpty()
        // The word as saved is no wrong choice beside the form the sentence uses.
        val others = targets.filter { it.key != word.key }
        return exercise(type, response, form, others, pool.without(word.text), meanings, translations, random)
            ?.copy(context = PracticeContext.EXAMPLE, practised = word.text, sentenceTranslation = example.translation, credit = example.credit)
    }

    /**
     * Whether [word] can be asked a question of [type], answered as [response] or the type's first
     * way: what the setup dialog counts, by the same rules the questions are built by.
     */
    fun canAsk(
        type: ExerciseType,
        word: PracticeWord,
        words: List<PracticeWord>,
        meanings: List<String> = emptyList(),
        translations: Map<String, String> = emptyMap(),
        families: Map<String, WordFamily> = emptyMap(),
        response: PracticeResponse = type.responses.first(),
    ): Boolean = exercise(type, response, word, words, WrongChoices(emptyList(), families = families), meanings, translations, Random(0)) != null

    /** The question of [type] on [word], or null when the word cannot be asked it. */
    private fun exercise(
        type: ExerciseType,
        response: PracticeResponse,
        word: PracticeWord,
        targets: List<PracticeWord>,
        pool: WrongChoices,
        meanings: List<String>,
        translations: Map<String, String>,
        random: Random,
    ): Exercise? {
        val base = Exercise(type, response, PracticeContext.ORIGINAL, word.text, word.translation, word.sentence, word.range, answer = word.text, termId = word.termId)
        return when (type) {
            ExerciseType.MATCHING -> null
            ExerciseType.SHADOW -> base.copy(spoken = word.sentence, answer = word.sentence, wholeSentence = true)
            ExerciseType.SOUND_ALIKE -> {
                val own = word.key
                val near = (pool.families[own]?.forms.orEmpty() + pool.alikes[own].orEmpty().map { it.lowercase() })
                    .filter { it != own && editDistance(it, own) <= SOUND_ALIKE_DISTANCE }.distinct()
                if (near.size < OPTIONS - 1) return null
                val written = near.shuffled(random).take(OPTIONS - 1).map { if (word.text.first().isUpperCase()) it.replaceFirstChar { c -> c.uppercase() } else it }
                base.copy(options = (written + word.text).shuffled(random), spoken = word.text)
            }
            ExerciseType.GAP -> answered(base, response, word, targets, pool, random)
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
                    base.copy(options = wordChoices(word, targets, pool, random), spoken = word.text)
                }
            }
            ExerciseType.HEAR_SENTENCE -> {
                if (word.translation.isNullOrBlank()) return null
                val heard = base.copy(spoken = word.sentence)
                if (response == PracticeResponse.CHOOSE) heard.copy(options = sentenceChoices(word, targets, pool, random)) else answered(heard, response, word, targets, pool, random)
            }
            ExerciseType.TRANSLATION -> {
                val translation = translations[word.sentence]?.trim()?.takeIf { it.isNotEmpty() } ?: return null
                answered(base.copy(sentenceTranslation = translation), response, word, targets, pool, random)
            }
            ExerciseType.BUILD -> {
                val translation = translations[word.sentence]?.trim()?.takeIf { it.isNotEmpty() } ?: return null
                val parts = word.sentence.trim().split(Regex("""\s+"""))
                if (parts.size !in MIN_BUILT..MAX_BUILT) return null
                // Shuffled so they never stand in their own order already.
                var tiles = parts.shuffled(random)
                while (tiles == parts) tiles = parts.shuffled(random)
                base.copy(sentenceTranslation = translation, tiles = tiles, answer = word.sentence.trim(), wholeSentence = true)
            }
            ExerciseType.FORM -> {
                val family = pool.families[word.key] ?: return null
                val translation = translations[word.sentence]?.trim()?.takeIf { it.isNotEmpty() } ?: return null
                val others = family.forms.filter { it != word.key }
                if (others.size < OPTIONS - 1) return null
                val asked = base.copy(sentenceTranslation = translation, lemma = family.lemma)
                if (response == PracticeResponse.CHOOSE) {
                    // The other forms written as the word is, capital and all.
                    val written = others.shuffled(random).take(OPTIONS - 1).map { if (word.text.first().isUpperCase()) it.replaceFirstChar { c -> c.uppercase() } else it }
                    asked.copy(options = (written + word.text).shuffled(random))
                } else {
                    answered(asked, response, word, targets, pool, random)
                }
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

    /** How many letters must be changed, added or removed to turn [a] into [b]. */
    private fun editDistance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) current[j + 1] = minOf(previous[j + 1] + 1, current[j] + 1, previous[j] + if (a[i] == b[j]) 0 else 1)
            previous = current
        }
        return previous[b.length]
    }

    /** A meaning's words of three letters or more, lower-cased: what two meanings share when they are alike. */
    private fun contentWords(text: String): Set<String> =
        text.lowercase().split(Regex("""[^\p{L}\p{N}]+""")).filter { it.length >= 3 }.toSet()

    /** [question] answered as [response]: with the word among others to choose from, or nothing to type or say it. */
    private fun answered(question: Exercise, response: PracticeResponse, word: PracticeWord, targets: List<PracticeWord>, pool: WrongChoices, random: Random): Exercise = when (response) {
        PracticeResponse.CHOOSE -> question.copy(options = wordChoices(word, targets, pool, random))
        // Said, the learner says the whole sentence; it counts when the word is heard in it.
        PracticeResponse.TYPE, PracticeResponse.SPEAK -> question
    }

    /**
     * The word among other words of its own sentence, which was heard whole: telling them apart
     * takes understanding what was heard. Too short a sentence is filled up from the page.
     */
    private fun sentenceChoices(word: PracticeWord, targets: List<PracticeWord>, pool: WrongChoices, random: Random): List<String> {
        val own = word.text.lowercase()
        val heard = word.sentence.split(Regex("""[^\p{L}\p{N}'’-]+""")).filter { part -> part.length > 1 && part.any { it.isLetter() } }
            .distinctBy { it.lowercase() }.filter { it.lowercase() != own }.shuffled(random).take(OPTIONS - 1)
        val more = wordChoices(word, targets, pool, random).filter { it.lowercase() != own && heard.none { h -> h.equals(it, ignoreCase = true) } }
        return ((heard + more).take(OPTIONS - 1) + word.text).shuffled(random)
    }

    /** The word among other words of the page: the other words being learned first, then any others. */
    private fun wordChoices(word: PracticeWord, targets: List<PracticeWord>, pool: WrongChoices, random: Random): List<String> {
        val own = word.key
        // One about as common as the word and looking like it, so the choice takes knowing the word.
        val alike = pool.alikes[own].orEmpty().filter { it.lowercase() != own }.shuffled(random).take(1)
        val learning = targets.map { it.text }.filter { it.lowercase() != own }.shuffled(random)
        val others = pool.pageWords.filter { other -> other.length > 1 && other.any { it.isLetter() } }.shuffled(random)
        val wrong = (alike + learning + others).distinctBy { it.lowercase() }.filter { it.lowercase() != own }.take(OPTIONS - 1)
        return (wrong + word.text).shuffled(random)
    }
}

/** How a word of a typed sentence compares with the word expected there. */
enum class WordResult { RIGHT, ACCENT, WRONG, MISSING }

/** A [word] of the expected sentence, how it was typed ([typed], null when missing), and how that compares. */
data class WordCheck(val word: String, val result: WordResult, val typed: String? = null)

/** How a typed answer compares with what was expected. */
enum class TypedResult {
    CORRECT,

    /** Right but for accents: counted as right, with the spelling shown. */
    ALMOST,

    /** One letter off inside a word of [PracticeChecker.TYPO_LENGTH] letters or more: counted as right, with the spelling shown. */
    TYPO,
    WRONG,
}

/**
 * Compares typed answers: case, punctuation and extra spaces do not count; accents nearly do not,
 * and neither does one slip of a letter inside a longer word. A difference at a word's end is no
 * slip: there words change their form ("casa", "casas").
 */
object PracticeChecker {
    const val TYPO_LENGTH = 5

    fun check(expected: String, typed: String): TypedResult {
        val wanted = normalize(expected)
        val given = normalize(typed)
        return when {
            given.isEmpty() -> TypedResult.WRONG
            given == wanted -> TypedResult.CORRECT
            unaccented(given) == unaccented(wanted) -> TypedResult.ALMOST
            isSlip(unaccented(wanted), unaccented(given)) -> TypedResult.TYPO
            else -> TypedResult.WRONG
        }
    }

    /**
     * One letter wrong, missing, extra, or two swapped, in a single word long enough to be sure of,
     * and not within its last two letters.
     */
    private fun isSlip(wanted: String, given: String): Boolean {
        if (' ' in wanted || ' ' in given || maxOf(wanted.length, given.length) < TYPO_LENGTH) return false
        val first = wanted.zip(given).indexOfFirst { (a, b) -> a != b }.let { if (it < 0) minOf(wanted.length, given.length) else it }
        if (first >= maxOf(wanted.length, given.length) - 2) return false
        val a = wanted.substring(first)
        val b = given.substring(first)
        return when {
            a.length == b.length -> a.drop(1) == b.drop(1) || (a.length >= 2 && a[0] == b[1] && a[1] == b[0] && a.drop(2) == b.drop(2))
            a.length == b.length + 1 -> a.drop(1) == b
            b.length == a.length + 1 -> b.drop(1) == a
            else -> false
        }
    }

    /**
     * [typed] against [expected] word by word, in [expected]'s words: each right, right but for
     * accents, wrong (with what was typed in its place), or missing. Words typed in the right
     * order are lined up first; what is left between them is paired in order.
     */
    fun compareWords(expected: String, typed: String): List<WordCheck> {
        val wanted = expected.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
        val given = typed.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
        val a = wanted.map { unaccented(normalize(it)) }
        val b = given.map { unaccented(normalize(it)) }
        // The longest run of words typed in the right order, found from the end.
        val lcs = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) for (j in b.indices.reversed()) {
            lcs[i][j] = if (a[i] == b[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
        val checks = ArrayList<WordCheck>(wanted.size)
        var i = 0
        var j = 0
        val pendingWanted = ArrayList<Int>()
        val pendingGiven = ArrayList<Int>()
        fun flush() {
            pendingWanted.forEachIndexed { k, w ->
                val g = pendingGiven.getOrNull(k)
                checks += if (g == null) WordCheck(wanted[w], WordResult.MISSING) else WordCheck(wanted[w], WordResult.WRONG, given[g])
            }
            pendingWanted.clear()
            pendingGiven.clear()
        }
        while (i < a.size && j < b.size) {
            when {
                a[i] == b[j] -> {
                    flush()
                    checks += WordCheck(wanted[i], if (normalize(wanted[i]) == normalize(given[j])) WordResult.RIGHT else WordResult.ACCENT, given[j])
                    i++
                    j++
                }
                lcs[i + 1][j] >= lcs[i][j + 1] -> pendingWanted += i++
                else -> pendingGiven += j++
            }
        }
        while (i < a.size) pendingWanted += i++
        while (j < b.size) pendingGiven += j++
        flush()
        return checks
    }

    /**
     * What was [heard] said aloud, against the [word] the sentence was to hold, or the sentence
     * [expected] repeated whole when there is no word: right when the word is among the words
     * heard, or when every word of the sentence was; almost when [SHADOW_SHARE] of them were. The
     * recogniser writes what it hears, so accents are not held against the learner.
     */
    fun checkSpoken(heard: String, word: String?, expected: String): TypedResult {
        val said = unaccented(normalize(heard))
        if (said.isEmpty()) return TypedResult.WRONG
        if (word != null) return if (" ${unaccented(normalize(word))} " in " $said ") TypedResult.CORRECT else TypedResult.WRONG
        val words = compareWords(expected, heard)
        val share = words.count { it.result == WordResult.RIGHT || it.result == WordResult.ACCENT }.toDouble() / words.size.coerceAtLeast(1)
        return when {
            share >= 1.0 -> TypedResult.CORRECT
            share >= SHADOW_SHARE -> TypedResult.ALMOST
            else -> TypedResult.WRONG
        }
    }

    /** The share of a repeated sentence's words that must be heard for it to count. */
    const val SHADOW_SHARE = 0.8

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
