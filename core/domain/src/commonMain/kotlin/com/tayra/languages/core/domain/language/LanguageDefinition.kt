package com.tayra.languages.core.domain.language

import com.tayra.languages.core.domain.model.Language

data class StoryDefinition(
    val title: String,
    val text: String,
    val sourceUrl: String? = null,
)

/** A predefined language with its sample stories. */
data class LanguageDefinition(
    val language: Language,
    val stories: List<StoryDefinition>,
) {
    val name: String get() = language.name
}
