package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.model.Language

/**
 * Lao: a stretch of letters cut into its likeliest words by how common they are, as [ThaiParser] cuts Thai, but never inside
 * a syllable. A word does not end before a vowel, tone mark or subscript ລ written after its letter (ຕໍ່ມາ is ຕໍ່ + ມາ), nor
 * after a vowel written before one (ເ, ແ, ໂ, ໃ, ໄ), nor between ຫ and the letter it leads (ຫງ, ຫຍ, ຫວ). A letter with no
 * vowel after it ends the syllable before it (ຕາມ is one word, not ຕາ "eye" + ມ; ທຸກມື້ is ທຸກ + ມື້), unless ວ or ອ follow
 * it (ຄວາມ, ສວນ, ຄອງ), or it is one that ends no syllable and ຣ and a vowel follow it, a cluster (ໃຫ້ຝຣັ່ງ is ໃຫ້ + ຝຣັ່ງ),
 * and a letter that begins a syllable, with no vowel yet, is never a word's end (ຄວາມ is no ຄ + ວາມ, ຝຣັ່ງ no ຝ + ຣັ່ງ).
 * A letter alone is no word, though the dictionary has the letters; letters no word covers go a syllable at a time, together
 * one word. ໆ, which repeats the word before it, and the Lao digits are no words.
 *
 * As in Thai, some grammatical words always stand apart, though the lexicon has them joined to the word next to them: the
 * negator ບໍ່ before a word (ບໍ່ດີ is ບໍ່ + ດີ), and after one the complementizer ວ່າ and the aspect word ຢູ່ (ຄິດວ່າ is ຄິດ +
 * ວ່າ, ອາໃສຢູ່ ອາໃສ + ຢູ່). Not ກໍ່, which begins words of its own (ກໍ່ສ້າງ "build", ກໍລະກົດ "July").
 *
 * Text is read in today's spelling: ໍ + າ as ຳ, ຫນ, ຫມ and ຫລ as ໜ, ໝ and ຫຼ, so a word reads the same however it was typed;
 * the lexicon's words are spelled so too ([lexiconWord]).
 *
 * tools/build_frequency_list.py (lao_break, lao_normalise, segmenter) and tools/check_course.py (lao_spans) cut Lao the same
 * way; keep the three the same.
 */
class LaoParser : SegmentingParser("Lao", "lo", joinUnknown = true, byFrequency = true) {

    override fun parse(text: String, language: Language): List<ParsedToken> = super.parse(normalise(text), language)

    override fun lowercase(text: String): String = normalise(text).lowercase()

    override fun lexiconWord(word: String): String = normalise(word)

    override fun canBreak(run: String, at: Int): Boolean {
        if (at <= 0 || at >= run.length) return true
        val c = run[at]
        val before = run[at - 1]
        return c !in FOLLOWING && before !in LEADING && !(before == 'ຫ' && c in LED) &&
            !(isLetter(c) && !beginsSyllable(run, at)) && !(isLetter(before) && (isBare(run, at - 1) || leadsCluster(run, at - 1)))
    }

    override fun standsApart(c: Char): Boolean = c == KO_LA || c in LAO_DIGITS

    override fun matches(text: String, lexicon: Lexicon): Boolean = text in lexicon && !isLetterAlone(text) && !joinsGrammaticalWord(text, lexicon)

    /** Whether [word] is one of [BEFORE] and words after it, or words and one of [AFTER], cut between syllables. */
    private fun joinsGrammaticalWord(word: String, lexicon: Lexicon): Boolean =
        BEFORE.any { word.length > it.length && word.startsWith(it) && canBreak(word, it.length) && cut(word, it.length, word.length, lexicon) } ||
            AFTER.any { word.length > it.length && word.endsWith(it) && canBreak(word, word.length - it.length) && cut(word, 0, word.length - it.length, lexicon) }

