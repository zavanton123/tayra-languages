package com.tayra.languages.core.domain.dictionary

/**
 * Which of a word's dictionary meanings its sentence uses, read from translations: the meaning whose
 * words the sentence's translation has, the meaning's headword counting above a word of its
 * explanation; else the meaning the word alone translates as; else the dictionary's first. Measured
 * on lessons' new words, it picks the right meaning about seven times in ten, the first meaning five.
 */
object MeaningChooser {
    /** The meaning chosen, by entry and sense of the lookup; [fits] when the sentence's translation chose it. */
    data class Choice(val entry: Int, val sense: Int, val gloss: String, val fits: Boolean)

    /**
     * The meaning of [word] in [sentence] among [lookup]'s, from [sentenceTranslation] and
     * [wordTranslation] (either may be missing); null when the lookup has no meaning. [languageCode]
     * is the word's language, for the hint its neighbours give of what part of speech it is.
     */
    fun choose(lookup: DictionaryLookup, word: String, sentence: String?, languageCode: String?, sentenceTranslation: String?, wordTranslation: String?): Choice? {
        val senses = lookup.entries.flatMapIndexed { e, entry -> entry.senses.mapIndexedNotNull { s, sense -> sense.glosses.firstOrNull()?.let { Candidate(e, s, entry.pos, sense) } } }
        if (senses.isEmpty()) return null
        val pos = sentence?.let { partOfSpeech(languageCode, it, word) }
        val inSentence = { allowed: Set<String>? -> sentenceTranslation?.let { best(senses, it, allowed) } }
        val alone = { allowed: Set<String>? -> wordTranslation?.let { best(senses, it, allowed) } }
        pos?.let { allowed -> inSentence(allowed)?.let { return it.choice(fits = true) } }
        pos?.let { allowed -> alone(allowed)?.let { return it.choice(fits = false) } }
        inSentence(null)?.let { return it.choice(fits = true) }
        alone(null)?.let { return it.choice(fits = false) }
        return (pos?.let { allowed -> senses.firstOrNull { it.pos in allowed } } ?: senses.first()).choice(fits = false)
    }

    private class Candidate(val entry: Int, val sense: Int, val pos: String, val meaning: DictionarySense) {
        fun choice(fits: Boolean) = Choice(entry, sense, meaning.glosses.first(), fits)
    }

    /** The meaning sharing the most telling word with [text]; the earlier one on ties; null when none shares any. */
    private fun best(senses: List<Candidate>, text: String, allowed: Set<String>?): Candidate? {
        val have = words(text, gloss = false)
        val plain = text.lowercase()
        var found: Candidate? = null
        var top = 0
        for (candidate in senses) {
            if (allowed != null && candidate.pos !in allowed) continue
            val score = score(candidate.meaning, have, plain)
            if (score > top) {
                found = candidate
                top = score
            }
        }
        return found
    }

    /** 3 for a meaning whose short term is in the text ("попытка" of "попытка, проба"), 2 for a term's first word, 1 for another of its words, 0 for none. */
    private fun score(sense: DictionarySense, have: List<String>, plain: String): Int {
        var best = 0
        for (gloss in sense.glosses) {
            for (part in gloss.replace(BRACKETS, " ").split(',', ';')) {
                // Chinese and Japanese glosses have no spaces between words: the term is found whole.
                if (part.any { it.isCjk() }) {
                    val term = part.trim()
                    if (term.isNotEmpty() && term in plain) best = maxOf(best, 3)
                    continue
                }
                val term = words(part, gloss = true)
                term.forEachIndexed { j, w ->
                    if (have.any { same(w, it) }) best = maxOf(best, if (j == 0 && term.size <= 2) 3 else if (j == 0) 2 else 1)
                }
            }
        }
        return best
    }

    /**
     * Words of one word: the same, or sharing at least their first four letters and all but at most
     * four of the shorter one's, which takes in endings and aspect pairs ("направить", "направлять")
     * in any language without a stemmer for it; measured, as good as a Russian stemmer.
     */
    private fun same(a: String, b: String): Boolean {
        if (a == b) return true
        var n = 0
        while (n < a.length && n < b.length && a[n] == b[n]) n++
        return n >= 4 && n >= minOf(a.length, b.length) - 4
    }

    /** The words of [text] worth matching: no brackets, no dictionaries' labels ("амер.", "разг."), no little words. */
    private fun words(text: String, gloss: Boolean): List<String> {
        var t = text.lowercase().replace(BRACKETS, " ")
        if (gloss) t = t.replace(LABEL, " ")
        return WORD.findAll(t).map { it.value }.filter { it.length > 1 && it !in STOP }.toList()
    }

    private fun Char.isCjk(): Boolean = this in '぀'..'ヿ' || this in '一'..'鿿' || this in '㐀'..'䶿'

    /**
     * What the word's neighbours say it is, where the language has rules for it: in English a word
     * after an article or possessive is a noun or adjective, one after "to", a modal or a pronoun a
     * verb. Measured, it takes the right meanings from about 69 to 74 in 100 English words.
     */
    private fun partOfSpeech(languageCode: String?, sentence: String, word: String): Set<String>? {
        if (languageCode != "en") return null
        val tokens = Regex("""[\p{L}']+""").findAll(sentence.lowercase()).map { it.value }.toList()
        val at = tokens.indexOf(word.lowercase())
        if (at <= 0) return null
        return when (tokens[at - 1]) {
            in EN_DETERMINERS -> setOf("noun", "adj")
            in EN_BEFORE_VERBS -> setOf("verb")
            else -> null
        }
    }

    private val BRACKETS = Regex("""\([^)]*\)""")
    private val LABEL = Regex("""(?:^|(?<=[\s;,(]))\p{L}{1,6}(?:-\p{L}{1,4})*\.(?=\s|$|[,;])""")
    private val WORD = Regex("""\p{L}+(?:-\p{L}+)*""")

    /** Words that say nothing of a meaning: the commonest little words of the glosses' languages, and dictionaries' "someone", "something". */
    private val STOP = setOf(
        "что", "как", "это", "кто-либо", "кого-либо", "кому-либо", "кем-либо", "ком-либо", "что-либо", "чего-либо", "чему-либо", "чем-либо",
        "чём-либо", "какой-либо", "для", "без", "над", "под", "про", "через", "который", "такой", "также", "быть", "себя", "свой", "тот",
        "something", "someone", "somebody", "with", "from", "into", "that", "this", "have", "make", "the", "and",
    )

    private val EN_DETERMINERS = setOf("the", "a", "an", "my", "our", "your", "his", "her", "its", "their", "this", "that", "these", "those", "every", "each", "some", "any", "no", "one", "first", "last")
    private val EN_BEFORE_VERBS = setOf("to", "can", "could", "will", "would", "should", "must", "may", "might", "shall", "i", "we", "you", "they", "he", "she", "it", "don't", "didn't", "doesn't", "won't", "can't")
}
