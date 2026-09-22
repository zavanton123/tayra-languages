package com.tayra.languages.core.domain.settings

/**
 * User preferences. Persisted as key-value pairs; see [SettingsRepository].
 */
data class UserSettings(
    val currentLanguageId: Long = 0,
    val themeId: String = DEFAULT_THEME,
    val showHighlights: Boolean = true,
    val showStreakOnHome: Boolean = false,
    val statsSampleSize: Int = 5,
    val promoteParentTranslation: Boolean = true,
    val showComponents: Boolean = true,
    val readingFontScale: Float = 1.0f,
    val readingLineHeight: Float = 1.6f,
    val readingColumnWidth: Int = 720,
    val focusMode: Boolean = false,
    val tapSetsStatus: Boolean = false,
    val demoDataLoaded: Boolean = false,
    val hotkeys: Map<HotkeyAction, Hotkey?> = HotkeyAction.defaults,
) {
    fun hotkeyFor(action: HotkeyAction): Hotkey? = hotkeys[action]

    companion object {
        const val DEFAULT_THEME = "default"
        const val MIN_STATS_SAMPLE_SIZE = 1
        const val MAX_STATS_SAMPLE_SIZE = 500
    }
}
