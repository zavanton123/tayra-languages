package com.tayra.languages.core.domain.language

import com.tayra.languages.core.domain.model.Language

/** A predefined language: how its texts are parsed and looked up. */
data class LanguageDefinition(
    val language: Language,
) {
    val name: String get() = language.name
}
