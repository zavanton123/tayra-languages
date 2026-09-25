package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.Language

/** An example sentence using a term, with a translation when available. */
data class ExampleSentence(val text: String, val translation: String?)

/** Finds example sentences for a term, e.g. from a sentence corpus. */
interface ExampleSentencesProvider {
    /** Examples in the term's language, translated into the target language when possible. Never throws. */
    suspend fun examples(text: String, language: Language, targetLanguage: String, limit: Int = 5): List<ExampleSentence>
}
