package com.tayra.languages.core.ui.i18n.de

/**
 * The German interface: English text to its German wording, one map per area of the app, as in
 * the Russian one. A plural text maps its English "other" form to its forms separated by `|`
 * (see `trPlural`).
 */
internal val germanParts: List<Pair<String, Map<String, String>>> = listOf(
    "common" to deCommon,
    "books" to deBooks,
    "courses" to deCourses,
    "frequency" to deFrequency,
    "home" to deHome,
    "news" to deNews,
    "reading" to deReading,
    "settings" to deSettings,
    "settings-more" to deSettingsMore,
    "terms" to deTerms,
)

internal val germanStrings: Map<String, String> by lazy { germanParts.fold(emptyMap()) { all, (_, part) -> all + part } }
