package com.tayra.languages.core.domain.morphology

/**
 * Undoes inflections: Yomitan's LanguageTransformer, reading the rules tools/export_yomitan_rules.mjs
 * exported from its transform descriptors.
 *
 * A rule applies to a word its pattern matches, when the word is in one of the conditions the rule
 * starts from (any, for the word as written), and gives a word in the conditions it ends in; a
 * condition is a part of speech or an intermediate form ("-て" in Japanese, "noun plural"), and
 * conditions with sub-conditions stand for all of them. [transform] applies the rules over and over,
 * so "geschlafen" can become "schlafen" and "beautifully" "beautiful".
 */
class LanguageTransformer internal constructor(data: List<String>) {

    /** A word the text may be an inflection of, with the conditions it is in and the transforms that led to it, last first. */
    data class Deinflection(val text: String, val conditions: Int, val trace: List<String>)

    private class Transform(val id: String, val name: String, val rules: MutableList<Rule> = mutableListOf()) {
        /** Any rule's pattern, to pass over the transform quickly when none can apply. */
        val heuristic: (String) -> Boolean by lazy {
            if (rules.all { it.cheap }) {
                { text -> rules.any { it.matches(text) } }
            } else {
                val union = Regex(rules.joinToString("|") { "(?:${it.pattern})" });
                { text -> union.containsMatchIn(text) }
            }
        }
    }

    private class Rule(
        val pattern: String,
        private val ignoreCase: Boolean,
        private val guard: String,
        val conditionsIn: Int,
        val conditionsOut: Int,
        private val op: Op,
    ) {
        /** A pattern that is plain text anchored at one or both ends needs no regex. */
        private val literal: String? = pattern.removePrefix("^").removeSuffix("$").takeIf { !ignoreCase && it.none { c -> c in REGEX_SYNTAX } }
        private val anchoredStart = pattern.startsWith("^")
        private val anchoredEnd = pattern.endsWith("$") && !pattern.endsWith("\\$")
        val cheap: Boolean get() = literal != null
        private val regex: Regex by lazy { Regex(pattern, if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()) }

        fun matches(text: String): Boolean {
            val l = literal
            if (l != null) {
                return when {
                    anchoredStart && anchoredEnd -> text == l
                    anchoredStart -> text.startsWith(l)
                    anchoredEnd -> text.endsWith(l)
                    else -> text.contains(l)
                }
            }
            return regex.containsMatchIn(text)
        }

        fun apply(text: String): String? {
            if (guard.isNotEmpty() && text.startsWith(guard.substring(1)) != (guard[0] == '+')) return null
            return op.apply(text)?.takeIf { it.isNotEmpty() }
        }
    }

    private sealed interface Op {
        fun apply(text: String): String?

        /** JavaScript's text.slice(cutStart, -cutEnd) between the added letters. */
        class Slice(val cutStart: Int, val cutEnd: Int, val addStart: String, val addEnd: String) : Op {
            override fun apply(text: String): String {
                val end = text.length - cutEnd
                val middle = if (cutStart >= end) "" else text.substring(cutStart, end)
                return addStart + middle + addEnd
            }
        }

        class Constant(val word: String) : Op {
            override fun apply(text: String): String = word
        }

        /** A chain of replacements (first match only, unless global) and normalisations. */
        class Replace(val steps: List<Step>) : Op {
            override fun apply(text: String): String = steps.fold(text) { acc, step -> step.apply(acc) }
        }

        /** Yiddish plurals that change the stem's last vowel ("ע" back to "אַ"). */
        class Umlaut(val original: String, val changed: String, val cut: Int, val add: String) : Op {
            override fun apply(text: String): String? {
                if (text.length < cut) return null
                val stem = text.substring(0, text.length - cut)
                val at = stem.indexOfLast { it in YIDDISH_VOWELS }
                if (at < 0 || stem[at].toString() != changed) return null
                return stem.substring(0, at) + original + stem.substring(at + 1) + add
            }
        }
    }

    private class Step(pattern: String, flags: String, replacement: String) {
        private val normalisation = pattern.takeIf { it.startsWith("#") }
        private val regex = if (normalisation == null) Regex(pattern, if ('i' in flags) setOf(RegexOption.IGNORE_CASE) else emptySet()) else null
        private val global = 'g' in flags
        private val replacement = javaScriptReplacement(replacement)

        fun apply(text: String): String = when (normalisation) {
            "#nfd" -> Unicode.nfd(text)
            "#nfc" -> Unicode.nfc(text)
            null -> if (global) regex!!.replace(text, replacement) else regex!!.replaceFirst(text, replacement)
            else -> text
        }
    }

    private val conditionFlags = HashMap<String, Int>()
    private val dictionaryFormFlags = HashMap<String, Int>()
    private val transforms = mutableListOf<Transform>()

    init {
        val conditions = mutableListOf<Triple<String, Boolean, List<String>>>()
        var next = 0
        var current: Transform? = null
        for (line in data.asSequence().flatMap { it.split('\n') }) {
            if (line.isEmpty()) continue
            val f = line.split('\t')
            when (f[0]) {
                "C" -> conditions += Triple(f[1], f[2] == "1", f[3].split(',').filter { it.isNotEmpty() })
                "T" -> {
                    if (conditionFlags.isEmpty() && conditions.isNotEmpty()) next = assignFlags(conditions, next)
                    current = Transform(f[1], f[2]).also { transforms += it }
                }
                "R" -> {
                    val rule = Rule(
                        pattern = f[1],
                        ignoreCase = 'i' in f[2],
                        guard = f[3],
                        conditionsIn = flagsOf(f[4]),
                        conditionsOut = flagsOf(f[5]),
                        op = opOf(f.subList(6, f.size)),
                    )
                    checkNotNull(current) { "rule before any transform" }.rules += rule
                }
            }
        }
        if (conditionFlags.isEmpty() && conditions.isNotEmpty()) assignFlags(conditions, next)
    }

