package com.tayra.languages.core.domain.text

/** Word boundaries in plain text, for pointing at a single word in a sentence. */
object Words {
    /**
     * The word that covers [offset] in [text], or the one that ends right before it, as a
     * half-open range. Letters, digits, combining marks and inner apostrophes and hyphens make
     * up a word; it needs at least one letter. Null when the offset is on anything else.
     */
    fun rangeAt(text: String, offset: Int): IntRange? {
        if (text.isEmpty()) return null
        var i = offset.coerceIn(0, text.length)
        if (i == text.length || !isWordChar(text[i])) {
            if (i > 0 && isWordChar(text[i - 1])) i-- else return null
        }
        var start = i
        while (start > 0 && isWordChar(text[start - 1])) start--
        var end = i + 1
        while (end < text.length && isWordChar(text[end])) end++
        while (start < end && isJoiner(text[start])) start++
        while (end > start && isJoiner(text[end - 1])) end--
        if (start >= end || text.substring(start, end).none { it.isLetter() }) return null
        return start until end
    }

    private fun isWordChar(c: Char): Boolean =
        c.isLetterOrDigit() || isJoiner(c) || c.category == CharCategory.NON_SPACING_MARK || c.category == CharCategory.COMBINING_SPACING_MARK

    private fun isJoiner(c: Char): Boolean = c == '\'' || c == '\u2019' || c == '-'
}
