package com.tayra.languages.core.data.settings

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
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
            tapSetsStatus = store.getBoolean(Keys.TAP_SETS_STATUS, defaults.tapSetsStatus),
            splitSentences = store.getBoolean(Keys.SPLIT_SENTENCES, defaults.splitSentences),
            showTranslations = store.getBoolean(Keys.SHOW_TRANSLATIONS, defaults.showTranslations),
            sideBySideTranslations = store.getBoolean(Keys.SIDE_BY_SIDE_TRANSLATIONS, defaults.sideBySideTranslations),
            showSentencePlay = store.getBoolean(Keys.SHOW_SENTENCE_PLAY, defaults.showSentencePlay),
            demoDataLoaded = store.getBoolean(Keys.DEMO_DATA, defaults.demoDataLoaded),
            nativeLanguage = LanguageCatalog.nativeOption(
                store.getStringOrNull(Keys.NATIVE_LANGUAGE) ?: store.getString(Keys.LEGACY_TRANSLATION_TARGET, defaults.nativeLanguage),
            ).code,
            translationContactEmail = store.getString(Keys.TRANSLATION_EMAIL, defaults.translationContactEmail),
            translationEngine = TranslationEngine.entries.firstOrNull { it.name == store.getString(Keys.TRANSLATION_ENGINE, "") } ?: defaults.translationEngine,
            googleTranslateApiKey = loadSecret(Keys.GOOGLE_TRANSLATE_API_KEY),
            azureTranslatorApiKey = loadSecret(Keys.AZURE_TRANSLATOR_API_KEY),
            azureTranslatorRegion = store.getString(Keys.AZURE_TRANSLATOR_REGION, defaults.azureTranslatorRegion),
            alibabaAccessKeyId = loadSecret(Keys.ALIBABA_ACCESS_KEY_ID),
            alibabaAccessKeySecret = loadSecret(Keys.ALIBABA_ACCESS_KEY_SECRET),
            alibabaEndpoint = store.getString(Keys.ALIBABA_ENDPOINT, defaults.alibabaEndpoint),
            baiduAppId = loadSecret(Keys.BAIDU_APP_ID),
            baiduSecretKey = loadSecret(Keys.BAIDU_SECRET_KEY),
            deeplApiKey = loadSecret(Keys.DEEPL_API_KEY),
            qwenApiKey = loadSecret(Keys.QWEN_API_KEY),
            qwenModel = store.getString(Keys.QWEN_MODEL, defaults.qwenModel),
            qwenInternational = store.getBoolean(Keys.QWEN_INTERNATIONAL, defaults.qwenInternational),
            argosPython = store.getString(Keys.ARGOS_PYTHON, defaults.argosPython),
            hotkeys = HotkeyAction.entries.associateWith { action ->
                val stored = store.getStringOrNull(action.settingKey)
                if (stored == null) action.default else Hotkey.parse(stored)
            },
        )
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
        store.putBoolean(Keys.TAP_SETS_STATUS, s.tapSetsStatus)
        store.putBoolean(Keys.SPLIT_SENTENCES, s.splitSentences)
        store.putBoolean(Keys.SHOW_TRANSLATIONS, s.showTranslations)
        store.putBoolean(Keys.SIDE_BY_SIDE_TRANSLATIONS, s.sideBySideTranslations)
        store.putBoolean(Keys.SHOW_SENTENCE_PLAY, s.showSentencePlay)
        store.putBoolean(Keys.DEMO_DATA, s.demoDataLoaded)
        store.putString(Keys.NATIVE_LANGUAGE, s.nativeLanguage)
        store.putString(Keys.TRANSLATION_EMAIL, s.translationContactEmail)
        store.putString(Keys.TRANSLATION_ENGINE, s.translationEngine.name)
        storeSecret(Keys.GOOGLE_TRANSLATE_API_KEY, s.googleTranslateApiKey)
        storeSecret(Keys.AZURE_TRANSLATOR_API_KEY, s.azureTranslatorApiKey)
        store.putString(Keys.AZURE_TRANSLATOR_REGION, s.azureTranslatorRegion)
        storeSecret(Keys.ALIBABA_ACCESS_KEY_ID, s.alibabaAccessKeyId)
        storeSecret(Keys.ALIBABA_ACCESS_KEY_SECRET, s.alibabaAccessKeySecret)
        store.putString(Keys.ALIBABA_ENDPOINT, s.alibabaEndpoint)
        storeSecret(Keys.BAIDU_APP_ID, s.baiduAppId)
        storeSecret(Keys.BAIDU_SECRET_KEY, s.baiduSecretKey)
        storeSecret(Keys.DEEPL_API_KEY, s.deeplApiKey)
        storeSecret(Keys.QWEN_API_KEY, s.qwenApiKey)
        store.putString(Keys.QWEN_MODEL, s.qwenModel)
        store.putBoolean(Keys.QWEN_INTERNATIONAL, s.qwenInternational)
        store.putString(Keys.ARGOS_PYTHON, s.argosPython)
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
        const val FONT_SCALE = "reading_font_scale"
        const val LINE_HEIGHT = "reading_line_height"
        const val COLUMN_WIDTH = "reading_column_width"
        const val FOCUS_MODE = "reading_focus_mode"
        const val TAP_SETS_STATUS = "reading_tap_sets_status"
        const val SPLIT_SENTENCES = "reading_split_sentences"
        const val SIDE_BY_SIDE_TRANSLATIONS = "reading_side_by_side_translations"
        const val SHOW_SENTENCE_PLAY = "reading_show_sentence_play"
        const val SHOW_TRANSLATIONS = "reading_show_translations"
        const val TRANSLATION_ENGINE = "translation_engine"
        const val GOOGLE_TRANSLATE_API_KEY = "google_translate_api_key"
        const val AZURE_TRANSLATOR_API_KEY = "azure_translator_api_key"
        const val AZURE_TRANSLATOR_REGION = "azure_translator_region"
        const val ALIBABA_ACCESS_KEY_ID = "alibaba_access_key_id"
        const val ALIBABA_ACCESS_KEY_SECRET = "alibaba_access_key_secret"
        const val ALIBABA_ENDPOINT = "alibaba_endpoint"
        const val BAIDU_APP_ID = "baidu_app_id"
        const val BAIDU_SECRET_KEY = "baidu_secret_key"
        const val DEEPL_API_KEY = "deepl_api_key"
        const val QWEN_API_KEY = "qwen_api_key"
        const val QWEN_MODEL = "qwen_model"
        const val QWEN_INTERNATIONAL = "qwen_international"
        const val ARGOS_PYTHON = "argos_python"
        const val DEMO_DATA = "is_demo_data"
        const val NATIVE_LANGUAGE = "native_language"
        const val LEGACY_TRANSLATION_TARGET = "translation_target_language"
        const val TRANSLATION_EMAIL = "translation_contact_email"
    }
}
