package com.idiomiko.core.domain.language

/** The groups the languages to learn are offered in: parts of the world, the classical languages and the made ones. */
enum class LanguageRegion { EUROPE, ASIA, MIDDLE_EAST, AFRICA, AMERICAS_PACIFIC, CLASSICAL, CONSTRUCTED }

/**
 * Which groups each language to learn is in, by its English name; a language at home in more than
 * one, such as Russian in Europe and Asia, is in each. [popular] are the ones most often chosen.
 */
object LanguageRegions {
    val popular: Set<String> = setOf(
        "English", "Spanish", "French", "German", "Italian", "Portuguese", "Russian",
        "Japanese", "Korean", "Mandarin Chinese", "Arabic",
    )

    /** The groups of the language named [name], none for a language the catalog does not know. */
    fun of(name: String): Set<LanguageRegion> = regions[name].orEmpty()

    private val regions: Map<String, Set<LanguageRegion>> = buildMap {
        fun put(region: LanguageRegion, vararg names: String) = names.forEach { merge(it, setOf(region), Set<LanguageRegion>::plus) }
        put(
            LanguageRegion.EUROPE,
            "Albanian", "Armenian", "Azerbaijani", "Basque", "Belarusian", "Bosnian", "Breton", "Bulgarian", "Catalan", "Cornish",
            "Croatian", "Czech", "Danish", "Dutch", "English", "Estonian", "Finnish", "French", "Galician", "Georgian", "German",
            "Greek", "Hungarian", "Icelandic", "Irish", "Italian", "Kazakh", "Latvian", "Lithuanian", "Macedonian", "Maltese",
            "Manx", "Norwegian", "Polish", "Portuguese", "Romanian", "Russian", "Scottish Gaelic", "Serbian", "Slovak", "Slovene",
            "Spanish", "Swedish", "Turkish", "Ukrainian", "Welsh", "Yiddish",
        )
        put(
            LanguageRegion.ASIA,
            "Bengali", "Burmese", "Cantonese", "Gujarati", "Hindi", "Indonesian", "Japanese", "Kannada", "Kazakh", "Khmer",
            "Korean", "Lao", "Malay", "Mandarin Chinese", "Marathi", "Mongolian", "Nepali", "Pashto", "Punjabi", "Russian",
            "Sinhala", "Tagalog", "Tamil", "Telugu", "Thai", "Urdu", "Uzbek", "Vietnamese",
        )
        put(
            LanguageRegion.MIDDLE_EAST,
            "Arabic", "Armenian", "Assyrian", "Azerbaijani", "Egyptian Arabic", "Farsi", "Georgian", "Hebrew", "Pashto", "Turkish", "Yiddish",
        )
        put(LanguageRegion.AFRICA, "Afrikaans", "Amharic", "Arabic", "Egyptian Arabic", "Hausa", "Somali", "Swahili", "Yoruba", "Zulu")
        put(LanguageRegion.AMERICAS_PACIFIC, "English", "French", "Hawaiian", "Portuguese", "Spanish")
        put(LanguageRegion.CLASSICAL, "Ancient Greek", "Latin", "Old Irish")
        put(LanguageRegion.CONSTRUCTED, "Esperanto", "Toki Pona")
    }
}
