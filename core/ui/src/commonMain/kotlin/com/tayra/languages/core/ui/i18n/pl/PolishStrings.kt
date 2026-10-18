package com.tayra.languages.core.ui.i18n.pl

/**
 * The Polish interface: English text to its Polish wording, one map per area of the app, as in
 * the Russian one. A plural text maps its English "other" form to its forms separated by `|`
 * (see `trPlural`).
 */
internal val polishParts: List<Pair<String, Map<String, String>>> = listOf(
    "common" to plCommon,
    "books" to plBooks,
    "courses" to plCourses,
    "frequency" to plFrequency,
    "home" to plHome,
    "news" to plNews,
    "reading" to plReading,
    "settings" to plSettings,
    "settings-more" to plSettingsMore,
    "terms" to plTerms,
)

internal val polishStrings: Map<String, String> by lazy { polishParts.fold(emptyMap()) { all, (_, part) -> all + part } }
