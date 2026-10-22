package com.tayra.languages.core.domain.parse

import kotlin.math.ln

/**
 * Arabic: each word cut into the letters written onto its front and the word they are on (والكتاب is و + ال + كتاب,
 * بالبيت ب + ال + بيت, سيكتب س + يكتب), which then read as words of their own, as Korean particles do: a word keeps one
 * status however it is written. In front come the conjunction و or ف, then a preposition ب, ك or ل, the article ال,
 * the two together (بال, كال; ل with ال is لل, its alif dropped, and stays one piece: للبيت is لل + بيت), or the
 * future's س before a verb's imperfect. A pronoun after a word stays on it (كتابه, عليها).
 *
 * A word is cut only where what is left is a word the lexicon knows, written as it is or with a pronoun after it
 * (وكتابهم), of two letters or more. A word the frequency list has stays whole (ولد, كان, لكن, فيلم, برد): the list has
 * the words that are more often themselves than letters and a word. So may a headword of the list with a pronoun after
 * it (كلنا of كل, بابي of باب, not ب + أبي). Of the cuts and that, the one whose word is the most common wins, by the
 * costs SegmentingParser gives Chinese words, the letters in front costing the log of their rank alone. After the
 * conjunction a word of the list is cut no further (ولأن is و + لأن, as لأن is whole), and after لل a word of the list
 * is the article's (للبنك is لل + بنك, not ل + لبنك). A word only a dictionary has stays whole unless a cut leaves a
 * word of the list (لبنان is no ل + بنان; the dictionary's forms with ال, العالم, are cut).
 *
 * فقد and فهم, words of the list, are as often ف and a word, told apart by the word after them: فقد "lost" takes a
 * noun, ف + قد a verb, so فقد is whole before a word with the article or at the end of a sentence, else ف + قد; فهم
 * "understood" is ف + هم "so they" before what only follows a pronoun (فهم لا, فهم في, فهم يعملون).
 *
 * The vowel signs are no part of a word: they are left out of the words matched and saved ([lowercase]), and a word
 * written with them is cut as one without, each piece keeping its own.
 *
 * tools/build_frequency_list.py counts words cut the same way (arabic_pieces); keep the two the same.
 */
class ArabicParser : SpaceDelimitedParser(), LexiconParser {

    override val name: String get() = "Arabic"

    override fun lowercase(text: String): String = text.filterNot { isMark(it) }.lowercase()

    override fun words(word: String): List<String> = words(word, null, null)

    override fun words(word: String, previous: String?, next: String?): List<String> {
        val lexicon = Lexicons[CODE] ?: return listOf(word)
        // Where each letter is in the word as written, marks left out.
        val letters = word.indices.filter { !isMark(word[it]) }
        val pieces = cut(letters.map { word[it] }.joinToString(""), lexicon, next?.filterNot { isMark(it) })
        if (pieces.size == 1) return listOf(word)
        val out = mutableListOf<String>()
        var at = 0
        for (piece in pieces) {
            val end = at + piece.length
            out += word.substring(letters[at], if (end == letters.size) word.length else letters[end])
            at = end
        }
        return out
    }

    /** [word], written without its vowel signs, as the reader cuts it before [next] (the word after it, or null). */
    fun cut(word: String, lexicon: Lexicon, next: String? = null): List<String> {
        contextCut(word, next)?.let { return it }
        if (lexicon.rank(word) != null) return listOf(word)
        var best: Double? = null
        var cut = listOf(word)
        var afterListed = false
        // A headword, not another's form (فعليك is ف + عليك, no فعلي + ك), nor a word with the article (الفهم is ال + فهم).
        val owned = bases(word).filter { !it.startsWith(ARTICLE) }.mapNotNull { lexicon.headRank(it) }.minOrNull()
        if (owned != null) {
            best = ln(owned.toDouble()) + WORD_COST
            afterListed = true
        }
        for ((clitics, stem) in analyses(word)) {
            if (clitics.size > 1 && clitics[0] in CONJUNCTIONS && lexicon.rank(word.substring(1)) != null) continue
            if (clitics.lastOrNull() == "ل" && stem.startsWith("ل") && lexicon.rank(stem.substring(1)) != null) continue
            val bases = listOf(stem) + bases(stem)
            val ranked = bases.firstNotNullOfOrNull { lexicon.rank(it) }
            if (ranked == null && bases.none { it in lexicon }) continue
            if (ranked != null) afterListed = true
            val cost = clitics.sumOf { ln((lexicon.rank(it) ?: UNRANKED).toDouble()) } + ln((ranked ?: UNRANKED).toDouble()) + WORD_COST
            if (best == null || cost < best) {
                best = cost
                cut = clitics + stem
            }
        }
        if (best == null || word in lexicon && !afterListed) return listOf(word)
        return cut
    }

