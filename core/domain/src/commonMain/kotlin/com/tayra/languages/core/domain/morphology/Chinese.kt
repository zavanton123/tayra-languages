package com.tayra.languages.core.domain.morphology

/** Yomitan's Chinese reading helpers (ext/js/language/zh, GPL-3.0, Copyright (C) 2024-2026 Yomitan Authors). */
object Chinese {
    private val pinyinSeparators = Regex("[\\s・:'’-]|//")

    /** Pinyin as readings are matched: composed, lowercase, without spaces or syllable marks ("Wéi jī" as "wéijī"). */
    fun normalizePinyin(text: String): String = Unicode.nfc(text).lowercase().replace(pinyinSeparators, "")

    /** Pinyin without its tone marks too ("nǐhǎo" as "nihao"), for readings typed without them. */
    fun tonelessPinyin(text: String): String = Unicode.nfc(Unicode.nfd(normalizePinyin(text)).filter { it !in '̀'..'ͯ' })

    /** Whether the text has any Chinese character, as Yomitan's isStringPartiallyChinese tells. */
    fun isPartlyChinese(text: String): Boolean = text.any { c ->
        c.code in 0x4e00..0x9fff || c.code in 0x3400..0x4dbf || c.code in 0xf900..0xfaff || c.code in 0x3000..0x303f ||
            c.code in 0x3100..0x312f || c.code in 0x31a0..0x31bf || c.isSurrogate()
    }
}
