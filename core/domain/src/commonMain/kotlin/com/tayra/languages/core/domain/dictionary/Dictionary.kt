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
        /** Dictionaries bundled with the app as prebuilt SQLite files. */
        val bundled: List<DictionaryId> = listOf(DictionaryId("en", "ru"))

        /**
         * The layout of the bundled files, matching the "format" row of their meta table and
         * PRAGMA user_version; a file with another format is ignored.
         */
        const val FORMAT = 1

        fun bundledFor(sourceLanguage: String?, targetLanguage: String?): DictionaryId? =
            bundled.firstOrNull { it.sourceLanguage == sourceLanguage?.lowercase() && it.targetLanguage == targetLanguage?.lowercase() }
    }
}

/** Locates the bundled dictionary files produced by tools/build_english_russian_dictionary.py. */
interface DictionaryAssets {
    /** The whole SQLite file, for platforms that copy it into app storage; null when not bundled. */
    suspend fun readBytes(dictionary: DictionaryId): ByteArray?

    /** A URL the file can be fetched from, for the web; null when not bundled. */
    fun uri(dictionary: DictionaryId): String?
}

/** Looks words up in the bundled dictionaries. */
interface OfflineDictionary {
    suspend fun isAvailable(dictionary: DictionaryId): Boolean
    suspend fun lookup(dictionary: DictionaryId, text: String): DictionaryLookup
}
