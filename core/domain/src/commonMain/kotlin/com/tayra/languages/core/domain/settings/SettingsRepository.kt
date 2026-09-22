package com.tayra.languages.core.domain.settings

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<UserSettings>

    val current: UserSettings get() = settings.value

    suspend fun update(transform: (UserSettings) -> UserSettings)
}
