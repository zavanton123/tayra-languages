package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.render.MultiwordTermIndex
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.settings.SettingsRepository

/** Data shown when hovering over / tapping a term. */
data class TermPopup(
    val termText: String,
    val parentsText: String,
    val translation: String,
    val romanization: String,
    val flashMessage: String,
    val parents: List<TermPopup> = emptyList(),
    val components: List<TermPopup> = emptyList(),
) {
    val termAndParentsText: String get() = if (parentsText.isEmpty()) termText else "$termText ($parentsText)"

    /** True if there is anything worth showing for this entry. */
    val hasContent: Boolean get() = romanization.isNotEmpty() || translation.isNotEmpty()
}

class TermPopupBuilder(
    private val terms: TermRepository,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val readingService: ReadingService,
) {

    /** Popup data, or null if there is nothing to show. */
    suspend fun build(termId: Long): TermPopup? {
        val term = terms.getById(termId) ?: return null
        val parents = terms.parents(termId)
        val prefs = settings.current

        val components = if (prefs.showComponents) findComponents(term) else emptyList()

        val base = popupOf(term, parents)
        if (!base.hasContent && parents.isEmpty() && components.isEmpty()) return null

        var main = base
        var parentPopups = parents.map { popupOf(it, emptyList()) }
        if (prefs.promoteParentTranslation && parents.size == 1) {
            val parentTranslation = parentPopups[0].translation
            if (main.translation.isEmpty()) main = main.copy(translation = parentTranslation)
            if (main.translation == parentTranslation) parentPopups = listOf(parentPopups[0].copy(translation = ""))
        }

        val componentPopups = sortComponents(term, components).map { popupOf(it, emptyList()) }
        return main.copy(
            parents = parentPopups.filter { it.hasContent },
            components = componentPopups.filter { it.hasContent },
        )
    }

    private suspend fun findComponents(term: Term): List<Term> {
        if (!term.isMultiword) return emptyList()
        val language = languages.getById(term.languageId) ?: return emptyList()
        return readingService.findTermsInText(term.text, language)
            .filter { it.id != term.id && it.status != TermStatus.UNKNOWN }
    }

    /** Components sorted by position in the term, longest first on ties. */
    private fun sortComponents(term: Term, components: List<Term>): List<Term> {
        val termTokens = term.textLc.split(ZWS_STRING)
        val index = MultiwordTermIndex(components.map { it.textLc })
        val positions = HashMap<String, Int>()
        for ((textLc, position) in index.findAll(termTokens)) {
            positions[textLc] = minOf(positions[textLc] ?: Int.MAX_VALUE, position)
        }
        for (tokenIndex in termTokens.indices) {
            if (termTokens[tokenIndex] !in positions) positions[termTokens[tokenIndex]] = tokenIndex
        }
        return components
            .filter { positions.containsKey(it.textLc) }
            .sortedWith(compareBy({ positions.getValue(it.textLc) }, { -it.text.length }))
    }

    private fun popupOf(term: Term, parents: List<Term>): TermPopup = TermPopup(
        termText = clean(term.text),
        parentsText = parents.joinToString(", ") { clean(it.text) },
        translation = clean(term.translation),
        romanization = clean(term.romanization),
        flashMessage = clean(term.flashMessage),
    )

    private fun clean(text: String?): String = (text ?: "").trim().replace(ZWS_STRING, "")
}
