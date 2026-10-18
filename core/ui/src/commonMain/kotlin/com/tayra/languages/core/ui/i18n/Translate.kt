package com.tayra.languages.core.ui.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tayra.languages.core.ui.i18n.de.germanStrings
import com.tayra.languages.core.ui.i18n.el.greekStrings
import com.tayra.languages.core.ui.i18n.es.spanishStrings
import com.tayra.languages.core.ui.i18n.fr.frenchStrings
import com.tayra.languages.core.ui.i18n.pl.polishStrings
import com.tayra.languages.core.ui.i18n.pt.portugueseStrings
import com.tayra.languages.core.ui.i18n.ru.russianStrings
import com.tayra.languages.core.ui.i18n.zh.chineseStrings

/**
 * The language the interface is shown in. The app sets it from the settings; it is snapshot
 * state, so every composable that called [tr] recomposes when it changes.
 */
object UiLanguage {
    /** Codes of the languages the interface is translated into, English first. */
    val supported: List<String> = listOf("en", "zh", "fr", "de", "el", "pl", "pt", "ru", "es")

    var code: String by mutableStateOf("en")
        private set

    fun set(code: String) {
        this.code = code.takeIf { it in supported } ?: "en"
    }
}

/** Each translation, by its language's code: English text to its wording. */
internal fun translationOf(code: String): Map<String, String>? = when (code) {
    "zh" -> chineseStrings
    "fr" -> frenchStrings
    "de" -> germanStrings
    "el" -> greekStrings
    "pl" -> polishStrings
    "pt" -> portugueseStrings
    "ru" -> russianStrings
    "es" -> spanishStrings
    else -> null
}

/**
 * The interface text [english] in the current [UiLanguage]; `{0}`, `{1}`… are replaced by [args].
 *
 * The English text is the key: each translation (see [translationOf]) maps it to its own
 * wording, and text without a translation stays in English. Pass a single string literal, so
 * `TranslationsTest` can check that every text has its translations; `tr(name)` for text that
 * comes from data (language names, themes) is fine too.
 */
fun tr(english: String, vararg args: Any?): String = format(translationOf(UiLanguage.code)?.get(english) ?: english, args)

/**
 * A text that depends on [count], written in English as its [one] and [other] forms with `{0}`
 * for the count and `{1}`… for [args]. A translation, keyed by [other], gives the forms its
 * language has, separated by `|`, in the order [pluralIndex] numbers them.
 */
fun trPlural(count: Int, one: String, other: String, vararg args: Any?): String {
    val all = arrayOf<Any?>(count, *args)
    val forms = translationOf(UiLanguage.code)?.get(other)?.split('|') ?: return format(if (count == 1) one else other, all)
    return format(forms[pluralIndex(UiLanguage.code, count).coerceAtMost(forms.lastIndex)], all)
}

/** How many plural forms a translation into the language with [code] gives, as [pluralIndex] numbers them. */
internal fun pluralForms(code: String): Int = when (code) {
    "ru", "pl" -> 3
    "zh" -> 1
    else -> 2
}

/**
 * Which of its plural forms the language with [code] uses for [count], by CLDR's rules for whole
 * numbers: Russian one (1, 21), few (2–4, 22–24), many (the rest); Polish one (1), few (2–4,
 * 22–24), many (the rest); French and Portuguese one (0, 1), other; Chinese a single form; the
 * others one (1), other.
 */
internal fun pluralIndex(code: String, count: Int): Int {
    val n = if (count < 0) -count else count
    val few = n % 10 in 2..4 && n % 100 !in 12..14
    return when (code) {
        "ru" -> when {
            n % 10 == 1 && n % 100 != 11 -> 0
            few -> 1
            else -> 2
        }
        "pl" -> when {
            n == 1 -> 0
            few -> 1
            else -> 2
        }
        "fr", "pt" -> if (n <= 1) 0 else 1
        "zh" -> 0
        else -> if (n == 1) 0 else 1
    }
}

private fun format(text: String, args: Array<out Any?>): String {
    if (args.isEmpty()) return text
    var out = text
    args.forEachIndexed { i, arg -> out = out.replace("{$i}", arg.toString()) }
    return out
}

/** The case a language name takes inside a sentence, for the languages that decline it (Russian, Polish). */
enum class LanguageCase { NOMINATIVE, GENITIVE, PREPOSITIONAL }

/**
 * The language [name] (stored in English, "Portuguese") for use inside a sentence of the interface
 * language. English, German and Chinese keep the name as their lists write it ("Portugiesisch",
 * "葡萄牙语"); French, Spanish, Portuguese and Greek write it in lower case ("portugais"). Russian
 * and Polish name a language with an adjective, which goes lower case into [case]: Russian
 * «португальский», «(для) португальского (языка)», «(на) португальском (языке)», which the text
 * follows with «язык» where it needs it; Polish "portugalski", "(języka) portugalskiego", "(w
 * języku) portugalskim". A name that is no such adjective ("esperanto") stays as it is.
 */
fun languageInSentence(name: String, case: LanguageCase = LanguageCase.NOMINATIVE): String {
    val translated = tr(name)
    return when (UiLanguage.code) {
        "ru" -> declined(translated.lowercase(), case, "ий", "ого", "ом")
        "pl" -> declined(translated.lowercase(), case, "i", "iego", "im", adjective = "ki")
        "fr", "es", "pt", "el" -> translated.lowercase()
        else -> translated
    }
}

/** The adjective [nominative] in [case], its [ending] replaced; unchanged when it does not end in [adjective]. */
private fun declined(nominative: String, case: LanguageCase, ending: String, genitive: String, prepositional: String, adjective: String = ending): String {
    if (!nominative.endsWith(adjective)) return nominative
    val stem = nominative.dropLast(ending.length)
    return when (case) {
        LanguageCase.NOMINATIVE -> nominative
        LanguageCase.GENITIVE -> stem + genitive
        LanguageCase.PREPOSITIONAL -> stem + prepositional
    }
}

/**
 * A whole number with its thousands grouped as the interface language writes them: "10,000" in
 * English and Chinese, "10.000" in German, Greek, Spanish and Portuguese, "10 000" with a no-break
 * space in Russian and Polish, and with a narrow one in French.
 */
fun formatCount(n: Int): String {
    val separator = when (UiLanguage.code) {
        "ru", "pl" -> " "
        "fr" -> " "
        "de", "el", "es", "pt" -> "."
        else -> ","
    }
    val digits = if (n < 0) (-n.toLong()).toString() else n.toString()
    return (if (n < 0) "-" else "") + digits.reversed().chunked(3).joinToString(separator).reversed()
}
