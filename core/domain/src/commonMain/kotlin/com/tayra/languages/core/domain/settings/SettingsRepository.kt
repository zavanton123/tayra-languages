package com.tayra.languages.core.domain.settings

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<UserSettings>

    val current: UserSettings get() = settings.value

    suspend fun update(transform: (UserSettings) -> UserSettings)

    /** Where secrets such as API keys are kept on this platform, as a phrase for Settings. */
    val secretStorage: String get() = ""
}
