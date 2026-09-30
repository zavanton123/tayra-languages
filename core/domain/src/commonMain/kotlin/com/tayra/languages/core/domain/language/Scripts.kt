package com.tayra.languages.core.domain.language

/** The writing systems text is in, as far as telling one language's translations from another's needs. */
enum class Script { LATIN, CYRILLIC, GREEK, ARABIC, HEBREW, HAN, KANA, HANGUL, DEVANAGARI, THAI, GEORGIAN, ARMENIAN }

object Scripts {
    /** The scripts a language is written in; Latin when it is not one of the others. */
    fun of(languageCode: String): Set<Script> = when (languageCode.trim().lowercase()) {
        "ru", "uk", "be", "bg", "sr", "mk", "kk", "ky", "tg", "mn", "tt", "ba", "cv" -> setOf(Script.CYRILLIC)
        "el" -> setOf(Script.GREEK)
        "ar", "fa", "ur", "ps", "ug", "sd" -> setOf(Script.ARABIC)
        "he", "yi" -> setOf(Script.HEBREW)
        "zh" -> setOf(Script.HAN)
        "ja" -> setOf(Script.KANA, Script.HAN)
        "ko" -> setOf(Script.HANGUL, Script.HAN)
        "hi", "mr", "ne", "sa" -> setOf(Script.DEVANAGARI)
        "th" -> setOf(Script.THAI)
        "ka" -> setOf(Script.GEORGIAN)
        "hy" -> setOf(Script.ARMENIAN)
        else -> setOf(Script.LATIN)
    }

    /** The script most of the letters in [text] belong to, or null when it has none it recognises. */
    fun dominant(text: String): Script? =
        text.mapNotNull { scriptOf(it) }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

    private fun scriptOf(c: Char): Script? = when (c.code) {
        in 0x41..0x5A, in 0x61..0x7A, in 0xC0..0x24F, in 0x1E00..0x1EFF -> if (c.isLetter()) Script.LATIN else null
        in 0x370..0x3FF, in 0x1F00..0x1FFF -> Script.GREEK
        in 0x400..0x52F -> Script.CYRILLIC
        in 0x530..0x58F -> Script.ARMENIAN
        in 0x590..0x5FF -> Script.HEBREW
        in 0x600..0x6FF, in 0x750..0x77F -> Script.ARABIC
        in 0x900..0x97F -> Script.DEVANAGARI
        in 0xE00..0xE7F -> Script.THAI
        in 0x10A0..0x10FF -> Script.GEORGIAN
        in 0x3040..0x30FF -> Script.KANA
        in 0x4E00..0x9FFF, in 0x3400..0x4DBF -> Script.HAN
        in 0xAC00..0xD7AF, in 0x1100..0x11FF -> Script.HANGUL
        else -> null
    }
}
