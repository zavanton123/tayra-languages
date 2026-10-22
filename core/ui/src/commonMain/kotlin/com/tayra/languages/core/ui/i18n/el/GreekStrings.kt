package com.tayra.languages.core.ui.i18n.el

/**
 * The Greek interface: English text to its Greek wording, one map per area of the app, as in
 * the Russian one. A plural text maps its English "other" form to its forms separated by `|`
 * (see `trPlural`).
 */
internal val greekParts: List<Pair<String, Map<String, String>>> = listOf(
    "common" to elCommon,
    "books" to elBooks,
    "courses" to elCourses,
    "frequency" to elFrequency,
    "home" to elHome,
    "news" to elNews,
    "playlists" to elPlaylists,
    "reading" to elReading,
    "settings" to elSettings,
    "settings-more" to elSettingsMore,
    "terms" to elTerms,
)

internal val greekStrings: Map<String, String> by lazy { greekParts.fold(emptyMap()) { all, (_, part) -> all + part } }
