package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.Language

/** A suggested translation and the service it came from, for the hint under the field. */
data class TermSuggestion(val text: String, val source: String)

/** Looks up a suggested translation for a term, e.g. from an online dictionary. */
interface TermTranslationProvider {
    /** How the provider is named to the user. */
    val name: String

    /** A short translation or gloss, or null when nothing is known. Never throws. */
    suspend fun suggestTranslation(text: String, language: Language): String?

    suspend fun suggest(text: String, language: Language): TermSuggestion? =
        suggestTranslation(text, language)?.let { TermSuggestion(it, name) }
}
