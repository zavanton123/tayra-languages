package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermDraft
import com.tayra.languages.core.domain.model.TermMatch
import com.tayra.languages.core.domain.model.TermReferences
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.parse.lowercase
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.term.TermTextNormalizer

class TermValidationException(message: String, val duplicateOf: Term? = null) : Exception(message)

/** Changes applied to several terms at once. */
data class BulkTermUpdate(
    val termIds: List<Long>,
    val lowercaseTerms: Boolean = false,
    val removeParents: Boolean = false,
    val parentId: Long? = null,
    val parentText: String? = null,
    val status: TermStatus? = null,
)

class TermService(
    private val terms: TermRepository,
    private val languages: LanguageRepository,
) {

    suspend fun language(languageId: Long): Language =
        languages.getById(languageId) ?: throw NoSuchElementException("No language $languageId")

    suspend fun find(languageId: Long, text: String): Term? {
        val language = language(languageId)
        val spec = TermTextNormalizer.termFromText(language, text)
        return terms.findByTextLc(languageId, spec.textLc)
    }

    /** A draft for the existing term with the text, or a new draft if none exists. */
    suspend fun findOrNew(languageId: Long, text: String): TermDraft {
        find(languageId, text)?.let { return draftOf(it) }
        val language = language(languageId)
        val spec = TermTextNormalizer.termFromText(language, text)
        return TermDraft(
            languageId = languageId,
            text = spec.text,
            originalText = spec.text,
            romanization = spec.romanization ?: "",
        )
    }

    suspend fun load(termId: Long): TermDraft {
        val term = terms.getById(termId) ?: throw NoSuchElementException("No term $termId")
        return draftOf(term)
    }

    suspend fun draftOf(term: Term): TermDraft = TermDraft(
        id = term.id,
        languageId = term.languageId,
        text = term.text,
        originalText = term.text,
        translation = term.translation ?: "",
        romanization = term.romanization ?: "",
        status = term.status,
        syncStatus = term.syncStatus,
        parents = term.parents.map { it.text },
    )

    /**
     * Saves the draft, creating or updating parents, and returns the term id.
     *
     * @throws TermValidationException if the text is blank, would duplicate another
     *   term, or changes an existing term's text beyond its case.
     */
    /**
     * Saves [draft]. [sentence] is the sentence the term is being read in, if any: it is stored
     * with the term while the term is being learned (see [sentenceAt]).
     */
    suspend fun save(draft: TermDraft, sentence: String? = null): Long {
        if (draft.text.isBlank()) throw TermValidationException("Term text is required")
        val language = language(draft.languageId)
        val spec = TermTextNormalizer.termFromText(language, draft.text)

        val existing: Term? = if (draft.id != null) {
            terms.getById(draft.id) ?: throw NoSuchElementException("No term ${draft.id}")
        } else {
            terms.findByTextLc(language.id, spec.textLc)?.also { found ->
                if (draft.originalText.isBlank() || language.lowercase(draft.originalText) != found.textLc) {
                    throw TermValidationException("Term \"${found.displayText}\" already exists", duplicateOf = found)
                }
            }
        }
        if (existing != null && existing.textLc != spec.textLc) {
            throw TermValidationException("Can only change term case")
        }

        var term = (existing ?: spec).copy(
            text = spec.text,
            textLc = spec.textLc,
            tokenCount = spec.tokenCount,
            status = draft.status,
            translation = draft.translation.trim().ifEmpty { null },
            romanization = draft.romanization.trim().ifEmpty { null },
            flashMessage = null,
        )

        val parentTexts = draft.parents
            .map { it.trim() }
            .filter { it.isNotEmpty() && language.lowercase(it) != spec.textLc }
            .distinctBy { language.lowercase(it) }
        val parents = parentTexts.map { findOrCreateParent(it, language, term.copy(sentence = existing.sentenceAt(term.status, sentence)), isNewTerm = existing == null) }

        var syncStatus = draft.syncStatus && parents.size == 1
        if (syncStatus) {
            val parent = parents.single()
            if (draft.statusExplicitlySet || parent.status == TermStatus.UNKNOWN) {
                if (parent.status != term.status) applyStatus(listOf(parent), term.status) { sentence }
            } else {
                term = term.copy(status = parent.status)
            }
        }
        term = term.copy(syncStatus = syncStatus, sentence = existing.sentenceAt(term.status, sentence))

        val id = terms.save(term)
        terms.setParents(id, parents.map { it.id })
        propagateStatusToFollowingChildren(id, term.status, term.sentence)
        return id
    }

    /**
     * The sentence stored with this term once its status is [status]: the one it is being read
     * in ([provided]) when it starts being learned, the one it already has while it goes on
     * being learned, and none once it is known, ignored or unknown again.
     */
    private fun Term?.sentenceAt(status: TermStatus, provided: String?): String? = when {
        !status.isLearning -> null
        this?.status?.isLearning == true -> sentence ?: provided
        else -> provided
    }

    /** Sets the status of [targets], storing or clearing their sentences as [sentenceAt] says. */
    private suspend fun applyStatus(targets: List<Term>, status: TermStatus, provided: (Term) -> String?) {
        if (targets.isEmpty()) return
        terms.updateStatus(targets.map { it.id }, status)
        for (term in targets) {
            val sentence = term.sentenceAt(status, provided(term))
            if (sentence != term.sentence) terms.updateSentence(term.id, sentence)
        }
    }

    private suspend fun findOrCreateParent(parentText: String, language: Language, term: Term, isNewTerm: Boolean): Term {
        val spec = TermTextNormalizer.termFromText(language, parentText)
        val found = terms.findByTextLc(language.id, spec.textLc)
        val newOrUnknown = found == null || found.status == TermStatus.UNKNOWN
        var parent = found ?: spec.copy(status = term.status)

        if (newOrUnknown) parent = parent.copy(status = term.status, sentence = found.sentenceAt(term.status, term.sentence))
        if ((newOrUnknown || isNewTerm) && parent.translation.isNullOrBlank()) parent = parent.copy(translation = term.translation)

        if (found == null || parent != found) {
            val id = terms.save(parent)
            parent = parent.copy(id = id)
        }
        return parent
    }

    suspend fun delete(termId: Long) {
        terms.delete(termId)
    }

    suspend fun deleteAll(termIds: Collection<Long>) {
        terms.deleteAll(termIds)
    }

    /**
     * Sets the status of the terms, keeping linked parents and children in sync: a term
     * following its single parent shares the parent's status. [sentences] are the sentences the
     * terms are being read in, by id, stored with the terms that start being learned.
     */
    suspend fun setStatus(termIds: Collection<Long>, status: TermStatus, sentences: Map<Long, String> = emptyMap()) {
        if (termIds.isEmpty()) return
        val targets = terms.getByIds(termIds)
        applyStatus(targets, status) { sentences[it.id] }
        for (term in targets) {
            val sentence = sentences[term.id]
            if (term.syncStatus && term.parents.size == 1) {
                val parentId = term.parents.single().id
                terms.getById(parentId)?.let { parent -> applyStatus(listOf(parent), status) { sentence } }
                propagateStatusToFollowingChildren(parentId, status, sentence)
            }
            propagateStatusToFollowingChildren(term.id, status, sentence)
        }
    }

    suspend fun shiftStatus(termIds: Collection<Long>, delta: Int, sentences: Map<Long, String> = emptyMap()) {
        val byStatus = terms.getByIds(termIds).groupBy { it.status }
        for ((status, group) in byStatus) {
            val next = TermStatus.shifted(status, delta)
            if (next != status) setStatus(group.map { it.id }, next, sentences)
        }
    }

    private suspend fun propagateStatusToFollowingChildren(parentId: Long, status: TermStatus, sentence: String?) {
        val following = terms.children(parentId).filter { it.syncStatus && it.parents.size == 1 && it.status != status }
        applyStatus(following, status) { sentence }
    }

    suspend fun applyBulkUpdate(update: BulkTermUpdate) {
        if (update.termIds.isEmpty()) return
        val targets = terms.getByIds(update.termIds)
        val languageIds = targets.map { it.languageId }.distinct()
        if (languageIds.size > 1) throw TermValidationException("Terms are not all in the same language")
        val languageId = languageIds.firstOrNull() ?: return
        var parent: Term? = update.parentId?.let { terms.getById(it) }
        if (parent == null && !update.parentText.isNullOrBlank()) {
            val draft = findOrNew(languageId, update.parentText)
            parent = terms.getById(save(draft))
        }

        for (target in targets) {
            var term = terms.getById(target.id) ?: continue
            if (update.lowercaseTerms) term = term.copy(text = term.textLc)
            var parentIds = term.parents.map { it.id }
            if (update.removeParents) {
                parentIds = emptyList()
                term = term.copy(syncStatus = false)
            }
            if (parent != null && parent.id != term.id) {
                parentIds = listOf(parent.id)
                if (parent.status != TermStatus.UNKNOWN) term = term.copy(syncStatus = true, status = parent.status)
            }
            update.status?.let { term = term.copy(status = it) }
            if (term.status != target.status) term = term.copy(sentence = target.sentenceAt(term.status, null))
            terms.save(term)
            terms.setParents(term.id, parentIds)
            if (term.status != target.status) setStatus(listOf(term.id), term.status)
        }
    }

    suspend fun search(languageId: Long, text: String, limit: Int = 50): List<TermMatch> {
        if (text.isBlank()) return emptyList()
        val language = language(languageId)
        val spec = TermTextNormalizer.termFromText(language, text)
        if (spec.textLc.isBlank()) return emptyList()
        return terms.search(languageId, spec.textLc, limit)
    }

    /** Usages of the term, its children and its parents in read texts. */
    suspend fun references(languageId: Long, text: String): TermReferences {
        val language = language(languageId)
        val spec = TermTextNormalizer.termFromText(language, text)
        val term = terms.findByTextLc(languageId, spec.textLc)
        val termRefs = terms.references(languageId, spec.textLc)
        if (term == null) return TermReferences(termRefs, emptyList(), emptyList())

        val children = terms.children(term.id)
        val childRefs = children.flatMap { terms.references(languageId, it.textLc) }
        val parentRefs = terms.parents(term.id).map { parent ->
            val family = (listOf(parent) + terms.children(parent.id)).filter { it.id != term.id }
            parent.text.replace(ZWS_STRING, "") to family.flatMap { terms.references(languageId, it.textLc) }
        }
        return TermReferences(termRefs, childRefs, parentRefs)
    }
}
