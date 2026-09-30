package com.tayra.languages.core.domain.settings

import com.tayra.languages.core.domain.service.SpeechEngine
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
    /** A play button before each sentence that reads it aloud. */
    val showSentencePlay: Boolean = true,
    /** Clicking or tapping a word in the reader reads it aloud. */
    val speakWordOnClick: Boolean = false,
    /** The engine that reads sentences and terms aloud. */
    val speechEngine: SpeechEngine = SpeechEngine.SYSTEM,
    /** The chosen voice per engine and language, keyed `ENGINE:code`. */
    val speechVoices: Map<String, String> = emptyMap(),
    /** Playback speed for the local engines, 1 being normal. */
    val speechSpeed: Float = 1f,
    val demoDataLoaded: Boolean = false,
    /** ISO 639-1 code of the user's native language; translations and example sentences are shown in it. */
    val nativeLanguage: String = "en",
    /** Optional contact email sent to MyMemory, which raises its daily quota. */
    val translationContactEmail: String = "",
    /** Service used for sentence translations. */
    val translationEngine: TranslationEngine = TranslationEngine.ARGOS,
    /** Google Cloud Translation API key, used only when [translationEngine] is [TranslationEngine.GOOGLE]. */
    val googleTranslateApiKey: String = "",
    /** Azure AI Translator key, used only when [translationEngine] is [TranslationEngine.AZURE]. */
    val azureTranslatorApiKey: String = "",
    /** The Azure resource's region, such as westeurope; required for regional and multi-service resources. */
    val azureTranslatorRegion: String = "",
    /** Alibaba Cloud RAM AccessKey pair, used only when [translationEngine] is [TranslationEngine.ALIBABA]. */
    val alibabaAccessKeyId: String = "",
    val alibabaAccessKeySecret: String = "",
    /** Machine Translation endpoint host; mt.aliyuncs.com serves both Chinese and international accounts. */
    val alibabaEndpoint: String = "mt.aliyuncs.com",
    /** Baidu Translate open platform App ID and secret key, used only when [translationEngine] is [TranslationEngine.BAIDU]. */
    val baiduAppId: String = "",
    val baiduSecretKey: String = "",
    /** DeepL API key; a key ending in `:fx` is a free-plan key and goes to the free host. */
    val deeplApiKey: String = "",
    /** Alibaba Model Studio (DashScope) API key for the Qwen-MT models. */
    val qwenApiKey: String = "",
    /** qwen-mt-turbo (cheaper) or qwen-mt-plus (better). */
    val qwenModel: String = "qwen-mt-turbo",
    /** Model Studio region: the international endpoint or the mainland China one. */
    val qwenInternational: Boolean = true,
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
