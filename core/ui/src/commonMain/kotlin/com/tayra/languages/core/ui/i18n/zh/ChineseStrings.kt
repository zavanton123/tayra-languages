package com.tayra.languages.core.ui.i18n.zh

/**
 * The Chinese (Simplified) interface: English text to its Chinese wording, one map per area of the app, as in
 * the Russian one. A plural text maps its English "other" form to its forms separated by `|`
 * (see `trPlural`).
 */
internal val chineseParts: List<Pair<String, Map<String, String>>> = listOf(
    "common" to zhCommon,
    "books" to zhBooks,
    "courses" to zhCourses,
    "frequency" to zhFrequency,
    "home" to zhHome,
    "news" to zhNews,
    "playlists" to zhPlaylists,
    "reading" to zhReading,
    "settings" to zhSettings,
    "settings-more" to zhSettingsMore,
    "terms" to zhTerms,
)

internal val chineseStrings: Map<String, String> by lazy { chineseParts.fold(emptyMap()) { all, (_, part) -> all + part } }
