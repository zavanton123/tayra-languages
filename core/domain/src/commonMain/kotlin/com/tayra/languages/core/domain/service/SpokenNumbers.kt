package com.tayra.languages.core.domain.service

/**
 * Numbers written so that the speech engines read them as numbers. eSpeak, which Piper and Kokoro
 * take their phonemes from, reads the point of 38.000 as a full stop ("trinta e oito. zero zero
 * zero") and 38 000 digit by digit, yet 38000 as the number it is. So the separators a language
 * groups digits with are dropped, and an English decimal point is said as "point". Anything else
 * stays as written: years, dates (3.10.2024), times, phone numbers and decimal commas.
 */
object SpokenNumbers {

    fun normalize(text: String, languageCode: String?): String {
        if (text.none { it.isDigit() }) return text
        val language = languageCode?.substringBefore('-')?.substringBefore('_')?.lowercase() ?: return text
        return when (language) {
            "en" -> ENGLISH_NUMBER.replace(text) { english(text, it) }
            in POINT_GROUPING -> ungrouped(text, POINT_NUMBER, POINT_GROUPED, ".")
            in COMMA_GROUPING -> ungrouped(text, COMMA_NUMBER, COMMA_GROUPED, ",")
            in SPACE_GROUPING -> ungrouped(text, SPACE_NUMBER, SPACE_GROUPED, SPACES)
            else -> text
        }
    }

    private fun ungrouped(text: String, number: Regex, grouped: Regex, separators: String): String =
        number.replace(text) { match ->
            if (grouped.matches(match.value) && !phoneLike(text, match.range)) match.value.filterNot { it in separators } else match.value
        }

    private fun english(text: String, match: MatchResult): String {
        val run = match.value
        if (phoneLike(text, match.range)) return run
        val plain = when {
            ENGLISH_GROUPED.matches(run) -> run.replace(",", "")
            ',' !in run -> run
            else -> return run
        }
        val parts = plain.split('.')
        return when (parts.size) {
            1 -> plain
            // The digits after the point are said one by one: 3.14 is "three point one four".
            2 -> parts[0] + " point " + parts[1].toList().joinToString(" ")
            else -> run
        }
    }

    /** A number after a plus sign or joined by a hyphen to more digits, as in +7 912 or 3.456-7890. */
    private fun phoneLike(text: String, range: IntRange): Boolean {
        val before = text.getOrNull(range.first - 1)
        return before == '+' ||
            (before == '-' && text.getOrNull(range.first - 2)?.isDigit() == true) ||
            (text.getOrNull(range.last + 1) == '-' && text.getOrNull(range.last + 2)?.isDigit() == true)
    }

    private const val SPACES = "   "

    private val POINT_GROUPING = setOf(
        "pt", "es", "de", "it", "nl", "el", "tr", "da", "nb", "no", "nn", "sr", "hr", "bs", "sl", "mk", "id", "ro", "ca", "gl", "vi", "is", "eu",
    )
    private val COMMA_GROUPING = setOf("zh", "yue", "ja", "ko", "hi", "he", "th", "ga", "cy", "mt", "tl", "sw")
    private val SPACE_GROUPING = setOf("ru", "uk", "be", "bg", "cs", "pl", "fr", "sv", "fi", "sk", "et", "lv", "lt", "hu", "kk", "af")

    private val POINT_NUMBER = Regex("""\d+(?:\.\d+)*""")
    private val POINT_GROUPED = Regex("""\d{1,3}(?:\.\d{3})+""")
    private val COMMA_NUMBER = Regex("""\d+(?:,\d+)*""")
    private val COMMA_GROUPED = Regex("""\d{1,3}(?:,\d{3})+""")
    private val SPACE_NUMBER = Regex("""\d+(?:[$SPACES]\d+)*""")
    private val SPACE_GROUPED = Regex("""\d{1,3}(?:[$SPACES]\d{3})+""")
    private val ENGLISH_NUMBER = Regex("""\d+(?:[,.]\d+)*""")
    private val ENGLISH_GROUPED = Regex("""\d{1,3}(?:,\d{3})+(?:\.\d+)?""")
}
