package com.tayra.languages.core.ui.i18n.es

/**
 * The Spanish interface: English text to its Spanish wording, one map per area of the app, as in
 * the Russian one. A plural text maps its English "other" form to its forms separated by `|`
 * (see `trPlural`).
 */
internal val spanishParts: List<Pair<String, Map<String, String>>> = listOf(
    "common" to esCommon,
    "books" to esBooks,
    "courses" to esCourses,
    "frequency" to esFrequency,
    "home" to esHome,
    "news" to esNews,
    "playlists" to esPlaylists,
    "reading" to esReading,
    "settings" to esSettings,
    "settings-more" to esSettingsMore,
    "terms" to esTerms,
)

internal val spanishStrings: Map<String, String> by lazy { spanishParts.fold(emptyMap()) { all, (_, part) -> all + part } }
