package com.tayra.languages.core.ui.audio

/**
 * Tells Cantonese voices, which systems list under yue or as the Chinese of Hong Kong and Macau. A
 * system asked for yue without such a voice reads with its default one, Mandarin on a
 * Chinese-language device, so the synthesizers say nothing when none is installed.
 */
internal object CantoneseVoices {
    /** The voices' locales, in a form both Kotlin and PowerShell read, ignoring case. */
    const val LOCALES = "^(yue|zh[-_]yue|zh[-_](hant[-_])?(hk|mo))([-_]|$)"

    private val locales = Regex(LOCALES, RegexOption.IGNORE_CASE)

    fun isCantonese(languageCode: String?): Boolean = languageCode.equals("yue", ignoreCase = true)

    fun matches(locale: String): Boolean = locales.containsMatchIn(locale)
}
