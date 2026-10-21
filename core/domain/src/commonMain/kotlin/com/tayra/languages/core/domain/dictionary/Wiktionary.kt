package com.tayra.languages.core.domain.dictionary

/**
 * What a Wiktionary edition has on a word in one language, as WiktApi serves it: [edition] is the
 * Wiktionary whose language the glosses are in, [word] the page's title.
 */
data class WiktionaryPage(val word: String, val edition: String, val languageCode: String, val entries: List<WiktionaryEntry>) {
    /** The lemmas the word is a form of, in order, when it is no headword of its own. */
    val lemmas: List<String> get() = entries.flatMap { it.senses }.flatMap { it.formOf }.map { it.title }.distinct()

    /** True when one of the entries has meanings of its own rather than only pointers to lemmas. */
    val isHeadword: Boolean get() = entries.any { entry -> entry.senses.any { it.formOf.isEmpty() } }

    /** The lemma to record as the term's parent: the only one the word is a form of, when it has no meanings of its own. */
    val parentSuggestion: String? get() = if (isHeadword) null else lemmas.singleOrNull()

    /** Each form-of sense once, "third-person plural present indicative of comer", with the lemmas it names. */
    val formOf: List<WiktionarySense> get() = entries.flatMap { it.senses }.filter { it.formOf.isNotEmpty() }.distinctBy { it.text }

    /** Synonyms the senses give, each once. */
    val synonyms: List<String> get() = entries.flatMap { it.senses }.flatMap { it.synonyms }.filter { it != word }.distinct()

    val pronunciations: List<WiktionaryPronunciation> get() = entries.flatMap { it.pronunciations }.distinct()

    val audio: List<WiktionaryAudio> get() = entries.flatMap { it.audio }.distinctBy { it.file }

    /** The page as dictionary entries: its meanings, examples and first pronunciation, and the lemmas its forms point to. */
    fun toLookup(): DictionaryLookup {
        val found = entries.mapNotNull { entry ->
            val senses = entry.senses.filter { it.formOf.isEmpty() && it.glosses.isNotEmpty() }
            if (senses.isEmpty()) return@mapNotNull null
            DictionaryEntry(
                word = word,
                pos = entry.pos,
                ipa = entry.pronunciations.firstOrNull()?.ipa,
                senses = senses.take(MAX_SENSES).map { sense ->
                    DictionarySense(listOf(sense.glosses.joinToString(" ")), sense.tags.filter { it !in HIDDEN_SENSE_TAGS }, sense.examples.take(MAX_EXAMPLES))
                },
            )
        }
        return DictionaryLookup(found, if (found.isEmpty()) lemmas else emptyList())
    }

    private companion object {
        const val MAX_SENSES = 8
        const val MAX_EXAMPLES = 2
        val HIDDEN_SENSE_TAGS = setOf("form-of", "no-gloss")
    }
}

/** One part of speech of a page: [pos] as kaikki names it ("noun", "verb", "adj"), empty when the page does not say. */
data class WiktionaryEntry(
    val pos: String,
    val senses: List<WiktionarySense>,
    val pronunciations: List<WiktionaryPronunciation> = emptyList(),
    val audio: List<WiktionaryAudio> = emptyList(),
    val forms: List<WiktionaryForm> = emptyList(),
)

/**
 * A meaning of an entry. A form-of sense names the lemmas it points to in [formOf]; its
 * glosses are the description, "inflection of ко́шка (kóška):" then "genitive singular".
 */
data class WiktionarySense(
    val glosses: List<String>,
    val tags: List<String> = emptyList(),
    val examples: List<DictionaryExample> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val formOf: List<WiktionaryLemma> = emptyList(),
) {
    val text: String get() = glosses.joinToString(" ")
}

/** A lemma as a form-of sense writes it ([shown], with stress marks) and the page it is on ([title]). */
data class WiktionaryLemma(val shown: String, val title: String)

/** An IPA transcription, with the regions or varieties it is for ("Brazil", "Portugal"). */
data class WiktionaryPronunciation(val ipa: String, val tags: List<String> = emptyList())

/** A recording an entry links: [file] is its name on Wikimedia Commons, [tags] the regions it is from. */
data class WiktionaryAudio(val file: String, val mp3Url: String?, val oggUrl: String?, val tags: List<String> = emptyList()) {
    val pageUrl: String get() = "https://commons.wikimedia.org/wiki/File:" + file.replace(' ', '_')
}

/** A form of the word with its grammatical tags; [source] names the table it comes from ("conjugation", "declension"), null for the head line's forms. */
data class WiktionaryForm(val form: String, val tags: List<String>, val source: String? = null)

/** The forms one part of speech of a word has, as WiktApi's forms endpoint gives them. */
data class WiktionaryForms(val pos: String, val forms: List<WiktionaryForm>)

/** A word a Wiktionary edition lists, as its search finds it. */
data class WiktionarySearchResult(val word: String, val pos: String)

/** Looks words up on Wiktionary through the network; every function answers empty or null when it cannot. */
interface OnlineWiktionary {
    /**
     * The page for [word] in the language with the app's code [languageCode], from the edition in
     * [nativeLanguage] when that edition has it, else from the English one.
     */
    suspend fun page(word: String, languageCode: String, nativeLanguage: String): WiktionaryPage?

    /** The forms of [word] by part of speech, from [edition]. */
    suspend fun forms(word: String, languageCode: String, edition: String): List<WiktionaryForms>

    /** Words of the language starting with [prefix], from [edition]. */
    suspend fun search(prefix: String, languageCode: String, edition: String): List<WiktionarySearchResult>
}

