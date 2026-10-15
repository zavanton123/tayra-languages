package com.tayra.languages.core.domain.dictionary

import com.tayra.languages.core.domain.morphology.Japanese

/** One meaning of a dictionary entry, with usage labels such as "comparative" or "abbreviation" and the dictionary's example sentences. */
data class DictionarySense(val glosses: List<String>, val tags: List<String> = emptyList(), val examples: List<DictionaryExample> = emptyList())

/** An example sentence a dictionary gives for a sense, with its translation when it has one. */
data class DictionaryExample(val text: String, val translation: String? = null)

/**
 * A headword in one part of speech; [isOwnEntry] is true when it is the looked-up word itself rather than a lemma of it.
 * [inflection] names the inflections that lead from this word to the looked-up one ("-て", "past"), in the
 * order they apply, when the language's rules found it rather than the pack's forms.
 */
data class DictionaryEntry(
    val word: String,
    val pos: String,
    val ipa: String?,
    val senses: List<DictionarySense>,
    val isOwnEntry: Boolean = true,
    val inflection: List<String> = emptyList(),
    /** How the word is read, for languages whose script does not say it: kana for Japanese, pinyin for Chinese. */
    val reading: String? = null,
    /** The word's Japanese pitch accents, one per reading and accent. */
    val pitch: List<PitchAccent> = emptyList(),
    /** The title of the imported dictionary the entry comes from; null for the app's own pack. */
    val dictionary: String? = null,
    /** How common the imported frequency dictionaries say the word is. */
    val frequencies: List<WordFrequency> = emptyList(),
)

/**
 * How common a frequency dictionary says a word is: [value] is a rank (1 the most common) when
 * [rankBased], else a count; [display] is the dictionary's own text for it, when it has one.
 */
data class WordFrequency(val dictionary: String, val value: Int?, val display: String?, val rankBased: Boolean) {
    val label: String get() = display ?: value?.let { if (rankBased) "#$it" else "$it" }.orEmpty()
}

/** A frequency as a pack stores it, for the word in one reading or in all of them. */
data class StoredFrequency(val reading: String?, val value: Int?, val display: String?)

/**
 * A Japanese pitch accent: the reading and the mora after which the pitch falls (its downstep),
 * 0 when it never falls (heiban), as Kanjium numbers them.
 */
data class PitchAccent(val reading: String, val downstep: Int) {
    /** The reading's morae: a small kana belongs to the mora before it. */
    val morae: List<String> get() = Japanese.morae(reading)

    fun isHigh(mora: Int): Boolean = Japanese.isMoraPitchHigh(mora, downstep)

    /** The pattern's name (heiban, atamadaka, nakadaka, odaka, kifuku), or null when it has none. */
    fun category(isVerbOrAdjective: Boolean): String? = Japanese.pitchCategory(reading, downstep, isVerbOrAdjective)
}

/**
 * What KANJIDIC says of a kanji: its meanings in the pack's gloss language (English where it has
 * none), its on and kun readings, stroke count, school grade, JLPT level and newspaper frequency rank.
 */
data class KanjiInfo(
    val character: String,
    val meanings: List<String>,
    val onyomi: List<String>,
    val kunyomi: List<String>,
    val strokes: Int? = null,
    val grade: Int? = null,
    val jlpt: Int? = null,
    val frequency: Int? = null,
)

/**
 * What the dictionary knows about a clicked word: entries for the word itself first, then for
 * the lemmas it is a form of. [lemmas] lists the lemmas the word inflects, excluding the word.
 */
data class DictionaryLookup(val entries: List<DictionaryEntry>, val lemmas: List<String>) {
    val isEmpty: Boolean get() = entries.isEmpty()

    /**
     * The lemma to record as the term's parent: only when the word is not a headword itself, and
     * it inflects one lemma, or one lemma with far more senses than the rest ("went" belongs to
     * "go" and to the rare "wend").
     */
    val parentSuggestion: String?
        get() {
            if (entries.any { it.isOwnEntry }) return null
            lemmas.singleOrNull()?.let { return it }
            val weights = lemmas.map { lemma -> lemma to entries.filter { it.word == lemma }.sumOf { it.senses.size } }
                .sortedByDescending { it.second }
            if (weights.size < 2) return null
            val (top, second) = weights
            return top.first.takeIf { top.second >= DOMINANT_SENSE_RATIO * maxOf(second.second, 1) }
        }

    /** The first gloss, used to prefill an empty translation. */
    val suggestedTranslation: String? get() = entries.firstOrNull()?.senses?.firstOrNull()?.glosses?.firstOrNull()

    companion object {
        val EMPTY = DictionaryLookup(emptyList(), emptyList())
        private const val DOMINANT_SENSE_RATIO = 3
    }
}

/**
 * Identifies a dictionary by the ISO 639-1 codes of the language it explains and the language it explains in.
 * A [glossary] is a word finder instead: words of the reader's own language with their translations
 * into the language they learn, so a word can be found from its meaning. An [imported] dictionary
 * is one the user brought, by its [ImportedDictionary.key].
 */
data class DictionaryId(val sourceLanguage: String, val targetLanguage: String, val glossary: Boolean = false, val imported: String? = null) {
    val name: String
        get() = when {
            imported != null -> "imported-$imported"
            glossary -> "$sourceLanguage-$targetLanguage-gloss"
            else -> "$sourceLanguage-$targetLanguage"
        }

    companion object {
        /**
         * The layout of the pack files, matching the "format" row of their meta table and
         * PRAGMA user_version; a file with another format is ignored.
         */
        const val FORMAT = 1
    }
}

/** A downloadable dictionary: a gzip-compressed SQLite file produced by tools/build_dictionary.py or tools/build_yomitan_dictionary.py. */
data class DictionaryPack(val id: DictionaryId, val title: String, val url: String, val downloadSize: Long? = null)

