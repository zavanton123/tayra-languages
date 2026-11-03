package com.tayra.languages.core.data.settings

import com.tayra.languages.core.domain.flashcards.BacklogOrder
import com.tayra.languages.core.domain.flashcards.NewCardMix
import com.tayra.languages.core.domain.flashcards.NewCardOrder
import com.tayra.languages.core.domain.settings.UserSettings

/** A setting that follows the account: its value as text, and when it last changed (0: never, here). */
data class SyncedValue(val name: String, val value: String, val at: Long)

/**
 * The settings that follow the account: the languages chosen and how learning is set up. The rest,
 * how the app looks, sounds and reaches its services, belongs to each device.
 */
internal object SyncedSettings {
    class Field(val name: String, val read: (UserSettings) -> String, val write: (UserSettings, String) -> UserSettings)

    private fun text(name: String, read: (UserSettings) -> String, write: (UserSettings, String) -> UserSettings) = Field(name, read, write)
    private fun number(name: String, read: (UserSettings) -> Int, write: (UserSettings, Int) -> UserSettings) =
        Field(name, { read(it).toString() }, { s, v -> v.toIntOrNull()?.let { write(s, it) } ?: s })
    private fun flag(name: String, read: (UserSettings) -> Boolean, write: (UserSettings, Boolean) -> UserSettings) =
        Field(name, { read(it).toString() }, { s, v -> v.toBooleanStrictOrNull()?.let { write(s, it) } ?: s })

    /** The languages being learned, by this device's ids: sync names them by code. */
    const val MY_LANGUAGES = "myLanguages"

    val fields: List<Field> = listOf(
        text("nativeLanguage", { it.nativeLanguage }) { s, v -> s.copy(nativeLanguage = v) },
        text(MY_LANGUAGES, { it.myLanguages }) { s, v -> s.copy(myLanguages = v) },
        flag("showLearningLevels", { it.showLearningLevels }) { s, v -> s.copy(showLearningLevels = v) },
        number("flashcardNewPerDay", { it.flashcardNewPerDay }) { s, v -> s.copy(flashcardNewPerDay = v.coerceIn(0, UserSettings.MAX_FLASHCARDS_PER_DAY)) },
        number("flashcardReviewsPerDay", { it.flashcardReviewsPerDay }) { s, v -> s.copy(flashcardReviewsPerDay = v.coerceIn(0, UserSettings.MAX_FLASHCARDS_PER_DAY)) },
        number("flashcardRetention", { it.flashcardRetention }) { s, v -> s.copy(flashcardRetention = v.coerceIn(UserSettings.MIN_FLASHCARD_RETENTION, UserSettings.MAX_FLASHCARD_RETENTION)) },
        text("flashcardLearnSteps", { it.flashcardLearnSteps }) { s, v -> s.copy(flashcardLearnSteps = v) },
        text("flashcardRelearnSteps", { it.flashcardRelearnSteps }) { s, v -> s.copy(flashcardRelearnSteps = v) },
        flag("flashcardTwoButtons", { it.flashcardTwoButtons }) { s, v -> s.copy(flashcardTwoButtons = v) },
        flag("flashcardLoadBalancing", { it.flashcardLoadBalancing }) { s, v -> s.copy(flashcardLoadBalancing = v) },
        text("flashcardEasyDays", { it.flashcardEasyDays }) { s, v -> s.copy(flashcardEasyDays = v) },
        text("flashcardBacklogOrder", { it.flashcardBacklogOrder.name }) { s, v -> s.copy(flashcardBacklogOrder = BacklogOrder.entries.firstOrNull { it.name == v } ?: s.flashcardBacklogOrder) },
        text("flashcardNewOrder", { it.flashcardNewOrder.name }) { s, v -> s.copy(flashcardNewOrder = NewCardOrder.entries.firstOrNull { it.name == v } ?: s.flashcardNewOrder) },
        text("flashcardNewMix", { it.flashcardNewMix.name }) { s, v -> s.copy(flashcardNewMix = NewCardMix.entries.firstOrNull { it.name == v } ?: s.flashcardNewMix) },
        number("flashcardLeechThreshold", { it.flashcardLeechThreshold }) { s, v -> s.copy(flashcardLeechThreshold = v.coerceIn(0, UserSettings.MAX_LEECH_THRESHOLD)) },
        flag("flashcardLeechSuspend", { it.flashcardLeechSuspend }) { s, v -> s.copy(flashcardLeechSuspend = v) },
        text("practiceSetupReading", { it.practiceSetupReading }) { s, v -> s.copy(practiceSetupReading = v) },
        text("practiceSetupVocabulary", { it.practiceSetupVocabulary }) { s, v -> s.copy(practiceSetupVocabulary = v) },
    )

    fun stampKey(name: String) = "sync.setting.$name"
}
