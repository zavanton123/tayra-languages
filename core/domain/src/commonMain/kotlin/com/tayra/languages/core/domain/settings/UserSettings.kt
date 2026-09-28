package com.tayra.languages.core.domain.settings

import com.tayra.languages.core.domain.service.TranslationEngine

/**
 * User preferences. Persisted as key-value pairs; see [SettingsRepository].
 */
data class UserSettings(
    val currentLanguageId: Long = 0,
    val themeId: String = DEFAULT_THEME,
    val showHighlights: Boolean = true,
    val showStreakOnHome: Boolean = false,
    val statsSampleSize: Int = 5,
    val readingFontScale: Float = 1.0f,
    val readingLineHeight: Float = 1.6f,
    val readingColumnWidth: Int = 720,
    val focusMode: Boolean = false,
    val tapSetsStatus: Boolean = false,
    /** Start every sentence on its own line while reading. */
    val splitSentences: Boolean = false,
    /** Show a translation under every sentence while reading. */
    val showTranslations: Boolean = false,
    /** Original sentence on the left, its translation on the right, instead of the translation underneath. */
    val sideBySideTranslations: Boolean = false,
    val demoDataLoaded: Boolean = false,
    /** ISO 639-1 code of the user's native language; translations and example sentences are shown in it. */
    val nativeLanguage: String = "en",
    /** Optional contact email sent to MyMemory, which raises its daily quota. */
    val translationContactEmail: String = "",
    /** Service used for sentence translations. */
    val translationEngine: TranslationEngine = TranslationEngine.MYMEMORY,
    /** Google Cloud Translation API key, used only when [translationEngine] is [TranslationEngine.GOOGLE]. */
    val googleTranslateApiKey: String = "",
    /** Python executable that has Argos Translate installed; empty means `python3` from the PATH. */
    val argosPython: String = "",
    val hotkeys: Map<HotkeyAction, Hotkey?> = HotkeyAction.defaults,
) {
    fun hotkeyFor(action: HotkeyAction): Hotkey? = hotkeys[action]

    companion object {
        const val DEFAULT_THEME = "default"
        const val MIN_STATS_SAMPLE_SIZE = 1
        const val MAX_STATS_SAMPLE_SIZE = 500
    }
}
