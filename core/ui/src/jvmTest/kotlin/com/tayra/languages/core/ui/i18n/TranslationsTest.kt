package com.tayra.languages.core.ui.i18n

import com.tayra.languages.core.ui.i18n.de.germanParts
import com.tayra.languages.core.ui.i18n.el.greekParts
import com.tayra.languages.core.ui.i18n.es.spanishParts
import com.tayra.languages.core.ui.i18n.fr.frenchParts
import com.tayra.languages.core.ui.i18n.pl.polishParts
import com.tayra.languages.core.ui.i18n.pt.portugueseParts
import com.tayra.languages.core.ui.i18n.ru.russianParts
import com.tayra.languages.core.ui.i18n.zh.chineseParts
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every text the sources pass to `tr` and `trPlural` as a literal has its translation in every
 * interface language, with the same placeholders and as many plural forms as the language has;
 * every language translates the texts the others do, and no two areas translate a text differently.
 */
class TranslationsTest {

    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }

    private val sources: List<File> = listOf("core/ui", "shared", "feature", "desktopApp", "androidApp", "webApp")
        .map { File(root, it) }
        .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" && "/src/" in it.path && "Main/" in it.path && "/build/" !in it.path }.toList() }

    /** Each translation's areas, by its language's code. */
    private val parts: Map<String, List<Pair<String, Map<String, String>>>> = mapOf(
        "zh" to chineseParts, "fr" to frenchParts, "de" to germanParts, "el" to greekParts,
        "pl" to polishParts, "pt" to portugueseParts, "ru" to russianParts, "es" to spanishParts,
    )

    @AfterTest
    fun backToEnglish() = UiLanguage.set("en")

    @Test
    fun everyLanguageIsTranslated() {
        assertEquals(UiLanguage.supported.filter { it != "en" }.toSet(), parts.keys)
    }

    @Test
    fun everyTextHasItsTranslations() {
        val problems = mutableListOf<String>()
        val missing = mutableMapOf<String, MutableSet<String>>()
        for (file in sources) {
            val text = file.readText()
            for (call in calls(text)) {
                val where = "${file.relativeTo(root)}:${text.substring(0, call.offset).count { it == '\n' } + 1}"
                when (call.name) {
                    "tr" -> {
                        val arg = call.args.firstOrNull() ?: continue
                        val key = literal(arg, where, problems) ?: continue
                        for (code in parts.keys) {
                            val translated = translationOf(code)!![key]
                            if (translated == null) missing.getOrPut(code) { linkedSetOf() } += key
                            else if (placeholders(translated) != placeholders(key)) problems += "$where: $code placeholders differ in \"$key\" → \"$translated\""
                        }
                    }
                    "trPlural" -> {
                        if (call.args.size < 3) { problems += "$where: trPlural needs a count, one and other"; continue }
                        literal(call.args[1], where, problems) ?: continue
                        val other = literal(call.args[2], where, problems) ?: continue
                        for (code in parts.keys) {
                            val translated = translationOf(code)!![other]
                            when {
                                translated == null -> missing.getOrPut(code) { linkedSetOf() } += other
                                translated.split('|').size != pluralForms(code) -> problems += "$where: $code \"$other\" needs ${pluralForms(code)} forms separated by |, has \"$translated\""
                            }
                        }
                    }
                }
            }
        }
        for ((code, keys) in missing) problems += "Missing $code translations (${keys.size}):\n" + keys.joinToString("\n") { "    \"${escape(it)}\" to \"\"," }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    /** Texts that come from data, such as language names and months, are translated in every language too. */
    @Test
    fun everyLanguageTranslatesTheSameTexts() {
        val russian = translationOf("ru")!!.keys
        for (code in parts.keys) {
            val keys = translationOf(code)!!.keys
            assertTrue(keys == russian, "$code lacks ${(russian - keys).take(10)} and has ${(keys - russian).take(10)} besides")
        }
    }

    @Test
    fun noTextIsTranslatedTwoWays() {
        val conflicts = mutableListOf<String>()
        for ((code, areas) in parts) {
            val seen = mutableMapOf<String, Pair<String, String>>()
            for ((part, map) in areas) {
                for ((key, value) in map) {
                    val earlier = seen[key]
                    if (earlier != null && earlier.second != value) conflicts += "$code \"$key\": ${earlier.first} \"${earlier.second}\", $part \"$value\""
                    else seen[key] = part to value
                }
            }
        }
        assertTrue(conflicts.isEmpty(), "Translated differently:\n" + conflicts.joinToString("\n"))
    }

    @Test
    fun pluralsFollowEachLanguagesRule() {
        val counts = listOf(0, 1, 2, 5, 11, 21, 22, 25, 112)
        assertEquals(listOf(2, 0, 1, 2, 2, 0, 1, 2, 2), counts.map { pluralIndex("ru", it) })
        assertEquals(listOf(2, 0, 1, 2, 2, 2, 1, 2, 2), counts.map { pluralIndex("pl", it) })
        assertEquals(listOf(0, 0, 1, 1, 1, 1, 1, 1, 1), counts.map { pluralIndex("fr", it) })
        assertEquals(listOf(1, 0, 1, 1, 1, 1, 1, 1, 1), counts.map { pluralIndex("de", it) })
        assertEquals(List(counts.size) { 0 }, counts.map { pluralIndex("zh", it) })
    }

    @Test
    fun aLanguageNameFitsTheSentence() {
        UiLanguage.set("pl")
        assertEquals(listOf("portugalski", "portugalskiego", "portugalskim"), LanguageCase.entries.map { languageInSentence("Portuguese", it) })
        assertEquals("esperanto", languageInSentence("Esperanto", LanguageCase.GENITIVE), "a name that is no adjective")
        UiLanguage.set("ru")
        assertEquals("португальском", languageInSentence("Portuguese", LanguageCase.PREPOSITIONAL))
        UiLanguage.set("fr")
        assertEquals("portugais", languageInSentence("Portuguese", LanguageCase.PREPOSITIONAL))
        UiLanguage.set("de")
        assertEquals("Portugiesisch", languageInSentence("Portuguese"))
        assertEquals("12.345", formatCount(12345))
        UiLanguage.set("fr")
        assertEquals("12\u202F345", formatCount(12345))
    }

    private class Call(val name: String, val offset: Int, val args: List<String>)

    /** The `tr(` and `trPlural(` calls in a source, with their arguments as written. */
    private fun calls(text: String): List<Call> {
        val found = mutableListOf<Call>()
        Regex("""(?<![\w.$])(tr|trPlural)\(""").findAll(text).forEach { match ->
            // Skips the declarations themselves.
            if (text.substring(maxOf(0, match.range.first - 4), match.range.first).endsWith("fun ")) return@forEach
            found += Call(match.groupValues[1], match.range.first, arguments(text, match.range.last + 1))
        }
        return found
    }

    /** The top-level arguments from [start], just after the opening parenthesis. */
    private fun arguments(text: String, start: Int): List<String> {
        val args = mutableListOf<String>()
        var depth = 0
        var i = start
        var from = start
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' -> i = skipString(text, i) - 1
                c == '(' || c == '[' || c == '{' -> depth++
                (c == ')' || c == ']' || c == '}') && depth > 0 -> depth--
                c == ')' -> { args += text.substring(from, i).trim(); return args.filter { it.isNotEmpty() } }
                c == ',' && depth == 0 -> { args += text.substring(from, i).trim(); from = i + 1 }
            }
            i++
        }
        return args
    }

    private fun skipString(text: String, start: Int): Int {
        if (text.startsWith("\"\"\"", start)) return text.indexOf("\"\"\"", start + 3).let { if (it < 0) text.length else it + 3 }
        var i = start + 1
        while (i < text.length && text[i] != '"') i += if (text[i] == '\\') 2 else 1
        return i + 1
    }

    /** The text of a plain string literal argument; null for other expressions, with a problem for literals that cannot be keys. */
    private fun literal(arg: String, where: String, problems: MutableList<String>): String? {
        if (!arg.startsWith('"')) return null
        if (arg.startsWith("\"\"\"") || skipString(arg, 0) != arg.length) {
            problems += "$where: pass one plain string literal, not $arg"
            return null
        }
        val body = arg.substring(1, arg.length - 1)
        if (Regex("""(?<!\\)\$[{\w]""").containsMatchIn(body)) {
            problems += "$where: no string templates in translated text, use {0}: $arg"
            return null
        }
        return unescape(body)
    }

    private fun unescape(s: String): String = buildString {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    'n' -> append('\n'); 't' -> append('\t'); 'r' -> append('\r')
                    'u' -> { append(s.substring(i + 2, i + 6).toInt(16).toChar()); i += 4 }
                    else -> append(n)
                }
                i += 2
            } else {
                append(c); i++
            }
        }
    }

    private fun escape(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("$", "\\$")

    private fun placeholders(s: String) = Regex("""\{\d+}""").findAll(s).map { it.value }.toSet()
}
