package com.tayra.languages.feature.languages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The native and interface languages; the language being learned is chosen as in the top bar, through `LocalLearningLanguage`. */
class LanguagesViewModel(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<UserSettings> = settings.settings

    fun setNativeLanguage(code: String) = viewModelScope.launch { settings.update { it.copy(nativeLanguage = code) } }

    fun setInterfaceLanguage(code: String) = viewModelScope.launch { settings.update { it.copy(uiLanguage = code) } }
}
