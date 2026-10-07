package com.tayra.languages.core.ui.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tayra.languages.core.ui.i18n.ru.russianStrings

/**
 * The language the interface is shown in. The app sets it from the settings; it is snapshot
 * state, so every composable that called [tr] recomposes when it changes.
 */
object UiLanguage {
    /** Codes of the languages the interface is translated into, English first. */
    val supported: List<String> = listOf("en", "ru")

    var code: String by mutableStateOf("en")
        private set

    fun set(code: String) {
        this.code = code.takeIf { it in supported } ?: "en"
    }
}

/**
 * The interface text [english] in the current [UiLanguage]; `{0}`, `{1}`… are replaced by [args].
 *
 * The English text is the key: each translation (see [russianStrings]) maps it to its own
 * wording, and text without a translation stays in English. Pass a single string literal, so
 * `TranslationsTest` can check that every text has its translation; `tr(name)` for text that
 * comes from data (language names, themes) is fine too.
 */
fun tr(english: String, vararg args: Any?): String = format(translated(english), args)

/**
 * A text that depends on [count], written in English as its [one] and [other] forms with `{0}`
 * for the count and `{1}`… for [args]. A Russian translation, keyed by [other], gives its three
 * forms separated by `|`: one (1, 21), few (2–4, 22–24) and many (5–20, 25).
 */
fun trPlural(count: Int, one: String, other: String, vararg args: Any?): String {
    val all = arrayOf<Any?>(count, *args)
    if (UiLanguage.code != "ru") return format(if (count == 1) one else other, all)
    val forms = russianStrings[other]?.split('|') ?: return format(if (count == 1) one else other, all)
    return format(forms[russianPluralIndex(count).coerceAtMost(forms.lastIndex)], all)
}

private fun translated(english: String): String = when (UiLanguage.code) {
    "ru" -> russianStrings[english] ?: english
    else -> english
}

/** 0 for one, 1 for few, 2 for many. */
internal fun russianPluralIndex(count: Int): Int {
    val n = if (count < 0) -count else count
    return when {
        n % 10 == 1 && n % 100 != 11 -> 0
        n % 10 in 2..4 && n % 100 !in 12..14 -> 1
        else -> 2
    }
}

private fun format(text: String, args: Array<out Any?>): String {
    if (args.isEmpty()) return text
    var out = text
    args.forEachIndexed { i, arg -> out = out.replace("{$i}", arg.toString()) }
    return out
}
