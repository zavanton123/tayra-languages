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

    /** The lemma to record as the term's parent: only when the word is not a headword itself and inflects exactly one lemma. */
    val parentSuggestion: String? get() = lemmas.singleOrNull()?.takeIf { entries.none { e -> e.isOwnEntry } }

    /** The first gloss, used to prefill an empty translation. */
    val suggestedTranslation: String? get() = entries.firstOrNull()?.senses?.firstOrNull()?.glosses?.firstOrNull()

    companion object {
        val EMPTY = DictionaryLookup(emptyList(), emptyList())
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

/** The packs the app knows how to download. */
object DictionaryPacks {
    val all: List<DictionaryPack> = listOf(
        DictionaryPack(
            DictionaryId("en", "ru"),
            "English → Russian",
            "https://github.com/zavanton123/tayra-languages/releases/download/v0.1.0/en-ru.sqlite.gzip",
        ),
        DictionaryPack(
            DictionaryId("de", "ru"),
            "German → Russian",
            "https://github.com/zavanton123/tayra-languages/releases/download/v0.1.0/de-ru.sqlite.gzip",
        ),
        DictionaryPack(
            DictionaryId("fr", "ru"),
            "French → Russian",
            "https://github.com/zavanton123/tayra-languages/releases/download/v0.1.0/fr-ru.sqlite.gzip",
        ),
        DictionaryPack(
            DictionaryId("pt", "ru"),
            "Portuguese → Russian",
            "https://github.com/zavanton123/tayra-languages/releases/download/v0.1.0/pt-ru.sqlite.gzip",
        ),
        DictionaryPack(
            DictionaryId("sr", "ru"),
            "Serbian → Russian",
            "https://github.com/zavanton123/tayra-languages/releases/download/v0.1.0/sr-ru.sqlite.gzip",
        ),
        DictionaryPack(
            DictionaryId("es", "ru"),
            "Spanish → Russian",
            "https://github.com/zavanton123/tayra-languages/releases/download/v0.1.0/es-ru.sqlite.gzip",
        ),
    )

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
