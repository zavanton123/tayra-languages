package com.idiomiko.core.domain.service

import com.idiomiko.core.domain.book.SentenceBuilder
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.model.ZWS_STRING
import com.idiomiko.core.domain.parse.Lexicons
import com.idiomiko.core.domain.parse.lowercase
import com.idiomiko.core.domain.repository.BookRepository
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.repository.TermListFilter
import com.idiomiko.core.domain.repository.TermListSort
import com.idiomiko.core.domain.repository.TermRepository
import com.idiomiko.core.domain.term.TermTextNormalizer

/**
 * Keeps the terms of a language whose parser came to cut its words (Arabic: والكتاب is و + ال + كتاب; Hebrew: ובבית is
 * ו + ב + בית) the words the reader shows. A term saved as a word now cut becomes the word left after the letters in
 * front, which then has one status however it is written; where that word was saved too (وهو and هو), the two become
 * one, keeping the more advanced status and whatever either has that the other lacks. A phrase is cut again, and the
 * language's stored sentences are made again. Words are matched without their vowel signs now, so a term saved with
 * them (أيضًا) is matched as أيضا. Thai's parser came to cut no word inside a syllable (ต่อมา is ต่อ + มา, no longer ต่อม +
 * า): a phrase saved over the old cut is cut again, while a word saved stays the word it is, unless the lexicon has it and
 * the reader still reads it, alone, as several words (ไม่ดี is ไม่ + ดี, คิดว่า คิด + ว่า): it becomes those words, a
 * phrase. So does a Manx word written onto another with an apostrophe (t'eh is t' + eh, ta'n ta + 'n), and one saved with
 * an apostrophe at its edge ('sy) becomes the word without it. Lao came to be cut as Thai is, and read in today's spelling
 * (ຫນັງສື as ໜັງສື, ນໍ້າ as ນ້ຳ): its terms are matched so, and its phrases cut again. Khmer's parser came to cut no word inside a
 * syllable either, and to match subscript ដ as the ត it looks like (សប្ដាហ៍ as សប្តាហ៍), as Thai's words are kept.
 * Armenian cuts nothing, but
 * matches և written as two letters (կարեւոր) as the one (կարևոր). Ancient Greek cuts nothing either, but matches a word
 * with the accent it has standing alone (καλὸς as καλός, ἄνθρωπός as ἄνθρωπος). Maltese's came to cut a word cut short
 * before another (f'Malta is f' + Malta), so a word saved so is the word after it. Run at start: it changes nothing once the terms are as
 * the reader reads them.
 */
class CutWordsMigration(
    private val languages: LanguageRepository,
    private val terms: TermRepository,
    private val books: BookRepository,
    /** Loads the language's lexicon (LexiconService.prepare, the installed packs read first), by which its words are cut. */
    private val prepare: suspend (Language) -> Unit,
) {
    /** What the migration did to a language's terms. */
    data class Change(val cut: Int, val merged: Int)

    suspend fun migrate(): Map<Long, Change> {
        val cutting = languages.getAll().filter { it.parserType in PARSERS }
        if (cutting.isEmpty()) return emptyMap()
        val out = mutableMapOf<Long, Change>()
        for (language in cutting) {
            val saved = terms.list(TermListFilter(languageId = language.id, includeIgnored = true), TermListSort(), 0, Int.MAX_VALUE).items
            if (saved.isEmpty()) continue
            prepare(language)
            val change = migrate(language, saved.map { it.id })
            if (change.cut + change.merged == 0) continue
            out[language.id] = change
            rebuildSentences(language)
        }
        return out
    }

    private suspend fun migrate(language: Language, ids: List<Long>): Change {
        var cut = 0
        var merged = 0
        for (id in ids) {
            // Read again: a term merged into before is saved anew.
            val term = terms.getById(id) ?: continue
            val pieces = TermTextNormalizer.normalize(language, term.displayText)
            // A word is the word its letters in front are on; a phrase is its words as they are cut now.
            val text = when {
                term.tokenCount != 1 -> pieces
                language.parserType in EDGES_APART -> withoutEdges(pieces)
                language.parserType !in WHOLE_WORDS -> pieces.substringAfterLast(ZWS_STRING)
                inLexicon(language, language.lowercase(term.textLc)) -> pieces
                else -> term.text
            }
            val textLc = language.lowercase(text)
            val tokenCount = TermTextNormalizer.tokenCount(text)
            if (text.isEmpty() || text == term.text && textLc == term.textLc && tokenCount == term.tokenCount) continue
            val existing = if (textLc == term.textLc) null else terms.findByTextLc(language.id, textLc)
            if (existing == null || existing.id == term.id) {
                terms.save(term.copy(text = text, textLc = textLc, tokenCount = tokenCount))
                cut++
                continue
            }
            terms.save(
                existing.copy(
                    status = if (advance(term.status) > advance(existing.status)) term.status else existing.status,
                    translation = existing.translation?.takeIf { it.isNotBlank() } ?: term.translation,
                    romanization = existing.romanization?.takeIf { it.isNotBlank() } ?: term.romanization,
                    sentence = existing.sentence ?: term.sentence,
                ),
            )
            if (existing.parents.isEmpty() && term.parents.isNotEmpty()) terms.setParents(existing.id, term.parents.map { it.id })
            terms.delete(term.id)
            merged++
        }
        return Change(cut, merged)
    }

    /** Whether the language's lexicon, loaded by [prepare], has [word]. */
    private fun inLexicon(language: Language, word: String): Boolean =
        LanguageCodes.codeFor(language.name)?.let { Lexicons[it] }?.contains(word) == true

    private suspend fun rebuildSentences(language: Language) {
        for (book in books.getBooks().filter { it.languageId == language.id }) {
            for (page in books.getPages(book.id)) books.replaceSentences(page.id, SentenceBuilder.build(page.text, language))
            books.clearStats(book.id)
        }
    }

    /** How far along a status is; ignoring a word is a choice, so it wins only over not having met it. */
    private fun advance(status: TermStatus): Int = when (status) {
        TermStatus.IGNORED -> 1
        else -> TermStatus.progression.indexOf(status).coerceAtLeast(0) * 2
    }

    private companion object {
        /** The parsers that cut a word written as one into words, or match a saved word otherwise than it was saved. */
        val PARSERS = setOf("arabic", "thai", "persian", "hebrew", "armenian", "ancientgreek", "manx", "lao", "maltese", "khmer")

        /** Those whose saved words become the words they are cut into, without the apostrophes at their edges. */
        val EDGES_APART = setOf("manx")

        /** Those whose saved words stay whole, but those of the lexicon the reader reads as several words. */
        val WHOLE_WORDS = setOf("thai", "lao", "khmer")

        /** The tokens of a cut word but the apostrophes before and after it. */
        fun withoutEdges(pieces: String): String {
            fun isApostrophes(token: String) = token.all { it == '\'' }
            return pieces.split(ZWS_STRING).dropWhile(::isApostrophes).dropLastWhile(::isApostrophes).joinToString(ZWS_STRING)
        }
    }
}