/** How the app's language codes and Wiktionary's editions meet. */
object WiktionaryCodes {
    /** The editions WiktApi serves, by their codes. */
    val editions: Set<String> = setOf("cs", "de", "el", "en", "es", "fr", "id", "it", "ja", "ko", "ku", "ms", "nl", "pl", "pt", "ru", "simple", "th", "tr", "vi", "zh")

    /** Wiktionary files these languages under another code: Serbo-Croatian, Bokmål, and Cantonese among Chinese. */
    private val codes = mapOf("sr" to "sh", "hr" to "sh", "bs" to "sh", "no" to "nb", "yue" to "zh")

    /** The English Wiktionary's headings, where they differ from the app's names, by Wiktionary code. */
    private val headings = mapOf(
        "sh" to "Serbo-Croatian", "nb" to "Norwegian Bokmål", "zh" to "Chinese", "fa" to "Persian",
        "aii" to "Assyrian Neo-Aramaic", "sl" to "Slovene",
    )

    fun languageCode(code: String): String = code.trim().lowercase().let { codes[it] ?: it }

    /** The editions to ask, in order: the native language's when there is one, then the English one. */
    fun editionsFor(nativeLanguage: String): List<String> =
        listOfNotNull(nativeLanguage.trim().lowercase().takeIf { it in editions && it != "simple" }, "en").distinct()

    /** The page on Wiktionary itself; the English edition's section of the language is named by [languageName], in English. */
    fun entryUrl(page: WiktionaryPage, languageName: String?): String {
        val title = page.word.replace(' ', '_')
        val heading = (headings[page.languageCode] ?: languageName?.substringBefore('(')?.trim())?.takeIf { page.edition == "en" && it.isNotEmpty() }
        return "https://${page.edition}.wiktionary.org/wiki/$title" + (heading?.let { "#" + it.replace(' ', '_') } ?: "")
    }

    /** [text] without the stress and length marks dictionaries add (ко́шка as кошка), lowercased, for comparing forms. */
    fun plain(text: String): String = text.filterNot { it in '̀'..'ͯ' }.lowercase().trim()
}

/** A detail for the back of a flashcard: a gender ([form] null), or a form named by its tags ("plural" → "Häuser"). */
data class WordHint(val tags: List<String>, val form: String? = null)

/** The few facts of a word worth a glance while reviewing it: its gender, its plural and a couple of verb forms. */
object WordHints {
    private val genders = listOf("masculine", "feminine", "neuter", "common")

    /** The verb forms worth knowing, each by the tags that may name it, the first a table has winning: the present, the past, the participle. */
    private val verbForms = listOf(
        listOf(listOf("first-person", "singular", "present", "indicative"), listOf("first-person", "singular", "present")),
        listOf(listOf("first-person", "singular", "preterite", "indicative"), listOf("first-person", "singular", "preterite"), listOf("first-person", "singular", "past")),
        listOf(listOf("past", "participle"), listOf("perfect", "participle")),
    )

    fun of(page: WiktionaryPage): List<WordHint> {
        // The entry with the most meanings is the word as it is mostly met: the verb comer, not the noun ("food").
        val entry = page.entries.maxByOrNull { e -> e.senses.count { it.formOf.isEmpty() } }?.takeIf { e -> e.senses.any { it.formOf.isEmpty() } } ?: return emptyList()
        val hints = mutableListOf<WordHint>()
        val tags = entry.senses.flatMap { it.tags } + entry.forms.filter { "canonical" in it.tags }.flatMap { it.tags }
        genders.firstOrNull { it in tags }?.let { hints += WordHint(listOf(it)) }
        val forms = InflectionTables.realForms(entry.forms)
        val plural = forms.firstOrNull { it.source == null && it.tags == listOf("plural") }
            ?: forms.firstOrNull { it.tags.toSet() == setOf("nominative", "plural") }
            ?: forms.firstOrNull { it.source == null && "plural" in it.tags && it.tags.size == 1 }
        if (plural != null && entry.pos != "verb") hints += WordHint(listOf("plural"), plural.form)
        if (entry.pos == "verb" || forms.any { it.source == "conjugation" }) {
            val picked = verbForms.mapNotNull { names ->
                names.firstNotNullOfOrNull { wanted ->
                    // A participle's table names its masculine singular too ("comido"); that one stands for it.
                    val form = forms.firstOrNull { it.tags.toSet() == wanted.toSet() }
                        ?: forms.firstOrNull { it.tags.containsAll(wanted) && (it.tags - wanted.toSet()).all { tag -> tag in DEFAULT_TAGS } }
                    form?.takeIf { WiktionaryCodes.plain(it.form) != WiktionaryCodes.plain(page.word) }?.let { WordHint(wanted, it.form) }
                }
            }
            hints += picked.distinctBy { it.form }.take(MAX_VERB_FORMS)
        }
        return hints.take(MAX_HINTS)
    }

    private val DEFAULT_TAGS = setOf("masculine", "singular")
    private const val MAX_VERB_FORMS = 2
    private const val MAX_HINTS = 4
}

/** The parent an inflected form gets: the offline dictionary's answer whenever it has one, else Wiktionary's. */
fun parentSuggestion(offline: DictionaryLookup, online: WiktionaryPage?): String? =
    if (!offline.isEmpty) offline.parentSuggestion else online?.parentSuggestion

/** The multi-word expressions among search results for [lemma]: those that have it as a word of their own. */
fun expressionsOf(results: List<WiktionarySearchResult>, lemma: String): List<String> {
    val wanted = WiktionaryCodes.plain(lemma)
    return results.map { it.word }.filter { word ->
        word.contains(' ') && word.split(' ', '-').any { WiktionaryCodes.plain(it) == wanted }
    }.distinct()
}
