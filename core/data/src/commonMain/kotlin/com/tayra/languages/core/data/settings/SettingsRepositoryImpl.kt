package com.tayra.languages.core.data.settings

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.TranslationEngine
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
class SettingsRepositoryImpl(
    private val store: Settings,
    /** Holds the API keys; the plain [store] never sees them. */
    private val secure: SecureStore = InMemorySecureStore(),
) : SettingsRepository {

    private val state = MutableStateFlow(load())

    override val settings: StateFlow<UserSettings> = state.asStateFlow()

    override val secretStorage: String get() = secure.description

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
        forgetRetiredSettings()
        val defaults = UserSettings()
        return UserSettings(
            currentLanguageId = store.getLong(Keys.CURRENT_LANGUAGE, defaults.currentLanguageId),
            themeId = store.getString(Keys.THEME, defaults.themeId),
            showHighlights = store.getBoolean(Keys.SHOW_HIGHLIGHTS, defaults.showHighlights),
            showStreakOnHome = store.getBoolean(Keys.SHOW_STREAK, defaults.showStreakOnHome),
            statsSampleSize = store.getInt(Keys.STATS_SAMPLE_SIZE, defaults.statsSampleSize),
            readingFontScale = store.getFloat(Keys.FONT_SCALE, defaults.readingFontScale),
            readingLineHeight = store.getFloat(Keys.LINE_HEIGHT, defaults.readingLineHeight),
            readingColumnWidth = store.getInt(Keys.COLUMN_WIDTH, defaults.readingColumnWidth),
            focusMode = store.getBoolean(Keys.FOCUS_MODE, defaults.focusMode),
            splitSentences = store.getBoolean(Keys.SPLIT_SENTENCES, defaults.splitSentences),
            showTranslations = store.getBoolean(Keys.SHOW_TRANSLATIONS, defaults.showTranslations),
            sideBySideTranslations = store.getBoolean(Keys.SIDE_BY_SIDE_TRANSLATIONS, defaults.sideBySideTranslations),
            showSentencePlay = store.getBoolean(Keys.SHOW_SENTENCE_PLAY, defaults.showSentencePlay),
            speakWordOnClick = store.getBoolean(Keys.SPEAK_WORD_ON_CLICK, defaults.speakWordOnClick),
            autoPause = store.getBoolean(Keys.AUTO_PAUSE, defaults.autoPause),
            speechEngine = SpeechEngine.entries.firstOrNull { it.name == store.getString(Keys.SPEECH_ENGINE, "") } ?: defaults.speechEngine,
            speechVoices = store.getString(Keys.SPEECH_VOICES, "").split('\n').mapNotNull { line -> line.split('\t').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap(),
            speechSpeed = store.getFloat(Keys.SPEECH_SPEED, defaults.speechSpeed),
            demoDataLoaded = store.getBoolean(Keys.DEMO_DATA, defaults.demoDataLoaded),
            nativeLanguage = LanguageCatalog.nativeOption(
                store.getStringOrNull(Keys.NATIVE_LANGUAGE) ?: store.getString(Keys.LEGACY_TRANSLATION_TARGET, defaults.nativeLanguage),
            ).code,
            translationContactEmail = store.getString(Keys.TRANSLATION_EMAIL, defaults.translationContactEmail),
            translationEngine = TranslationEngine.entries.firstOrNull { it.name == store.getString(Keys.TRANSLATION_ENGINE, "") } ?: defaults.translationEngine,
            googleTranslateApiKey = loadSecret(Keys.GOOGLE_TRANSLATE_API_KEY),
            argosPython = store.getString(Keys.ARGOS_PYTHON, defaults.argosPython),
            hotkeys = loadHotkeys(),
        )
    }

    /**
     * The saved shortcuts. Saving settings stores every shortcut, so defaults added or changed later
     * are applied once here: an unassigned page shortcut gets its new key, and a word shortcut still
     * on its old plain arrow, or clashing with another shortcut, moves to its Ctrl arrow.
     */
    private fun loadHotkeys(): Map<HotkeyAction, Hotkey?> {
        val stored = HotkeyAction.entries.associateWith { store.getStringOrNull(it.settingKey) }
        val saved = stored.mapValues { (action, value) -> if (value == null) action.default else Hotkey.parse(value) }
        val laterDefaults = !store.getBoolean(Keys.LATER_HOTKEY_DEFAULTS, false)
        val ctrlWords = !store.getBoolean(Keys.CTRL_WORD_HOTKEYS, false)
        val knownKey = !store.getBoolean(Keys.KNOWN_HOTKEY, false)
        val unknownKey = !store.getBoolean(Keys.UNKNOWN_HOTKEY, false)
        return saved.mapValues { (action, key) ->
            val oldDefault = HotkeyAction.changedDefaults[action]
            val clashes = key != null && saved.any { (other, otherKey) -> other != action && otherKey == key }
            when {
                laterDefaults && action in HotkeyAction.laterDefaults && stored[action] == "" -> action.default
                unknownKey && action in HotkeyAction.unknownKeyDefaults && stored[action] == "" -> action.default
                ctrlWords && oldDefault != null && stored[action] != null && (key == oldDefault || clashes) -> action.default
                knownKey && key != null && key == HotkeyAction.knownKeyChange[action] -> action.default
                else -> key
            }
        }
    }

    /** Reads a secret, moving a value an older build left in the plain store into the secure one. */
    private fun loadSecret(key: String): String {
        val legacy = store.getStringOrNull(key)
        if (legacy != null) {
            if (legacy.isNotBlank()) runCatching { secure.put(key, legacy) }.onFailure { Logger.w(it) { "Could not move $key to secure storage" } }
            store.remove(key)
            return legacy
        }
        return runCatching { secure.get(key) }.onFailure { Logger.w(it) { "Could not read $key from secure storage" } }.getOrNull().orEmpty()
    }

    /** Drops what was saved for translation services the app no longer offers, keys and secrets included. */
    private fun forgetRetiredSettings() {
        for (key in Keys.RETIRED) {
            if (store.hasKey(key)) store.remove(key)
            runCatching { secure.remove(key) }
        }
    }

    private fun storeSecret(key: String, value: String) {
        runCatching { if (value.isBlank()) secure.remove(key) else secure.put(key, value) }
            .onFailure { Logger.w(it) { "Could not write $key to secure storage" } }
    }

    private fun persist(s: UserSettings) {
        store.putLong(Keys.CURRENT_LANGUAGE, s.currentLanguageId)
        store.putString(Keys.THEME, s.themeId)
        store.putBoolean(Keys.SHOW_HIGHLIGHTS, s.showHighlights)
        store.putBoolean(Keys.SHOW_STREAK, s.showStreakOnHome)
        store.putInt(Keys.STATS_SAMPLE_SIZE, s.statsSampleSize)
        store.putFloat(Keys.FONT_SCALE, s.readingFontScale)
        store.putFloat(Keys.LINE_HEIGHT, s.readingLineHeight)
        store.putInt(Keys.COLUMN_WIDTH, s.readingColumnWidth)
        store.putBoolean(Keys.FOCUS_MODE, s.focusMode)
        store.putBoolean(Keys.SPLIT_SENTENCES, s.splitSentences)
        store.putBoolean(Keys.SHOW_TRANSLATIONS, s.showTranslations)
        store.putBoolean(Keys.SIDE_BY_SIDE_TRANSLATIONS, s.sideBySideTranslations)
        store.putBoolean(Keys.SHOW_SENTENCE_PLAY, s.showSentencePlay)
        store.putBoolean(Keys.SPEAK_WORD_ON_CLICK, s.speakWordOnClick)
        store.putBoolean(Keys.AUTO_PAUSE, s.autoPause)
        store.putString(Keys.SPEECH_ENGINE, s.speechEngine.name)
        store.putString(Keys.SPEECH_VOICES, s.speechVoices.entries.joinToString("\n") { "${it.key}\t${it.value}" })
        store.putFloat(Keys.SPEECH_SPEED, s.speechSpeed)
        store.putBoolean(Keys.DEMO_DATA, s.demoDataLoaded)
        store.putString(Keys.NATIVE_LANGUAGE, s.nativeLanguage)
        store.putString(Keys.TRANSLATION_EMAIL, s.translationContactEmail)
        store.putString(Keys.TRANSLATION_ENGINE, s.translationEngine.name)
        storeSecret(Keys.GOOGLE_TRANSLATE_API_KEY, s.googleTranslateApiKey)
        store.putString(Keys.ARGOS_PYTHON, s.argosPython)
        for (action in HotkeyAction.entries) {
            store.putString(action.settingKey, s.hotkeys[action]?.serialized ?: "")
        }
        store.putBoolean(Keys.LATER_HOTKEY_DEFAULTS, true)
        store.putBoolean(Keys.CTRL_WORD_HOTKEYS, true)
        store.putBoolean(Keys.KNOWN_HOTKEY, true)
        store.putBoolean(Keys.UNKNOWN_HOTKEY, true)
    }

    private object Keys {
        /** Settings of the Microsoft, Alibaba, Baidu, DeepL and Qwen translation services, removed on 2026-10-03. */
        val RETIRED = listOf(
            "azure_translator_api_key", "azure_translator_region", "alibaba_access_key_id", "alibaba_access_key_secret", "alibaba_endpoint",
            "baidu_app_id", "baidu_secret_key", "deepl_api_key", "qwen_api_key", "qwen_model", "qwen_international",
        )

        const val CURRENT_LANGUAGE = "current_language_id"
        const val THEME = "current_theme"
        const val SHOW_HIGHLIGHTS = "show_highlights"
        const val SHOW_STREAK = "show_streak_on_home"
        const val STATS_SAMPLE_SIZE = "stats_calc_sample_size"
        const val FONT_SCALE = "reading_font_scale"
        const val LINE_HEIGHT = "reading_line_height"
        const val COLUMN_WIDTH = "reading_column_width"
        const val FOCUS_MODE = "reading_focus_mode"
        const val SPLIT_SENTENCES = "reading_split_sentences"
        const val SIDE_BY_SIDE_TRANSLATIONS = "reading_side_by_side_translations"
        const val SHOW_SENTENCE_PLAY = "reading_show_sentence_play"
        const val SPEAK_WORD_ON_CLICK = "reading_speak_word_on_click"
        const val AUTO_PAUSE = "reading_auto_pause"
        const val LATER_HOTKEY_DEFAULTS = "hotkeys_later_defaults_applied"
        const val CTRL_WORD_HOTKEYS = "hotkeys_ctrl_word_applied_v2"
        const val KNOWN_HOTKEY = "hotkeys_known_on_k_applied"
        const val UNKNOWN_HOTKEY = "hotkeys_unknown_on_u_applied"
        const val SPEECH_ENGINE = "speech_engine"
        const val SPEECH_VOICES = "speech_voices"
        const val SPEECH_SPEED = "speech_speed"
        const val SHOW_TRANSLATIONS = "reading_show_translations"
        const val TRANSLATION_ENGINE = "translation_engine"
        const val GOOGLE_TRANSLATE_API_KEY = "google_translate_api_key"
        const val ARGOS_PYTHON = "argos_python"
        const val DEMO_DATA = "is_demo_data"
        const val NATIVE_LANGUAGE = "native_language"
        const val LEGACY_TRANSLATION_TARGET = "translation_target_language"
        const val TRANSLATION_EMAIL = "translation_contact_email"
    }
}
