package com.tayra.languages.core.domain.model

enum class DictionaryUse(val key: String) {
    TERMS("terms"),
    SENTENCES("sentences");

    companion object {
        fun fromKey(key: String): DictionaryUse = entries.first { it.key == key }
    }
}

enum class DictionaryType(val key: String) {
    EMBEDDED("embedded"),
    POPUP("popup");

    companion object {
        fun fromKey(key: String): DictionaryType = when (key) {
            "embedded", "embeddedhtml" -> EMBEDDED
            "popup", "popuphtml" -> POPUP
            else -> throw IllegalArgumentException("Unknown dictionary type '$key'")
        }
    }
}

data class LanguageDictionary(
    val id: Long = 0,
    val useFor: DictionaryUse,
    val type: DictionaryType,
    val url: String,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
) {
    /** Replaces the lookup placeholder with the (url-encoded) text. */
    fun lookupUrl(encodedText: String): String =
        url.replace(LOOKUP_PLACEHOLDER, encodedText).replace(LEGACY_PLACEHOLDER, encodedText)

    companion object {
        const val LOOKUP_PLACEHOLDER = "[LUTE]"
        const val LEGACY_PLACEHOLDER = "###"
    }
}

data class Language(
    val id: Long = 0,
    val name: String = "",
    val dictionaries: List<LanguageDictionary> = emptyList(),
    val characterSubstitutions: String = DEFAULT_CHARACTER_SUBSTITUTIONS,
    val regexpSplitSentences: String = DEFAULT_SPLIT_SENTENCES,
    val exceptionsSplitSentences: String = DEFAULT_SPLIT_SENTENCE_EXCEPTIONS,
    val wordCharacters: String = DEFAULT_WORD_CHARACTERS,
    val rightToLeft: Boolean = false,
    val showRomanization: Boolean = false,
    val parserType: String = DEFAULT_PARSER_TYPE,
) {
    fun activeDictionaries(useFor: DictionaryUse): List<LanguageDictionary> =
        dictionaries.filter { it.isActive && it.useFor == useFor }.sortedBy { it.sortOrder }

    val termDictionaries: List<LanguageDictionary> get() = activeDictionaries(DictionaryUse.TERMS)
    val sentenceDictionaries: List<LanguageDictionary> get() = activeDictionaries(DictionaryUse.SENTENCES)

    companion object {
        const val DEFAULT_CHARACTER_SUBSTITUTIONS = "´='|`='|’='|‘='|...=…|..=‥"
        const val DEFAULT_SPLIT_SENTENCES = ".!?"
        const val DEFAULT_SPLIT_SENTENCE_EXCEPTIONS = "Mr.|Mrs.|Dr.|[A-Z].|Vd.|Vds."
        const val DEFAULT_WORD_CHARACTERS = "a-zA-ZÀ-ÖØ-öø-ȳáéíóúÁÉÍÓÚñÑ"
        const val DEFAULT_PARSER_TYPE = "spacedel"
    }
}

data class LanguageSummary(
    val id: Long,
    val name: String,
    val bookCount: Int,
    val termCount: Int,
)
