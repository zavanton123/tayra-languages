package com.tayra.languages.core.domain.language

/** ISO 639-1 (or 639-3 where no two-letter code exists) codes for the predefined language names. */
object LanguageCodes {
    private val byName: Map<String, String> = mapOf(
        "afrikaans" to "af", "ainu" to "ain", "albanian" to "sq", "amharic" to "am", "arabic" to "ar", "armenian" to "hy",
        "azerbaijani" to "az", "basque" to "eu", "belarusian" to "be", "bengali" to "bn", "bosnian" to "bs", "breton" to "br",
        "bulgarian" to "bg", "catalan" to "ca", "cebuano" to "ceb", "classical chinese" to "zh", "mandarin chinese" to "zh",
        "chinese" to "zh", "croatian" to "hr", "czech" to "cs", "danish" to "da", "dutch" to "nl", "english" to "en",
        "esperanto" to "eo", "estonian" to "et", "faroese" to "fo", "farsi" to "fa", "persian" to "fa", "finnish" to "fi",
        "french" to "fr", "galician" to "gl", "georgian" to "ka", "german" to "de", "gothic" to "got", "greek" to "el",
        "hebrew" to "he", "hindi" to "hi", "hungarian" to "hu", "icelandic" to "is", "indonesian" to "id", "italian" to "it",
        "japanese" to "ja", "kazakh" to "kk", "khmer" to "km", "korean" to "ko", "latin" to "la", "latvian" to "lv",
        "lithuanian" to "lt", "macedonian" to "mk", "nahuatl" to "nah", "navajo" to "nv", "norwegian" to "no",
        "okinawan" to "ryu", "polish" to "pl", "portuguese" to "pt", "punjabi" to "pa", "romanian" to "ro", "russian" to "ru",
        "sanskrit" to "sa", "serbian" to "sr", "slovak" to "sk", "slovene" to "sl", "slovenian" to "sl", "spanish" to "es",
        "swahili" to "sw", "swedish" to "sv", "thai" to "th", "tibetan" to "bo", "toki pona" to "tok", "turkish" to "tr",
        "ukrainian" to "uk", "vietnamese" to "vi", "welsh" to "cy",
    )

    /** Code for a language name, or null when unknown. Names may carry a variant in parentheses. */
    fun codeFor(languageName: String): String? {
        val key = languageName.trim().lowercase().substringBefore('(').trim()
        return byName[key] ?: byName.entries.firstOrNull { key.startsWith(it.key) }?.value
    }
}
