package com.tayra.languages.core.ui.i18n.pt

/**
 * The Portuguese (Brazil) interface: English text to its Portuguese wording, one map per area of the app, as in
 * the Russian one. A plural text maps its English "other" form to its forms separated by `|`
 * (see `trPlural`).
 */
internal val portugueseParts: List<Pair<String, Map<String, String>>> = listOf(
    "common" to ptCommon,
    "books" to ptBooks,
    "courses" to ptCourses,
    "frequency" to ptFrequency,
    "home" to ptHome,
    "news" to ptNews,
    "reading" to ptReading,
    "settings" to ptSettings,
    "settings-more" to ptSettingsMore,
    "terms" to ptTerms,
)

internal val portugueseStrings: Map<String, String> by lazy { portugueseParts.fold(emptyMap()) { all, (_, part) -> all + part } }
