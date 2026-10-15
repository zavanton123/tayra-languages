package com.tayra.languages.core.data.ocr

import com.tayra.languages.core.data.files.LineReflow
import com.tayra.languages.core.data.files.isUnspaced
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.morphology.ChineseScript

/** The writing systems text recognition tells apart: the engines read some and not others. */
internal enum class OcrScript {
    LATIN, CYRILLIC, GREEK, ARABIC, HEBREW, ARMENIAN, GEORGIAN, DEVANAGARI, BENGALI, GURMUKHI, KANNADA, THAI, LAO, KHMER,
    TIBETAN, SYRIAC, ETHIOPIC, HAN_SIMPLIFIED, HAN_TRADITIONAL, JAPANESE, KOREAN,
}

/** What recognizing a language takes: its script, and the Tesseract models that read it. */
internal data class OcrLanguage(val code: String?, val script: OcrScript, val tesseractModels: List<String>) {
    /** Written without spaces between words, so a space Tesseract puts between two letters is none. */
    val unspaced: Boolean get() = script in UNSPACED
}

private val UNSPACED = setOf(OcrScript.HAN_SIMPLIFIED, OcrScript.HAN_TRADITIONAL, OcrScript.JAPANESE, OcrScript.THAI, OcrScript.LAO, OcrScript.KHMER, OcrScript.TIBETAN)

internal object OcrLanguages {

    /**
     * How [language] is recognized. A language Tesseract has no model for is read by its script's
     * model (Manx, Hawaiian, Toki Pona by the Latin one); a language Tayra cannot name is taken as
     * written in the Latin alphabet.
     */
    fun of(language: Language): OcrLanguage {
        val code = LanguageCodes.codeFor(language.name)
        if (code == "zh" || code == "yue") {
            val traditional = code == "yue" || language.chineseScript == ChineseScript.TRADITIONAL
            return OcrLanguage(code, if (traditional) OcrScript.HAN_TRADITIONAL else OcrScript.HAN_SIMPLIFIED, listOf(if (traditional) "chi_tra" else "chi_sim"))
        }
        val (script, models) = BY_CODE[code] ?: (OcrScript.LATIN to "script/Latin")
        return OcrLanguage(code, script, models.split('+'))
    }

    /** A model's name for people: the language it reads, or its script. */
    fun title(model: String): String = when {
        model.startsWith("script/") -> model.removePrefix("script/").replace("HanS", "Chinese (simplified)").replace("HanT", "Chinese (traditional)").replace('_', ' ') + " script"
        else -> TITLES[model] ?: model
    }