    /** فقد and فهم cut by [next], the word after them; null for any other word. */
    private fun contextCut(word: String, next: String?): List<String>? {
        val stem = CONTEXT_WORDS[word] ?: return null
        val whole = when {
            next == null -> true
            word == "فقد" -> next.startsWith(ARTICLE)
            else -> next !in THEY_BEFORE && !THEY_VERB.matches(next)
        }
        return if (whole) listOf(word) else listOf("ف", stem)
    }

    /** Every way [word] is the letters in front and a word of two letters or more. */
    private fun analyses(word: String): List<Pair<List<String>, String>> {
        val out = mutableListOf<Pair<List<String>, String>>()
        for (conjunction in listOf("") + CONJUNCTIONS) {
            if (!word.startsWith(conjunction)) continue
            val rest = word.substring(conjunction.length)
            val head = if (conjunction.isEmpty()) emptyList() else listOf(conjunction)
            val options = mutableListOf<Pair<List<String>, String>>()
            if (conjunction.isNotEmpty()) options += emptyList<String>() to rest
            if (rest.startsWith(FOR_THE)) options += listOf(FOR_THE) to rest.substring(FOR_THE.length)
            for (preposition in PREPOSITIONS) {
                if (!rest.startsWith(preposition)) continue
                options += listOf(preposition) to rest.substring(preposition.length)
                if (preposition != "ل" && rest.startsWith(ARTICLE, preposition.length)) {
                    options += listOf(preposition, ARTICLE) to rest.substring(preposition.length + ARTICLE.length)
                }
            }
            if (rest.startsWith(ARTICLE)) options += listOf(ARTICLE) to rest.substring(ARTICLE.length)
            if (rest.startsWith(FUTURE) && rest.length > 1 && rest[1] in IMPERFECT) options += listOf(FUTURE) to rest.substring(FUTURE.length)
            for ((clitics, stem) in options) if (stem.length >= 2) out += (head + clitics) to stem
        }
        return out
    }

    /**
     * The words [word] may be with a pronoun after it, as they are written before one: كتاب of كتابه, مدرسة of
     * مدرستي, على of عليه, بنى of بناها, كتبوا of كتبوه; ب and ل take them too (به, لك).
     */
    private fun bases(word: String): List<String> {
        val out = mutableListOf<String>()
        for (suffix in SUFFIXES) {
            val base = word.dropLast(suffix.length)
            if (!word.endsWith(suffix) || base.isEmpty() || base.length == 1 && base !in SUFFIXED_PREPOSITIONS) continue
            out += base
            when {
                base.endsWith("ت") -> out += base.dropLast(1) + "ة"
                base.endsWith("ي") || base.endsWith("ا") -> out += base.dropLast(1) + "ى"
                base.endsWith("و") -> out += base + "ا"
            }
        }
        return out
    }

    private companion object {
        const val CODE = "ar"
        const val WORD_COST = 4.0
        const val UNRANKED = 50_000

        val CONJUNCTIONS = listOf("و", "ف")
        val PREPOSITIONS = listOf("ب", "ك", "ل")
        const val ARTICLE = "ال"
        const val FOR_THE = "لل"
        const val FUTURE = "س"

        /** The words cut by the word after them ([contextCut]), each with the word after its ف. */
        val CONTEXT_WORDS = mapOf("فقد" to "قد", "فهم" to "هم")

        /** What follows هم "they" and never فهم "understood": a negation, قد, في, and the like. */
        val THEY_BEFORE = setOf("لا", "لم", "لن", "قد", "في", "ليسوا", "كانوا", "أيضا", "جميعا")

        /** A verb's plural present (يعملون, تعرفون, سيذهبون). */
        val THEY_VERB = Regex("^[يتس].{2,}ون$")

        /** The letters a verb's imperfect begins with, which the future's س comes before. */
        const val IMPERFECT = "يتنأ"

        /** The pronouns joined to the end of a word, longest first, but كن, seldom one and the end of many a word (يمكن, لنكن). */
        val SUFFIXES = listOf("هما", "كما", "هم", "هن", "كم", "ها", "نا", "ني", "ه", "ك", "ي")
        val SUFFIXED_PREPOSITIONS = setOf("ب", "ل")

        /** Whether [c] is a vowel sign, shadda, sukun, the dagger alif or the tatweel. */
        fun isMark(c: Char): Boolean = c in '\u064B'..'\u065F' || c == '\u0670' || c == '\u0640'
    }
}
