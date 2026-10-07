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
    /** The typeface of the reader's text, by id; see the reading fonts in the UI. */
    val readingFont: String = DEFAULT_READING_FONT,
    val readingColumnWidth: Int = 720,
    /** Whether the reading text is justified rather than aligned to the start. */
    val readingJustified: Boolean = false,
    val focusMode: Boolean = false,
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
    /** Continuous reading stops after each sentence, so the next press of play reads the following one. */
    val autoPause: Boolean = false,
    /** The engine that reads sentences and terms aloud. */
    val speechEngine: SpeechEngine = SpeechEngine.SYSTEM,
    /** The chosen voice per engine and language, keyed `ENGINE:code`. */
    val speechVoices: Map<String, String> = emptyMap(),
    /** Playback speed for the local engines, 1 being normal. */
    val speechSpeed: Float = 1f,
    val demoDataLoaded: Boolean = false,
    /** Whether the languages got their tutorial book, which replaced the sample books of earlier versions. */
    val tutorialBooksAdded: Boolean = false,
    /** ISO 639-1 code of the user's native language; translations and example sentences are shown in it. */
    val nativeLanguage: String = "en",
    /** ISO 639-1 code of the language the interface is shown in; so far the interface exists only in English. */
    val uiLanguage: String = "en",
    /** Optional contact email sent to MyMemory, which raises its daily quota. */
    val translationContactEmail: String = "",
    /** Service used for sentence translations. */
    val translationEngine: TranslationEngine = TranslationEngine.ARGOS,
    /** Google Cloud Translation API key, used only when [translationEngine] is [TranslationEngine.GOOGLE]. */
    val googleTranslateApiKey: String = "",
    /** Python executable that has Argos Translate installed; empty means `python3` from the PATH. */
    val argosPython: String = "",
    /** New flashcards introduced per day, per language. */
    val flashcardNewPerDay: Int = 20,
    /** Reviews shown per day, per language; 0 is no limit. */
    val flashcardReviewsPerDay: Int = 0,
    /** The chance of recall flashcard reviews are timed for, in percent. */
    val flashcardRetention: Int = 90,
    /** Waits before a new card comes back while it is learned, e.g. "1m 10m"; see the flashcards' LearningSteps. */
    val flashcardLearnSteps: String = "1m 10m",
    /** Waits before a forgotten card comes back. */
    val flashcardRelearnSteps: String = "10m",
    /** Read the sentence (or word) aloud when a flashcard's answer is shown. */
    val flashcardAutoplay: Boolean = true,
    val hotkeys: Map<HotkeyAction, Hotkey?> = HotkeyAction.defaults,
) {
    fun hotkeyFor(action: HotkeyAction): Hotkey? = hotkeys[action]

    companion object {
        const val DEFAULT_THEME = "default"
        const val DEFAULT_READING_FONT = "serif"
        const val MIN_STATS_SAMPLE_SIZE = 1
        const val MAX_STATS_SAMPLE_SIZE = 500
        const val MAX_FLASHCARDS_PER_DAY = 9999
        const val MIN_FLASHCARD_RETENTION = 70
        const val MAX_FLASHCARD_RETENTION = 97
    }
}