    /** Whether [word] from [from] to [to] can be cut between syllables into words of the lexicon, none a letter alone. */
    private fun cut(word: String, from: Int, to: Int, lexicon: Lexicon): Boolean {
        val reached = BooleanArray(to + 1)
        reached[from] = true
        for (start in from until to) {
            if (!reached[start]) continue
            for (end in start + 1..to) {
                val piece = word.substring(start, end)
                if (canBreak(word, end) && piece in lexicon && !isLetterAlone(piece)) reached[end] = true
            }
        }
        return reached[to]
    }

    /**
     * Whether the letter at [at] can begin a syllable: a vowel or mark follows it, or ວ or ອ, or it is ຫ leading a letter,
     * or it leads a cluster ([leadsCluster]).
     */
    private fun beginsSyllable(run: String, at: Int): Boolean {
        val next = run.getOrNull(at + 1) ?: return false
        return next in FOLLOWING || next in VOWEL_LETTERS || (run[at] == 'ຫ' && next in LED) || leadsCluster(run, at)
    }

    /** Whether the letter at [at] is one that ends no syllable with ຣ and a vowel after it, one syllable: ຝຣັ່ງ, ອາຟຣິກາ. */
    private fun leadsCluster(run: String, at: Int): Boolean =
        run[at] in CLUSTERING && run.getOrNull(at + 1) == 'ຣ' && run.getOrNull(at + 2)?.let { it in FOLLOWING } == true

    /**
     * Whether the letter at [at] begins a syllable with no vowel of its own yet: first, or after a letter that ends one (not
     * a letter after a vowel written before it, as ທ of ເທດ, whose next letter ends its syllable).
     */
    private fun isBare(run: String, at: Int): Boolean = at == 0 ||
        (isLetter(run[at - 1]) && run[at - 1] !in VOWEL_LETTERS && run[at - 1] != 'ຍ' && (at < 2 || run[at - 2] !in LEADING))

    companion object {
        /** The vowels and marks written after or above or below the letter they go with, and the subscript ລ. */
        private const val FOLLOWING = "ະັາຳິີຶື຺ຸູົຼຽ່້໊໋໌ໍ໎"

        /** The vowels written before the letter they go with. */
        private const val LEADING = "ເແໂໃໄ"

        /** The letters a ຫ leads, which take its tone. */
        private const val LED = "ງຍນມລວຣ"

        /** The letters that are vowels too (ຄວາມ, ສວນ, ຄອງ, ເມືອງ), after which a letter may still end a syllable. */
        private const val VOWEL_LETTERS = "ວອ"

        /** The letters that end no syllable and lead ຣ in a cluster (ຝຣັ່ງ, ພຣະ, ຄຣິດ); ກ, ດ, ບ before ຣ mostly end one (ອັບຣາຮາມ). */
        private const val CLUSTERING = "ຂຄຕທປຜຝພຟ"

        private const val KO_LA = 'ໆ'
        private val LAO_DIGITS = '໐'..'໙'

        /** The grammatical words that join no word after them, and those that join none before them. */
        private val BEFORE = listOf("ບໍ່")
        private val AFTER = listOf("ວ່າ", "ຢູ່")

        private fun isLetterAlone(text: String): Boolean = text.length == 1 && isLetter(text[0])

        private fun isLetter(c: Char): Boolean = c in 'ກ'..'ຮ' || c in 'ໜ'..'ໟ'

        private val SPLIT_AM = Regex("ໍ([່-໋]?)າ")

        /** [text] in today's spelling: ໍ + າ as ຳ, the tone mark first; ຫນ, ຫມ, ຫລ as ໜ, ໝ, ຫຼ. */
        fun normalise(text: String): String {
            if (text.none { it in '຀'..'໿' }) return text
            return SPLIT_AM.replace(text, "$1ຳ").replace("ຫນ", "ໜ").replace("ຫມ", "ໝ").replace("ຫລ", "ຫຼ")
        }
    }
}