    /**
     * Each language's script and Tesseract models, by Tayra's code. Serbian is written in both of
     * its alphabets; Filipino's model reads Tagalog; Okinawan and Ainu are written in Japanese.
     */
    private val BY_CODE: Map<String?, Pair<OcrScript, String>> = mapOf(
        "af" to (OcrScript.LATIN to "afr"), "ain" to (OcrScript.JAPANESE to "jpn"), "sq" to (OcrScript.LATIN to "sqi"),
        "am" to (OcrScript.ETHIOPIC to "amh"), "ar" to (OcrScript.ARABIC to "ara"), "arz" to (OcrScript.ARABIC to "ara"),
        "hy" to (OcrScript.ARMENIAN to "hye"), "az" to (OcrScript.LATIN to "aze"), "eu" to (OcrScript.LATIN to "eus"),
        "be" to (OcrScript.CYRILLIC to "bel"), "bn" to (OcrScript.BENGALI to "ben"), "bs" to (OcrScript.LATIN to "bos"),
        "br" to (OcrScript.LATIN to "bre"), "bg" to (OcrScript.CYRILLIC to "bul"), "ca" to (OcrScript.LATIN to "cat"),
        "ceb" to (OcrScript.LATIN to "ceb"), "hr" to (OcrScript.LATIN to "hrv"), "cs" to (OcrScript.LATIN to "ces"),
        "da" to (OcrScript.LATIN to "dan"), "nl" to (OcrScript.LATIN to "nld"), "en" to (OcrScript.LATIN to "eng"),
        "eo" to (OcrScript.LATIN to "epo"), "et" to (OcrScript.LATIN to "est"), "fo" to (OcrScript.LATIN to "fao"),
        "fa" to (OcrScript.ARABIC to "fas"), "fi" to (OcrScript.LATIN to "fin"), "fr" to (OcrScript.LATIN to "fra"),
        "gl" to (OcrScript.LATIN to "glg"), "ka" to (OcrScript.GEORGIAN to "kat"), "de" to (OcrScript.LATIN to "deu"),
        "el" to (OcrScript.GREEK to "ell"), "grc" to (OcrScript.GREEK to "grc"), "he" to (OcrScript.HEBREW to "heb"),
        "yi" to (OcrScript.HEBREW to "yid"), "hi" to (OcrScript.DEVANAGARI to "hin"), "sa" to (OcrScript.DEVANAGARI to "san"),
        "hu" to (OcrScript.LATIN to "hun"), "is" to (OcrScript.LATIN to "isl"), "id" to (OcrScript.LATIN to "ind"),
        "it" to (OcrScript.LATIN to "ita"), "ja" to (OcrScript.JAPANESE to "jpn"), "ryu" to (OcrScript.JAPANESE to "jpn"),
        "kk" to (OcrScript.CYRILLIC to "kaz"), "km" to (OcrScript.KHMER to "khm"), "ko" to (OcrScript.KOREAN to "kor"),
        "kn" to (OcrScript.KANNADA to "kan"), "lo" to (OcrScript.LAO to "lao"), "la" to (OcrScript.LATIN to "lat"),
        "lv" to (OcrScript.LATIN to "lav"), "lt" to (OcrScript.LATIN to "lit"), "mk" to (OcrScript.CYRILLIC to "mkd"),
        "mn" to (OcrScript.CYRILLIC to "mon"), "mt" to (OcrScript.LATIN to "mlt"), "no" to (OcrScript.LATIN to "nor"),
        "pl" to (OcrScript.LATIN to "pol"), "pt" to (OcrScript.LATIN to "por"), "pa" to (OcrScript.GURMUKHI to "pan"),
        "ro" to (OcrScript.LATIN to "ron"), "ru" to (OcrScript.CYRILLIC to "rus"), "sr" to (OcrScript.CYRILLIC to "srp+srp_latn"),
        "sk" to (OcrScript.LATIN to "slk"), "sl" to (OcrScript.LATIN to "slv"), "es" to (OcrScript.LATIN to "spa"),
        "sw" to (OcrScript.LATIN to "swa"), "sv" to (OcrScript.LATIN to "swe"), "th" to (OcrScript.THAI to "tha"),
        "bo" to (OcrScript.TIBETAN to "bod"), "tr" to (OcrScript.LATIN to "tur"), "uk" to (OcrScript.CYRILLIC to "ukr"),
        "vi" to (OcrScript.LATIN to "vie"), "cy" to (OcrScript.LATIN to "cym"), "ga" to (OcrScript.LATIN to "gle"),
        "gd" to (OcrScript.LATIN to "gla"), "tl" to (OcrScript.LATIN to "fil"), "aii" to (OcrScript.SYRIAC to "syr"),
        "gv" to (OcrScript.LATIN to "script/Latin"), "kw" to (OcrScript.LATIN to "script/Latin"), "haw" to (OcrScript.LATIN to "script/Latin"),
        "sga" to (OcrScript.LATIN to "script/Latin"), "tok" to (OcrScript.LATIN to "script/Latin"), "nah" to (OcrScript.LATIN to "script/Latin"),
        "nv" to (OcrScript.LATIN to "script/Latin"),
    )

