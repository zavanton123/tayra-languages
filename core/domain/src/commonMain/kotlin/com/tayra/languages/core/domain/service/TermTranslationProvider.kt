package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.Language

/** Looks up a suggested translation for a term, e.g. from an online dictionary. */
interface TermTranslationProvider {
    /** A short translation or gloss, or null when nothing is known. Never throws. */
    suspend fun suggestTranslation(text: String, language: Language): String?
}
