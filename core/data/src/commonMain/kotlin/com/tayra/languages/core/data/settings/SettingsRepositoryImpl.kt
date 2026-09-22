package com.tayra.languages.core.data.settings

import com.russhwolf.settings.Settings
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Stores [UserSettings] as key-value pairs; an in-memory snapshot backs the flow so
 * that the same code works on platforms whose storage cannot be observed.
 */
class SettingsRepositoryImpl(private val store: Settings) : SettingsRepository {

    private val state = MutableStateFlow(load())

    override val settings: StateFlow<UserSettings> = state.asStateFlow()

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        val updated = state.updateAndGet(transform)
        persist(updated)
    }

    private fun MutableStateFlow<UserSettings>.updateAndGet(transform: (UserSettings) -> UserSettings): UserSettings {
        var result: UserSettings
        do {
            val current = value
            result = transform(current)
        } while (!compareAndSet(current, result))
        return result
    }

    private fun load(): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            currentLanguageId = store.getLong(Keys.CURRENT_LANGUAGE, defaults.currentLanguageId),
            themeId = store.getString(Keys.THEME, defaults.themeId),
            showHighlights = store.getBoolean(Keys.SHOW_HIGHLIGHTS, defaults.showHighlights),
            showStreakOnHome = store.getBoolean(Keys.SHOW_STREAK, defaults.showStreakOnHome),
            statsSampleSize = store.getInt(Keys.STATS_SAMPLE_SIZE, defaults.statsSampleSize),
            promoteParentTranslation = store.getBoolean(Keys.PROMOTE_PARENT_TRANSLATION, defaults.promoteParentTranslation),
            showComponents = store.getBoolean(Keys.SHOW_COMPONENTS, defaults.showComponents),
            readingFontScale = store.getFloat(Keys.FONT_SCALE, defaults.readingFontScale),
            readingLineHeight = store.getFloat(Keys.LINE_HEIGHT, defaults.readingLineHeight),
            readingColumnWidth = store.getInt(Keys.COLUMN_WIDTH, defaults.readingColumnWidth),
            focusMode = store.getBoolean(Keys.FOCUS_MODE, defaults.focusMode),
            tapSetsStatus = store.getBoolean(Keys.TAP_SETS_STATUS, defaults.tapSetsStatus),
            demoDataLoaded = store.getBoolean(Keys.DEMO_DATA, defaults.demoDataLoaded),
            hotkeys = HotkeyAction.entries.associateWith { action ->
                val stored = store.getStringOrNull(action.settingKey)
                if (stored == null) action.default else Hotkey.parse(stored)
            },
        )
    }

    private fun persist(s: UserSettings) {
        store.putLong(Keys.CURRENT_LANGUAGE, s.currentLanguageId)
        store.putString(Keys.THEME, s.themeId)
        store.putBoolean(Keys.SHOW_HIGHLIGHTS, s.showHighlights)
        store.putBoolean(Keys.SHOW_STREAK, s.showStreakOnHome)
        store.putInt(Keys.STATS_SAMPLE_SIZE, s.statsSampleSize)
        store.putBoolean(Keys.PROMOTE_PARENT_TRANSLATION, s.promoteParentTranslation)
        store.putBoolean(Keys.SHOW_COMPONENTS, s.showComponents)
        store.putFloat(Keys.FONT_SCALE, s.readingFontScale)
        store.putFloat(Keys.LINE_HEIGHT, s.readingLineHeight)
        store.putInt(Keys.COLUMN_WIDTH, s.readingColumnWidth)
        store.putBoolean(Keys.FOCUS_MODE, s.focusMode)
        store.putBoolean(Keys.TAP_SETS_STATUS, s.tapSetsStatus)
        store.putBoolean(Keys.DEMO_DATA, s.demoDataLoaded)
        for (action in HotkeyAction.entries) {
            store.putString(action.settingKey, s.hotkeys[action]?.serialized ?: "")
        }
    }

    private object Keys {
        const val CURRENT_LANGUAGE = "current_language_id"
        const val THEME = "current_theme"
        const val SHOW_HIGHLIGHTS = "show_highlights"
        const val SHOW_STREAK = "show_streak_on_home"
        const val STATS_SAMPLE_SIZE = "stats_calc_sample_size"
        const val PROMOTE_PARENT_TRANSLATION = "term_popup_promote_parent_translation"
        const val SHOW_COMPONENTS = "term_popup_show_components"
        const val FONT_SCALE = "reading_font_scale"
        const val LINE_HEIGHT = "reading_line_height"
        const val COLUMN_WIDTH = "reading_column_width"
        const val FOCUS_MODE = "reading_focus_mode"
        const val TAP_SETS_STATUS = "reading_tap_sets_status"
        const val DEMO_DATA = "is_demo_data"
    }
}