/** The packs the app knows how to download, by title. */
object DictionaryPacks {
    private const val RELEASES_URL = "https://github.com/zavanton123/tayra-languages/releases/download"

    /** Languages with packs, as sources or as the language of the glosses, by ISO 639 code. */
    private val names: Map<String, String> = mapOf(
        "sq" to "Albanian", "grc" to "Ancient Greek", "ar" to "Arabic", "hy" to "Armenian",
        "aii" to "Assyrian", "eu" to "Basque", "be" to "Belarusian", "bs" to "Bosnian", "br" to "Breton",
        "bg" to "Bulgarian", "yue" to "Cantonese", "ca" to "Catalan", "zh" to "Chinese", "kw" to "Cornish",
        "hr" to "Croatian", "cs" to "Czech", "da" to "Danish", "nl" to "Dutch", "arz" to "Egyptian Arabic",
        "en" to "English", "eo" to "Esperanto", "et" to "Estonian", "fa" to "Farsi", "fi" to "Finnish",
        "fr" to "French", "gl" to "Galician", "ka" to "Georgian", "de" to "German", "el" to "Greek",
        "haw" to "Hawaiian", "he" to "Hebrew", "hi" to "Hindi", "hu" to "Hungarian", "is" to "Icelandic",
        "id" to "Indonesian", "ga" to "Irish", "it" to "Italian", "ja" to "Japanese", "kn" to "Kannada",
        "km" to "Khmer", "ko" to "Korean", "lo" to "Lao", "la" to "Latin", "lv" to "Latvian",
        "lt" to "Lithuanian", "mk" to "Macedonian", "mt" to "Maltese", "gv" to "Manx", "mn" to "Mongolian",
        "no" to "Norwegian", "sga" to "Old Irish", "pl" to "Polish", "pt" to "Portuguese",
        "ro" to "Romanian", "ru" to "Russian", "gd" to "Scottish Gaelic", "sr" to "Serbian",
        "sk" to "Slovak", "sl" to "Slovene", "es" to "Spanish", "sv" to "Swedish", "tl" to "Tagalog",
        "th" to "Thai", "tok" to "Toki Pona", "tr" to "Turkish", "uk" to "Ukrainian", "vi" to "Vietnamese",
        "cy" to "Welsh", "yi" to "Yiddish",
    )

    private val published: List<DictionaryPack> = dictionaryPackReleases.flatMap { (release, sizes) ->
        sizes.mapNotNull { (name, size) ->
            val parts = name.split('-')
            val glossary = parts.size == 3 && parts[2] == "gloss"
            if (parts.size != 2 && !glossary) return@mapNotNull null
            val (source, target) = parts
            val title = "${names[source] ?: source} → ${names[target] ?: target}"
            DictionaryPack(DictionaryId(source, target, glossary), title, "$RELEASES_URL/$release/$name.sqlite.gzip", size)
        }
    }.sortedBy { it.title }

    /** A language's English name, by its code; the code itself when no pack has the language. */
    fun languageName(code: String): String = names[code] ?: code

    /** Every published dictionary of words; a pair the Wiktionaries cover too thinly has none. */
    val all: List<DictionaryPack> = published.filter { !it.id.glossary }

    /** The word finders, by the reader's language (their source) and the learned one. */
    val glossaries: List<DictionaryPack> = published.filter { it.id.glossary }

    /** Dictionaries and word finders: every pack that can be installed. */
    val everything: List<DictionaryPack> = published

    fun find(id: DictionaryId): DictionaryPack? = published.firstOrNull { it.id == id }

    fun find(sourceLanguage: String?, targetLanguage: String?): DictionaryPack? =
        all.firstOrNull { it.id.sourceLanguage == sourceLanguage?.lowercase() && it.id.targetLanguage == targetLanguage?.lowercase() }

    /** The word finder from [nativeLanguage] words to [learningLanguage] ones, when there is one. */
    fun findGlossary(nativeLanguage: String?, learningLanguage: String?): DictionaryPack? =
        glossaries.firstOrNull { it.id.sourceLanguage == nativeLanguage?.lowercase() && it.id.targetLanguage == learningLanguage?.lowercase() }
}

/** Whether a pack is on the device. */
sealed interface PackState {
    data object NotInstalled : PackState
    /** [progress] is 0..1, or null when the size is unknown. */
    data class Downloading(val progress: Float?) : PackState
    data class Installed(val sizeBytes: Long) : PackState
    data class Failed(val message: String) : PackState
}

data class PackStatus(val pack: DictionaryPack, val state: PackState)

/** Keeps downloaded packs on the device: files on Android, iOS and desktop, the browser cache on the web. */
interface DictionaryPackStore {
    /** Size of the installed pack in bytes, or null when it is not installed. */
    suspend fun installedSize(pack: DictionaryPack): Long?
    /** Downloads and stores the pack, reporting progress 0..1 (or null when unknown). Throws when the download fails. */
    suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit)
    suspend fun remove(pack: DictionaryPack)

    /** Stores an imported dictionary's pack, the SQLite file itself. */
    suspend fun installFile(pack: DictionaryPack, sqlite: ByteArray): Unit = throw UnsupportedOperationException("Importing dictionaries is not supported here")
}

/** Looks words up in the installed dictionaries. */
interface OfflineDictionary {
    suspend fun isAvailable(dictionary: DictionaryId): Boolean
    suspend fun lookup(dictionary: DictionaryId, text: String): DictionaryLookup

    /** The kanji [text] is written with, in order, from a Japanese pack; empty for other languages. */
    suspend fun kanji(dictionary: DictionaryId, text: String): List<KanjiInfo> = emptyList()
}