    private val TITLES: Map<String, String> = mapOf(
        "afr" to "Afrikaans", "jpn" to "Japanese", "sqi" to "Albanian", "amh" to "Amharic", "ara" to "Arabic", "hye" to "Armenian",
        "aze" to "Azerbaijani", "eus" to "Basque", "bel" to "Belarusian", "ben" to "Bengali", "bos" to "Bosnian", "bre" to "Breton",
        "bul" to "Bulgarian", "cat" to "Catalan", "ceb" to "Cebuano", "hrv" to "Croatian", "ces" to "Czech", "dan" to "Danish",
        "nld" to "Dutch", "eng" to "English", "epo" to "Esperanto", "est" to "Estonian", "fao" to "Faroese", "fas" to "Persian",
        "fin" to "Finnish", "fra" to "French", "glg" to "Galician", "kat" to "Georgian", "deu" to "German", "ell" to "Greek",
        "grc" to "Ancient Greek", "heb" to "Hebrew", "yid" to "Yiddish", "hin" to "Hindi", "san" to "Sanskrit", "hun" to "Hungarian",
        "isl" to "Icelandic", "ind" to "Indonesian", "ita" to "Italian", "kaz" to "Kazakh", "khm" to "Khmer", "kor" to "Korean",
        "kan" to "Kannada", "lao" to "Lao", "lat" to "Latin", "lav" to "Latvian", "lit" to "Lithuanian", "mkd" to "Macedonian",
        "mon" to "Mongolian", "mlt" to "Maltese", "nor" to "Norwegian", "pol" to "Polish", "por" to "Portuguese", "pan" to "Punjabi",
        "ron" to "Romanian", "rus" to "Russian", "srp" to "Serbian (Cyrillic)", "srp_latn" to "Serbian (Latin)", "slk" to "Slovak",
        "slv" to "Slovene", "spa" to "Spanish", "swa" to "Swahili", "swe" to "Swedish", "tha" to "Thai", "bod" to "Tibetan",
        "tur" to "Turkish", "ukr" to "Ukrainian", "vie" to "Vietnamese", "cym" to "Welsh", "gle" to "Irish", "gla" to "Scottish Gaelic",
        "fil" to "Filipino", "syr" to "Syriac", "chi_sim" to "Chinese (simplified)", "chi_tra" to "Chinese (traditional)",
    )
}

/** The text an engine read, made the app's: one paragraph per line, and no spaces a script does not write. */
internal object OcrText {

    fun clean(raw: String, language: OcrLanguage): String {
        val text = if (language.unspaced) dropSpacesBetweenUnspaced(raw) else raw
        return LineReflow.paragraphs(text, trustEmptyLines = false)
    }

    /** "我 们 是 学 生" as "我们是学生": Tesseract parts the characters of Chinese and Japanese with spaces. */
    internal fun dropSpacesBetweenUnspaced(text: String): String {
        val out = StringBuilder(text.length)
        for (i in text.indices) {
            val c = text[i]
            if (c == ' ' && i > 0 && i + 1 < text.length && isUnspaced(text[i - 1]) && isUnspaced(text[i + 1])) continue
            out.append(c)
        }
        return out.toString()
    }
}

/** The rotation a JPEG's EXIF orientation asks for before it is read, in quarter turns clockwise. */
internal object ExifOrientation {

    fun quarterTurns(bytes: ByteArray): Int = when (orientation(bytes)) {
        3, 4 -> 2
        5, 6 -> 1
        7, 8 -> 3
        else -> 0
    }

    /** The EXIF orientation tag (1 to 8) of a JPEG, or null for any other image or none. */
    internal fun orientation(bytes: ByteArray): Int? {
        if (bytes.size < 4 || u8(bytes, 0) != 0xFF || u8(bytes, 1) != 0xD8) return null
        var at = 2
        while (at + 4 <= bytes.size) {
            if (u8(bytes, at) != 0xFF) return null
            val marker = u8(bytes, at + 1)
            val length = (u8(bytes, at + 2) shl 8) or u8(bytes, at + 3)
            if (marker == 0xDA || length < 2) return null
            if (marker == 0xE1 && at + 10 <= bytes.size && bytes.copyOfRange(at + 4, at + 10).decodeToString() == "Exif\u0000\u0000") {
                return tiffOrientation(bytes, at + 10, minOf(bytes.size, at + 2 + length))
            }
            at += 2 + length
        }
        return null
    }

    private fun tiffOrientation(b: ByteArray, start: Int, end: Int): Int? {
        if (start + 8 > end) return null
        val little = u8(b, start) == 0x49
        fun u16(at: Int) = if (little) u8(b, at) or (u8(b, at + 1) shl 8) else (u8(b, at) shl 8) or u8(b, at + 1)
        fun u32(at: Int) = if (little) u16(at) or (u16(at + 2) shl 16) else (u16(at) shl 16) or u16(at + 2)
        val ifd = start + u32(start + 4)
        if (ifd + 2 > end) return null
        val count = u16(ifd)
        for (i in 0 until count) {
            val entry = ifd + 2 + 12 * i
            if (entry + 12 > end) return null
            if (u16(entry) == 0x0112) return u16(entry + 8).takeIf { it in 1..8 }
        }
        return null
    }

    private fun u8(b: ByteArray, at: Int): Int = b[at].toInt() and 0xFF
}
