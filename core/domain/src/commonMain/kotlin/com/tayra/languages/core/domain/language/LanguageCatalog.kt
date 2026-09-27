package com.tayra.languages.core.domain.language

/** The fixed set of languages the app offers: what can be learned and what translations are shown in. */
object LanguageCatalog {
    /** Names of the predefined languages a user can learn, in display order. */
    val targetLanguages: List<String> = listOf("English", "French", "German", "Portuguese", "Serbian", "Spanish")

    /** Languages translations and example sentences can be shown in. */
    val nativeLanguages: List<LanguageOption> = listOf(LanguageOption("en", "English"), LanguageOption("ru", "Russian"))

    fun isTarget(name: String): Boolean = targetLanguages.any { it.equals(name.trim(), ignoreCase = true) }

    /** The native language for a stored code, falling back to the first entry for unknown codes. */
    fun nativeOption(code: String): LanguageOption =
        nativeLanguages.firstOrNull { it.code == code.trim().lowercase() } ?: nativeLanguages.first()
}