    private fun assignFlags(conditions: List<Triple<String, Boolean, List<String>>>, start: Int): Int {
        var next = start
        var targets = conditions
        while (targets.isNotEmpty()) {
            val waiting = mutableListOf<Triple<String, Boolean, List<String>>>()
            for (target in targets) {
                val (name, _, subConditions) = target
                if (subConditions.isEmpty()) {
                    require(next < 32) { "more than 32 conditions" }
                    conditionFlags[name] = 1 shl next++
                } else if (subConditions.all { it in conditionFlags }) {
                    conditionFlags[name] = subConditions.fold(0) { acc, sub -> acc or conditionFlags.getValue(sub) }
                } else {
                    waiting += target
                }
            }
            require(waiting.size < targets.size) { "sub-conditions form a cycle" }
            targets = waiting
        }
        for ((name, isDictionaryForm, _) in conditions) if (isDictionaryForm) dictionaryFormFlags[name] = conditionFlags.getValue(name)
        return next
    }

    private fun flagsOf(names: String): Int =
        names.split(',').filter { it.isNotEmpty() }.fold(0) { acc, name -> acc or (conditionFlags[name] ?: error("unknown condition $name")) }

    private fun opOf(f: List<String>): Op = when (f[0]) {
        "s" -> Op.Slice(f[1].toInt(), f[2].toInt(), f.getOrElse(3) { "" }, f.getOrElse(4) { "" })
        "c" -> Op.Constant(f.getOrElse(1) { "" })
        "r" -> Op.Replace(f.drop(1).chunked(3).map { Step(it[0], it.getOrElse(1) { "" }, it.getOrElse(2) { "" }) })
        "y" -> Op.Umlaut(f[1], f[2], f[3].toInt(), f.getOrElse(4) { "" })
        else -> error("unknown rule kind ${f[0]}")
    }

    /** The conditions of a dictionary word in these parts of speech, by the names the rules use ("v", "n", "adj"). */
    fun dictionaryFormConditions(names: Collection<String>): Int = names.fold(0) { acc, name -> acc or (dictionaryFormFlags[name] ?: 0) }

    /** The flags of conditions by name, sub-conditions included ("n" for both "np" and "ns"). */
    fun conditions(names: Collection<String>): Int = names.fold(0) { acc, name -> acc or (conditionFlags[name] ?: 0) }

    /** The display name of a transform ("past", "plural"), by the id a trace holds. */
    fun transformName(id: String): String = transforms.firstOrNull { it.id == id }?.name ?: id

    val ruleCount: Int get() = transforms.sumOf { it.rules.size }

    /** One step of a deinflection: the rule applied, and the text it was applied to. */
    private class Frame(val transform: String, val rule: Int, val text: String)

    private class Path(val text: String, val conditions: Int, val frames: List<Frame>)

    /** The word itself and every word the rules can turn it into, nearest first. */
    fun transform(source: String): List<Deinflection> {
        val results = mutableListOf(Path(source, 0, emptyList()))
        var i = 0
        while (i < results.size && results.size < MAX_RESULTS) {
            val (text, conditions, frames) = results[i++].let { Triple(it.text, it.conditions, it.frames) }
            for (transform in transforms) {
                if (!transform.heuristic(text)) continue
                transform.rules.forEachIndexed { j, rule ->
                    if (!conditionsMatch(conditions, rule.conditionsIn) || !rule.matches(text)) return@forEachIndexed
                    // The same rule met again on the same text along one path would loop.
                    if (frames.any { it.transform == transform.id && it.rule == j && it.text == text }) return@forEachIndexed
                    val out = rule.apply(text) ?: return@forEachIndexed
                    results += Path(out, rule.conditionsOut, listOf(Frame(transform.id, j, text)) + frames)
                }
            }
        }
        return results.map { step -> Deinflection(step.text, step.conditions, step.frames.map { it.transform }) }
    }

    companion object {
        /** True when the word is in no condition yet (as written), or shares one with [next]. */
        fun conditionsMatch(current: Int, next: Int): Boolean = current == 0 || (current and next) != 0

        private const val MAX_RESULTS = 4096
        private const val REGEX_SYNTAX = "\\^$.|?*+()[]{}"
        private const val YIDDISH_VOWELS = "עװאאַױוײיִײַיאָ"

        /** A JavaScript replacement string ("$1", "$&", "$$") in the syntax Kotlin's Regex.replace reads. */
        internal fun javaScriptReplacement(replacement: String): String = buildString {
            var i = 0
            while (i < replacement.length) {
                val c = replacement[i]
                when {
                    c == '\\' -> append("\\\\")
                    c == '$' && i + 1 < replacement.length && replacement[i + 1] == '$' -> { append("\\$"); i++ }
                    c == '$' && i + 1 < replacement.length && replacement[i + 1] == '&' -> { append("\$0"); i++ }
                    c == '$' && i + 1 < replacement.length && replacement[i + 1].isDigit() -> append('$')
                    c == '$' -> append("\\$")
                    else -> append(c)
                }
                i++
            }
        }
    }
}
