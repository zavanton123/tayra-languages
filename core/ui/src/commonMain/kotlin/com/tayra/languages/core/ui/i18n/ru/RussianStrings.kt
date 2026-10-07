package com.tayra.languages.core.ui.i18n.ru

/**
 * The Russian interface: English text to its Russian wording, one map per area of the app so
 * the areas can be translated side by side. A plural text maps its English "other" form to its
 * three Russian forms separated by `|` (see `trPlural`).
 */
internal val russianParts: List<Pair<String, Map<String, String>>> = listOf(
    "common" to ruCommon,
    "books" to ruBooks,
    "courses" to ruCourses,
    "frequency" to ruFrequency,
    "reading" to ruReading,
    "settings" to ruSettings,
    "settings-more" to ruSettingsMore,
    "terms" to ruTerms,
)

internal val russianStrings: Map<String, String> by lazy { russianParts.fold(emptyMap()) { all, (_, part) -> all + part } }
