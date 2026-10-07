package com.tayra.languages.core.ui.i18n

import com.tayra.languages.core.ui.i18n.ru.russianParts
import com.tayra.languages.core.ui.i18n.ru.russianStrings
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every text the sources pass to `tr` and `trPlural` as a literal has its Russian translation,
 * with the same placeholders, and no two areas translate the same text differently.
 */
class TranslationsTest {

    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }

    private val sources: List<File> = listOf("core/ui", "shared", "feature", "desktopApp", "androidApp", "webApp")
        .map { File(root, it) }
        .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" && "/src/" in it.path && "Main/" in it.path && "/build/" !in it.path }.toList() }

    @Test
    fun everyTextHasItsRussianTranslation() {
        val problems = mutableListOf<String>()
        val missing = linkedSetOf<String>()
        for (file in sources) {
            val text = file.readText()
            for (call in calls(text)) {
                val where = "${file.relativeTo(root)}:${text.substring(0, call.offset).count { it == '\n' } + 1}"
                when (call.name) {
                    "tr" -> {
                        val arg = call.args.firstOrNull() ?: continue
                        val key = literal(arg, where, problems) ?: continue
                        val russian = russianStrings[key]
                        if (russian == null) missing += key
                        else if (placeholders(russian) != placeholders(key)) problems += "$where: placeholders differ in \"$key\" → \"$russian\""
                    }
                    "trPlural" -> {
                        if (call.args.size < 3) { problems += "$where: trPlural needs a count, one and other"; continue }
                        literal(call.args[1], where, problems) ?: continue
                        val other = literal(call.args[2], where, problems) ?: continue
                        val russian = russianStrings[other]
                        when {
                            russian == null -> missing += other
                            russian.split('|').size != 3 -> problems += "$where: \"$other\" needs three Russian forms separated by |, has \"$russian\""
                        }
                    }
                }
            }
        }
        if (missing.isNotEmpty()) problems += "Missing Russian translations (${missing.size}):\n" + missing.joinToString("\n") { "    \"${escape(it)}\" to \"\"," }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }

    @Test
    fun noTextIsTranslatedTwoWays() {
        val seen = mutableMapOf<String, Pair<String, String>>()
        val conflicts = mutableListOf<String>()
        for ((part, map) in russianParts) {
            for ((key, value) in map) {
                val earlier = seen[key]
                if (earlier != null && earlier.second != value) conflicts += "\"$key\": ${earlier.first} \"${earlier.second}\", $part \"$value\""
                else seen[key] = part to value
            }
        }
        assertTrue(conflicts.isEmpty(), "Translated differently:\n" + conflicts.joinToString("\n"))
    }

    @Test
    fun russianPluralsFollowTheRussianRule() {
        assertEquals(listOf(0, 1, 2, 2, 0, 1, 2, 2), listOf(1, 2, 5, 11, 21, 22, 25, 112).map(::russianPluralIndex))
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
