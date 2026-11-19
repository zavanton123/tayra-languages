package com.idiomiko.core.domain.parse

/**
 * Maltese: a preposition or particle cut short before a word is written onto it after an apostrophe (f'Malta, b'mod,
 * m'hemmx, x'inhu, s'issa, t'Għawdex), and is read as a word of its own, as the word it stands for (fi, bi, ma, xi, sa,
 * ta'). An apostrophe ending a word stays with it (ta', ma', seta'). The article and a preposition written onto it are
 * cut at their hyphen, as in any language whose word characters lack it (il-kelb is il + kelb, tad-dar tad + dar).
 *
 * tools/build_frequency_list.py counts words the same way (maltese_pieces), and tools/check_course.py reads them so; keep
 * the three the same.
 */
class MalteseParser : SpaceDelimitedParser() {

    override val name: String get() = "Maltese"

    override fun words(word: String): List<String> {
        val pieces = mutableListOf<String>()
        var rest = word
        while (rest.length > 2 && rest[1] == '\'' && rest[0].lowercaseChar() in SHORT && rest[2].isLetter()) {
            pieces += rest.substring(0, 2)
            rest = rest.substring(2)
        }
        pieces += rest
        return pieces
    }

    private companion object {
        /** The letters a word cut short before another is: f', b', m', x', s', t'. */
        const val SHORT = "fbmxst"
    }
}
