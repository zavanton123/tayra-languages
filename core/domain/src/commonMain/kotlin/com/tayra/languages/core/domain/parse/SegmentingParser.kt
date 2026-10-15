package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.model.Language

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
 */
open class SegmentingParser(
    override val name: String,
    private val code: String,
    private val joinUnknown: Boolean,
) : TextParser {

    /** How much longer than the lexicon's longest word a stretch may be, for the inflections [matches] accepts. */
    protected open val inflectionLength: Int = 0

    /** Whether [text] is a word: by default, one the lexicon lists. */
    protected open fun matches(text: String, lexicon: Lexicon): Boolean = text in lexicon

    override fun parse(text: String, language: Language): List<ParsedToken> {
        val prepared = applyCharacterSubstitutions(text, language.characterSubstitutions)
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
                    tokens += ParsedToken(run, isWord = false, isEndOfSentence = run.any { it in splitChars })
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

    private fun longestMatch(run: String, start: Int, lexicon: Lexicon): Int? {
        val limit = minOf(run.length, start + lexicon.maxLength + inflectionLength)
        for (end in limit downTo start + 1) {
            if (matches(lowercase(run.substring(start, end)), lexicon)) return end
        }
        return null
    }
}
