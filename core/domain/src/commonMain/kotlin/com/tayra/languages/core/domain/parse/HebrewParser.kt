package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.model.Language
import kotlin.math.ln

/**
 * Hebrew: each word cut into the letters written onto its front and the word they are on (ובבית is ו + ב + בית, מהבית
 * מ + ה + בית, שהילד ש + ה + ילד, וכשהוא ו + כש + הוא), which then read as words of their own, as Arabic's do: a word
 * keeps one status however it is written. In front come the conjunction ו, then ש, כש, מש or לכש ("that", "when"), then
 * a preposition ב, כ, ל or מ, then the article ה, which is not written after ב, כ and ל (בבית is "in the house"). A word
 * in ו doubles it after them (הוועדה is ה + וועדה, ועדה's spelling there). A pronoun after a word stays on it (ביתו,
 * עליה).
 *
 * A word is cut only where what is left is a word the lexicon knows, as it is, with its ו doubled, or with a pronoun
 * after it (לביתו), of two letters or more. A word the frequency list has stays whole (הלך, לפני, שלום, להיות, שלי): the
 * list has the words that are more often themselves than letters and a word. So may a headword of the list with a
 * pronoun after it (שלה of של, ביתה of בית), and a form of the list with one where no cut leaves a word of the list. Of
 * the cuts and that, the one whose word is the most common wins, by the costs SegmentingParser gives Chinese words, the
 * letters in front costing the log of their rank alone. After the conjunction a word of the list is cut no further
 * (ולפני is ו + לפני). A word only a dictionary has stays whole unless a cut leaves a word of the list (לייבא, an
 * infinitive, is no ל + ייבא).
 *
 * The points and cantillation are no part of a word: they are left out of the words matched and saved ([lowercase]), and
 * a word written with them is cut as one without, each piece keeping its own. An apostrophe after a Hebrew letter is the
 * geresh and a double quote between two the gershayim (ג'ינס is ג׳ינס, צה"ל צה״ל), as the list writes them.
 *
 * tools/build_frequency_list.py counts words cut the same way (hebrew_pieces); keep the two the same.
 */
class HebrewParser : SpaceDelimitedParser(), LexiconParser {

    override val name: String get() = "Hebrew"

    override fun parse(text: String, language: Language): List<ParsedToken> = super.parse(spelled(text), language)

    override fun lowercase(text: String): String = spelled(text).filterNot { isMark(it) }.lowercase()

    override fun words(word: String): List<String> = words(word, null, null)

    override fun words(word: String, previous: String?, next: String?): List<String> {
        val lexicon = Lexicons[CODE] ?: return listOf(word)
        // Where each letter is in the word as written, marks left out.
        val letters = word.indices.filter { !isMark(word[it]) }
        val pieces = cut(letters.map { word[it] }.joinToString(""), lexicon)
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

    /** [word], written without its marks, as the reader cuts it. */
    fun cut(word: String, lexicon: Lexicon): List<String> {
        if (lexicon.rank(word) != null) return listOf(word)
        var best: Double? = null
        var cut = listOf(word)
        var afterListed = false
        val owned = bases(word).mapNotNull { lexicon.headRank(it) }.minOrNull()
        if (owned != null) {
            best = ln(owned.toDouble()) + WORD_COST
            afterListed = true
        }
        for ((clitics, stem) in analyses(word)) {
            if (clitics.size > 1 && clitics[0] == CONJUNCTION && lexicon.rank(word.substring(1)) != null) continue
            val spellings = spellings(stem)
            val bases = spellings + spellings.flatMap { bases(it) }
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
        // Nor is a form of the list with a pronoun after it cut where no cut leaves a word of the list.
        val formed = bases(word).any { base -> lexicon.rank(base) != null && bases(base).none { lexicon.rank(it) != null } }
        if (!afterListed && formed) return listOf(word)
        return cut
    }

    /** Every way [word] is the letters in front and a word of two letters or more. */
    private fun analyses(word: String): List<Pair<List<String>, String>> {
        val out = mutableListOf<Pair<List<String>, String>>()
        for (conjunction in listOf("", CONJUNCTION)) {
            if (!word.startsWith(conjunction)) continue
            val after = word.substring(conjunction.length)
            for (relative in listOf("") + RELATIVES) {
                if (!after.startsWith(relative)) continue
                val rest = after.substring(relative.length)
                for (preposition in listOf("") + PREPOSITIONS) {
                    if (!rest.startsWith(preposition)) continue
                    val stem = rest.substring(preposition.length)
                    val clitics = listOf(conjunction, relative, preposition).filter { it.isNotEmpty() }
                    val options = mutableListOf(clitics to stem)
                    if ((preposition.isEmpty() || preposition == "מ") && stem.startsWith(ARTICLE)) options += (clitics + ARTICLE) to stem.substring(1)
                    for ((c, s) in options) if (c.isNotEmpty() && s.length >= 2) out += c to s
                }
            }
        }
        return out
    }

    /** The words [word] may be with a pronoun after it: דרך of דרכי, ארץ of ארצו, עבודה of עבודתו, בית of ביתה. */
    private fun bases(word: String): List<String> {
        val out = mutableListOf<String>()
        for (suffix in SUFFIXES) {
            if (!word.endsWith(suffix) || word.length - suffix.length < 2) continue
            val bare = word.dropLast(suffix.length)
            val base = bare.dropLast(1) + (FINALS[bare.last()] ?: bare.last())
            out += base
            if (base.endsWith("ת")) out += base.dropLast(1) + "ה"
        }
        return out
    }

    /** [stem] as written after the letters in front, and as the word it is when that doubles its first ו (וועדה, ועדה). */
    private fun spellings(stem: String): List<String> = if (stem.startsWith("וו") && stem.length > 3) listOf(stem, stem.substring(1)) else listOf(stem)

    private companion object {
        const val CODE = "he"
        const val WORD_COST = 4.0
        const val UNRANKED = 50_000

        const val CONJUNCTION = "ו"
        val RELATIVES = listOf("ש", "כש", "מש", "לכש")
        val PREPOSITIONS = listOf("ב", "כ", "ל", "מ")
        const val ARTICLE = "ה"

        /** The pronouns joined to the end of a word, longest first. */
        val SUFFIXES = listOf("יהם", "יהן", "יכם", "יכן", "ינו", "הם", "הן", "כם", "כן", "נו", "ני", "יו", "יה", "יך", "ו", "ה", "ך", "י")

        /** A letter's shape at the end of a word, which it loses before a pronoun (דרכי of דרך). */
        val FINALS = mapOf('כ' to 'ך', 'מ' to 'ם', 'נ' to 'ן', 'פ' to 'ף', 'צ' to 'ץ')

        /** An apostrophe after a Hebrew letter, the geresh; a double quote between two, the gershayim. */
        val GERESH = Regex("(?<=[\\u05D0-\\u05EA])['\\u2019]")
        val GERSHAYIM = Regex("(?<=[\\u05D0-\\u05EA])\"(?=[\\u05D0-\\u05EA])")

        fun spelled(text: String): String = text.replace(GERESH, "׳").replace(GERSHAYIM, "״")

        /** Whether [c] is a vowel point or a cantillation mark: not the maqaf, the paseq or the sof pasuq. */
        fun isMark(c: Char): Boolean =
            c in '֑'..'ֽ' || c == 'ֿ' || c == 'ׁ' || c == 'ׂ' || c == 'ׄ' || c == 'ׅ' || c == 'ׇ'
    }
}
