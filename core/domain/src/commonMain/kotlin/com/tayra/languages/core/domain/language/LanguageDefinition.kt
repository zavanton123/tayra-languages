package com.tayra.languages.core.domain.language

import com.tayra.languages.core.domain.model.Language

/** A predefined language: how its texts are parsed and looked up. */
data class LanguageDefinition(
    val language: Language,
) {
    val name: String get() = language.name
}

/** Every language definition: the generated ones, with those in tools/languages added or put in their place. */
object LanguageDefinitions {
    val all: List<LanguageDefinition> by lazy {
        val added = AddedLanguages.all.map { it.name }.toSet()
        PredefinedLanguages.all.filter { it.name !in added } + AddedLanguages.all
    }
}
