package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.model.Language

/**
 * Armenian: ՞ ՛ ՜ stand over a word's stressed vowel, inside the word (ինչո՞ւ, ո՞վ, գնա՛նք), and ՟ marks an abbreviation.
 * They belong to the word as it is shown and are left out of the word matched and saved ([lowercase]), so ինչո՞ւ is
 * ինչու, whatever status that has. և written as two letters (նաեւ) is matched as the one letter (նաև).
 *
 * The language's word characters hold the marks, and only ։ ends a sentence. A mark standing alone is no word.
 *
 * tools/build_frequency_list.py counts words the same way (armenian_unmarked); keep the two the same.
 */
class ArmenianParser : SpaceDelimitedParser() {

    override val name: String get() = "Armenian"

    override fun parse(text: String, language: Language): List<ParsedToken> =
        super.parse(text, language).map { token -> if (token.isWord && token.token.all(::isMark)) token.copy(isWord = false) else token }

    override fun lowercase(text: String): String = text.filterNot(::isMark).lowercase().replace("եւ", "և")

    private companion object {
        fun isMark(c: Char): Boolean = c == '՛' || c == '՜' || c == '՞' || c == '՟'
    }
}
