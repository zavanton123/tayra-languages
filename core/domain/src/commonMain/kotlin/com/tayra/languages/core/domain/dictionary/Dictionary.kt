package com.tayra.languages.core.domain.dictionary

/** One meaning of a dictionary entry, with usage labels such as "comparative" or "abbreviation". */
data class DictionarySense(val glosses: List<String>, val tags: List<String> = emptyList())

/** A headword in one part of speech; [isOwnEntry] is true when it is the looked-up word itself rather than a lemma of it. */
data class DictionaryEntry(val word: String, val pos: String, val ipa: String?, val senses: List<DictionarySense>, val isOwnEntry: Boolean = true)

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

/** Identifies a dictionary by the ISO 639-1 codes of the language it explains and the language it explains in. */
data class DictionaryId(val sourceLanguage: String, val targetLanguage: String) {
    val name: String get() = "$sourceLanguage-$targetLanguage"

    companion object {
        /**
         * The layout of the pack files, matching the "format" row of their meta table and
         * PRAGMA user_version; a file with another format is ignored.
         */
        const val FORMAT = 1
    }
}

/** A downloadable dictionary: a gzip-compressed SQLite file produced by tools/build_dictionary.py. */
data class DictionaryPack(val id: DictionaryId, val title: String, val url: String)

/** The packs the app knows how to download, by title. */
object DictionaryPacks {
    private const val BASE_URL = "https://github.com/zavanton123/tayra-languages/releases/download/v0.1.0"

    /** Source languages with packs, by ISO 639-1 code. */
    private val sources: List<Pair<String, String>> = listOf(
        "be" to "Belarusian",
        "bg" to "Bulgarian",
        "ca" to "Catalan",
        "hr" to "Croatian",
        "cs" to "Czech",
        "da" to "Danish",
        "nl" to "Dutch",
        "en" to "English",
        "et" to "Estonian",
        "fi" to "Finnish",
        "fr" to "French",
        "gl" to "Galician",
        "de" to "German",
        "el" to "Greek",
        "hu" to "Hungarian",
        "is" to "Icelandic",
        "it" to "Italian",
        "la" to "Latin",
        "lv" to "Latvian",
        "lt" to "Lithuanian",
        "mk" to "Macedonian",
        "no" to "Norwegian",
        "pl" to "Polish",
        "pt" to "Portuguese",
        "ro" to "Romanian",
        "ru" to "Russian",
        "sr" to "Serbian",
        "sk" to "Slovak",
        "sl" to "Slovene",
        "es" to "Spanish",
        "sv" to "Swedish",
        "tr" to "Turkish",
        "uk" to "Ukrainian",
    )

    /** Languages the glosses can be in. */
    private val targets: List<Pair<String, String>> = listOf("ru" to "Russian", "en" to "English", "de" to "German")

    /** Pairs without a pack: Russian to Russian is not a translation, and the German Wiktionary has no Galician or Serbian section. */
    private val missing: Set<String> = setOf("ru-ru", "gl-de", "sr-de")

    val all: List<DictionaryPack> = targets.flatMap { (target, targetName) ->
        sources.filter { (source, _) -> "$source-$target" !in missing }.map { (source, sourceName) ->
            DictionaryPack(DictionaryId(source, target), "$sourceName → $targetName", "$BASE_URL/$source-$target.sqlite.gzip")
        }
    }.sortedBy { it.title }

    fun find(id: DictionaryId): DictionaryPack? = all.firstOrNull { it.id == id }

    fun find(sourceLanguage: String?, targetLanguage: String?): DictionaryPack? =
        all.firstOrNull { it.id.sourceLanguage == sourceLanguage?.lowercase() && it.id.targetLanguage == targetLanguage?.lowercase() }
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
}

/** Looks words up in the installed dictionaries. */
interface OfflineDictionary {
    suspend fun isAvailable(dictionary: DictionaryId): Boolean
    suspend fun lookup(dictionary: DictionaryId, text: String): DictionaryLookup
}
