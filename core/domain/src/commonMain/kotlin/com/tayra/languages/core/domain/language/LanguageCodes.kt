package com.tayra.languages.core.domain.language

/** A language the user can pick as their native language. */
data class LanguageOption(val code: String, val name: String)

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

    /** ISO 639-3 codes as used by Tatoeba, keyed by the 639-1 codes above. */
    private val iso3ByIso1: Map<String, String> = mapOf(
        "af" to "afr", "sq" to "sqi", "am" to "amh", "ar" to "ara", "hy" to "hye", "az" to "aze", "eu" to "eus", "be" to "bel",
        "bn" to "ben", "bs" to "bos", "br" to "bre", "bg" to "bul", "ca" to "cat", "zh" to "cmn", "hr" to "hrv", "cs" to "ces",
        "da" to "dan", "nl" to "nld", "en" to "eng", "eo" to "epo", "et" to "est", "fo" to "fao", "fa" to "pes", "fi" to "fin",
        "fr" to "fra", "gl" to "glg", "ka" to "kat", "de" to "deu", "el" to "ell", "he" to "heb", "hi" to "hin", "hu" to "hun",
        "is" to "isl", "id" to "ind", "it" to "ita", "ja" to "jpn", "kk" to "kaz", "km" to "khm", "ko" to "kor", "la" to "lat",
        "lv" to "lvs", "lt" to "lit", "mk" to "mkd", "no" to "nob", "pl" to "pol", "pt" to "por", "pa" to "pan", "ro" to "ron",
        "ru" to "rus", "sa" to "san", "sr" to "srp", "sk" to "slk", "sl" to "slv", "es" to "spa", "sw" to "swh", "sv" to "swe",
        "th" to "tha", "bo" to "bod", "tr" to "tur", "uk" to "ukr", "vi" to "vie", "cy" to "cym", "tok" to "toki", "got" to "got",
        "nah" to "nah", "nv" to "nav", "ain" to "ain", "ceb" to "ceb", "ryu" to "ryu",
    )

    /** Every known language once, by its shortest name, sorted by name. */
    val options: List<LanguageOption> = byName.entries
        .groupBy({ it.value }, { it.key })
        .map { (code, names) -> LanguageOption(code, names.minBy { it.length }.replaceFirstChar { it.uppercase() }) }
        .sortedBy { it.name }

    /** The option for a code, or null when unknown. */
    fun option(code: String): LanguageOption? = options.firstOrNull { it.code == code.trim().lowercase() }

    /** Code for a language name, or null when unknown. Names may carry a variant in parentheses. */
    fun codeFor(languageName: String): String? {
        val key = languageName.trim().lowercase().substringBefore('(').trim()
        return byName[key] ?: byName.entries.firstOrNull { key.startsWith(it.key) }?.value
    }

    /** Tatoeba (ISO 639-3) code for a 639-1 code, or null when unknown. */
    fun tatoebaCode(iso1: String): String? = iso3ByIso1[iso1.trim().lowercase()]

    /** Tatoeba code for a language name, or null when unknown. */
    fun tatoebaCodeFor(languageName: String): String? = codeFor(languageName)?.let { tatoebaCode(it) }
}
