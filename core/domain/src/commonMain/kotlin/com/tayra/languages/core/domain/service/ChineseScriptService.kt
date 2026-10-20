package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.domain.morphology.ChineseScriptTables
import com.tayra.languages.core.domain.morphology.ChineseScripts
import com.tayra.languages.core.domain.book.SentenceBuilder
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermListFilter
import com.tayra.languages.core.domain.repository.TermListSort
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.model.ZWS
import com.tayra.languages.core.domain.parse.lowercase
import kotlinx.coroutines.flow.first

/**
 * Keeps a Chinese language in the script its learner chose: OpenCC's tables loaded while one is in
 * use, so texts and terms are converted wherever they are parsed, the vocabulary and frequency
 * screens included, and the language's saved terms and stored sentences converted when its script
 * changes. Words are matched by their simplified form in either script ([lowercase]), so changing
 * between the two only rewrites how the words are written; their statuses and counts stay theirs.
 */
class ChineseScriptService(
    private val languages: LanguageRepository,
    private val tables: ChineseScriptTables?,
    private val terms: TermRepository? = null,
    private val books: BookRepository? = null,
) {
    /** What a change of script did to the saved terms. */
    data class Change(val converted: Int, val merged: Int)

    /**
     * Loads the tables when some language uses a script; call once at start. Words saved before they
     * were matched by their simplified form, in traditional characters, are given that form.
     */
    suspend fun prepareIfUsed() {
        val scripted = languages.getAll().filter { it.chineseScript != null }
        if (tables == null || scripted.isEmpty()) return
        ChineseScripts.prepare(tables)
        val repository = terms ?: return
        for (language in scripted) {
            val keys = repository.observeWordStatuses(language.id).first().keys + repository.multiwordTerms(language.id).map { it.textLc }
            if (keys.none { language.lowercase(it) != it }) continue
            val change = convertTerms(language)
            if (change.converted + change.merged > 0) rebuildSentences(language)
        }
    }

    /**
     * Reads the language with [languageId] in [script] from now on, or as written when null. Its
     * terms are rewritten in the script; a term saved in both scripts, while the language was read
     * as written, becomes one, keeping the more advanced status and whatever either has that the
     * other lacks. The sentences of its texts are made again, so they are shown in the script.
     */
    suspend fun setScript(languageId: Long, script: ChineseScript?): Change {
        val language = languages.getById(languageId) ?: throw NoSuchElementException("No language $languageId")
        if (script != null) tables?.let { ChineseScripts.prepare(it) }
        val updated = language.copy(chineseScript = script)
        languages.save(updated)
        if (script == null || !ChineseScripts.isReady) return Change(0, 0)
        val change = convertTerms(updated)
        rebuildSentences(updated)
        return change
    }

    private suspend fun convertTerms(language: Language): Change {
        val repository = terms ?: return Change(0, 0)
        val all = repository.list(TermListFilter(languageId = language.id, includeIgnored = true), TermListSort(), 0, Int.MAX_VALUE).items
        var converted = 0
        var merged = 0
        val script = language.chineseScript ?: return Change(0, 0)
        for (term in all) {
            // In the words it was saved as: parsing it again out of its text could cut it differently.
            val text = ChineseScripts.convertWords(term.text, ZWS, script)
            val textLc = language.lowercase(text)
            val sentence = term.sentence?.let { ChineseScripts.convert(it, script) }
            if (text == term.text && textLc == term.textLc && sentence == term.sentence) continue
            val existing = if (textLc == term.textLc) null else repository.findByTextLc(language.id, textLc)
            if (existing == null || existing.id == term.id) {
                repository.save(term.copy(text = text, textLc = textLc, sentence = sentence))
                converted++
                continue
            }
            repository.save(
                existing.copy(
                    status = if (advance(term.status) > advance(existing.status)) term.status else existing.status,
                    translation = existing.translation?.takeIf { it.isNotBlank() } ?: term.translation,
                    romanization = existing.romanization?.takeIf { it.isNotBlank() } ?: term.romanization,
                    sentence = existing.sentence ?: term.sentence,
                ),
            )
            if (existing.parents.isEmpty() && term.parents.isNotEmpty()) repository.setParents(existing.id, term.parents.map { it.id })
            repository.delete(term.id)
            merged++
        }
        return Change(converted, merged)
    }

    private suspend fun rebuildSentences(language: Language) {
        val repository = books ?: return
        for (book in repository.getBooks().filter { it.languageId == language.id }) {
            for (page in repository.getPages(book.id)) repository.replaceSentences(page.id, SentenceBuilder.build(page.text, language))
            repository.clearStats(book.id)
        }
    }

    /** How far along a status is; ignoring a word is a choice, so it wins only over not having met it. */
    private fun advance(status: TermStatus): Int = when (status) {
        TermStatus.IGNORED -> 1
        else -> TermStatus.progression.indexOf(status).coerceAtLeast(0) * 2
    }
}
