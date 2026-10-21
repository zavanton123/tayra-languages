package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.morphology.ChineseScripts
import kotlin.math.ln

/**
 * A parser for languages written without spaces between words (Chinese, Japanese, Thai, Lao,
 * Khmer): a stretch of the language's letters is cut into the longest words its lexicon knows,
 * scanning from the left as Yomitan does when it looks text up. [matches] may accept more than
 * the lexicon's own words, such as an inflection of one. Letters no word covers become a word
 * each ([joinUnknown] false: every Chinese character is a word of its own) or one word together.
 *
 * Where the language has an analyser ([Segmenters]), a word never runs past the analyser's word
 * ("かれはいしゃ" is "he is a doctor", not "he, dentist"), and within one it is the longest run of
 * the analyser's pieces the lexicon knows, so 選んでください is 選んで and ください, both looked up.
 *
 * With [byFrequency] and a lexicon that knows how common its words are, a stretch is cut into its
 * likeliest words instead ([likeliest]): Chinese read by the longest word first goes wrong where two
 * common words make a rarer one (不知道 read as 不知 + 道, 从中国 as 从中 + 国).
 */
open class SegmentingParser(
    override val name: String,
    private val code: String,
    private val joinUnknown: Boolean,
    private val byFrequency: Boolean = false,
) : LexiconParser {

    /** How much longer than the lexicon's longest word a stretch may be, for the inflections [matches] accepts. */
    protected open val inflectionLength: Int = 0

    /** Whether [text] is a word: by default, one the lexicon lists. */
    protected open fun matches(text: String, lexicon: Lexicon): Boolean = text in lexicon

    override fun parse(text: String, language: Language): List<ParsedToken> {
        // Chinese is read in the language's script, whichever one the text was written in, so its words
        // are kept in that script and a word reads the same in a simplified and a traditional text.
        val written = language.chineseScript?.let { ChineseScripts.convert(text, it) } ?: text
        val prepared = applyCharacterSubstitutions(written, language.characterSubstitutions)
            .replace("\r\n", "\n")
            .replace('{', '[')
            .replace('}', ']')
        val wordChar = Regex("[${normalizeRegexEscapes(language.wordCharacters.trim()).ifEmpty { SpaceDelimitedParser.DEFAULT_WORD_CHARACTERS }}]")
        val splitChars = language.regexpSplitSentences.trim().ifEmpty { SpaceDelimitedParser.DEFAULT_SPLIT_SENTENCES }
        val lexicon = Lexicons[code]
        val tokens = mutableListOf<ParsedToken>()
        val paragraphs = prepared.split("\n")
        paragraphs.forEachIndexed { index, paragraph ->
            var i = 0
            while (i < paragraph.length) {
                val isWord = wordChar.matches(paragraph[i].toString())
                var j = i + 1
                while (j < paragraph.length && wordChar.matches(paragraph[j].toString()) == isWord) j++
                val run = paragraph.substring(i, j)
                if (isWord) {
                    segment(run, lexicon).forEach { tokens += ParsedToken(it, isWord = true) }
                } else {
                    tokens += ParsedToken(run, isWord = false, isEndOfSentence = endsSentence(paragraph, i, j, splitChars))
                }
                i = j
            }
            if (index != paragraphs.lastIndex) tokens += ParsedToken(PARAGRAPH_MARK, isWord = false, isEndOfSentence = true)
        }
        return tokens
    }

    /** The words of a stretch of letters, longest first from the left. */
    fun segment(run: String, lexicon: Lexicon? = Lexicons[code]): List<String> {
        val words = mutableListOf<String>()
        val unknown = StringBuilder()
        Segmenters[code]?.of(run)?.let { return analysed(run, it, lexicon) }
        if (byFrequency && lexicon != null && lexicon.ranked) return likeliest(run, lexicon)
        var i = 0
        while (i < run.length) {
            val end = lexicon?.let { longestMatch(run, i, it) }
            if (end == null) {
                if (joinUnknown) unknown.append(run[i]) else words += run[i].toString()
                i++
                continue
            }
            if (unknown.isNotEmpty()) {
                words += unknown.toString()
                unknown.clear()
            }
            words += run.substring(i, end)
            i = end
        }
        if (unknown.isNotEmpty()) words += unknown.toString()
        return words
    }

    private fun analysed(run: String, analysis: Analysis, lexicon: Lexicon?): List<String> {
        val words = (analysis.words + 0 + run.length).filter { it in 0..run.length }.sorted()
        val pieces = (analysis.pieces + words).filter { it in 0..run.length }.sorted()
        val out = mutableListOf<String>()
        for ((from, to) in words.zipWithNext()) {
            var i = from
            while (i < to) {
                val ends = pieces.filter { it in (i + 1)..to }
                // The longest run of pieces the lexicon knows, else the next piece by itself (a name, a word it lacks).
                val end = ends.lastOrNull { end -> lexicon != null && matches(lowercase(run.substring(i, end)), lexicon) } ?: ends.first()
                out += run.substring(i, end)
                i = end
            }
        }
        return out
    }

    /**
     * The cut of [run] whose words are, together, the most common: each word costs the logarithm of
     * its frequency rank plus [WORD_COST], so a common word is kept whole and a rarer word made of
     * common ones loses to them. A word the lexicon has without a rank (a dictionary's) counts as
     * rank [UNRANKED]; a character no word covers is a word of its own at rank [UNKNOWN].
     */
    private fun likeliest(run: String, lexicon: Lexicon): List<String> {
        val best = DoubleArray(run.length + 1) { if (it == 0) 0.0 else Double.MAX_VALUE }
        val from = IntArray(run.length + 1)
        for (end in 1..run.length) {
            for (start in maxOf(0, end - lexicon.maxLength - inflectionLength) until end) {
                if (best[start] == Double.MAX_VALUE) continue
                val word = lowercase(run.substring(start, end))
                // After a number or a demonstrative, a measure word goes with it: 两个人 is 两 + 个 + 人, not 两 + 个人.
                if (end - start > 1 && start > 0 && run[start - 1] in COUNTING && run[start] in MEASURES) continue
                val rank = when {
                    matches(word, lexicon) -> lexicon.rank(word) ?: UNRANKED
                    end - start == 1 -> UNKNOWN
                    else -> continue
                }
                val cost = best[start] + ln(rank.toDouble()) + WORD_COST
                if (cost < best[end]) {
                    best[end] = cost
                    from[end] = start
                }
            }
        }
        val words = ArrayDeque<String>()
        var end = run.length
        while (end > 0) {
            words.addFirst(run.substring(from[end], end))
            end = from[end]
        }
        return words.toList()
    }

    private companion object {
        // Tuned against jieba's cuts of Chinese Wikipedia articles: fewer gives too many short words.
        const val WORD_COST = 4.0
        const val UNRANKED = 50_000
        const val UNKNOWN = 100_000

        /** The numbers and demonstratives a Chinese measure word follows. */
        const val COUNTING = "一二两三四五六七八九十百千万几这那哪每"

        /** Common Chinese measure words, in both scripts. */
        const val MEASURES = "个個本只隻位次天名家件张張条條种種句双雙块塊杯碗岁歲点點辆輛台層层门門份节節场場篇首遍"
    }

    private fun longestMatch(run: String, start: Int, lexicon: Lexicon): Int? {
        val limit = minOf(run.length, start + lexicon.maxLength + inflectionLength)
        for (end in limit downTo start + 1) {
            if (matches(lowercase(run.substring(start, end)), lexicon)) return end
        }
        return null
    }
}
