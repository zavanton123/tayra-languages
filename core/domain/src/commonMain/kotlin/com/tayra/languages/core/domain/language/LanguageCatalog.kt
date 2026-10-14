package com.tayra.languages.core.domain.language

/** The fixed set of languages the app offers: what can be learned and what translations are shown in. */
object LanguageCatalog {
    /** Names of the predefined languages a user can learn, in display order. */
    val targetLanguages: List<String> = listOf(
        "Albanian", "Ancient Greek", "Armenian", "Basque", "Belarusian", "Bosnian", "Breton",
        "Bulgarian", "Catalan", "Cornish", "Croatian", "Czech", "Danish", "Dutch", "English",
        "Esperanto", "Estonian", "Finnish", "French", "Galician", "Georgian", "German", "Greek",
        "Hawaiian", "Hungarian", "Icelandic", "Indonesian", "Irish", "Italian", "Latin", "Latvian",
        "Lithuanian", "Macedonian", "Maltese", "Manx", "Mongolian", "Norwegian", "Old Irish", "Polish",
        "Portuguese", "Romanian", "Russian", "Scottish Gaelic", "Serbian", "Slovak", "Slovene",
        "Spanish", "Swedish", "Tagalog", "Toki Pona", "Turkish", "Ukrainian", "Vietnamese", "Welsh",
    )

    /** Languages translations and example sentences can be shown in. */
    val nativeLanguages: List<LanguageOption> = listOf(
        LanguageOption("en", "English"), LanguageOption("zh", "Chinese"), LanguageOption("fr", "French"),
        LanguageOption("de", "German"), LanguageOption("el", "Greek"), LanguageOption("pl", "Polish"),
        LanguageOption("pt", "Portuguese"), LanguageOption("ru", "Russian"), LanguageOption("es", "Spanish"),
    )

    /** Languages the interface is translated into, each named in itself. */
    val interfaceLanguages: List<LanguageOption> = listOf(LanguageOption("en", "English"), LanguageOption("ru", "Русский"))

    fun isTarget(name: String): Boolean = targetLanguages.any { it.equals(name.trim(), ignoreCase = true) }

    /** The native language for a stored code, falling back to the first entry for unknown codes. */
    fun nativeOption(code: String): LanguageOption =
        nativeLanguages.firstOrNull { it.code == code.trim().lowercase() } ?: nativeLanguages.first()

    /** The interface language for a stored code, falling back to the first entry for unknown codes. */
    fun interfaceOption(code: String): LanguageOption =
        interfaceLanguages.firstOrNull { it.code == code.trim().lowercase() } ?: interfaceLanguages.first()
}
