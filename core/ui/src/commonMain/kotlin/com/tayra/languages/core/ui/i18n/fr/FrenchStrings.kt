package com.tayra.languages.core.ui.i18n.fr

/**
 * The French interface: English text to its French wording, one map per area of the app, as in
 * the Russian one. A plural text maps its English "other" form to its forms separated by `|`
 * (see `trPlural`).
 */
internal val frenchParts: List<Pair<String, Map<String, String>>> = listOf(
    "common" to frCommon,
    "books" to frBooks,
    "courses" to frCourses,
    "frequency" to frFrequency,
    "home" to frHome,
    "news" to frNews,
    "reading" to frReading,
    "settings" to frSettings,
    "settings-more" to frSettingsMore,
    "terms" to frTerms,
)

internal val frenchStrings: Map<String, String> by lazy { frenchParts.fold(emptyMap()) { all, (_, part) -> all + part } }
