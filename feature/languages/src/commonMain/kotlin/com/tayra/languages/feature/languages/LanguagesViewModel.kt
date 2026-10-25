package com.tayra.languages.feature.languages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.service.LanguageRemoval
import com.tayra.languages.core.domain.service.LanguageRemovalService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The native and interface languages, and the removal of a language being learned; the language
 * being learned is chosen as in the top bar, through `LocalLearningLanguage`.
 */
class LanguagesViewModel(private val settings: SettingsRepository, private val removal: LanguageRemovalService? = null) : ViewModel() {
    val state: StateFlow<UserSettings> = settings.settings

    /**
     * A language asked about before it is removed, with what removing it deletes once counted; while
     * [removing], it cannot be called off. [problems] are what could not be removed, after.
     */
    data class RemovalPrompt(val languageId: Long, val name: String, val removal: LanguageRemoval? = null, val removing: Boolean = false, val problems: List<String> = emptyList())

    private val _removing = MutableStateFlow<RemovalPrompt?>(null)
    val removing: StateFlow<RemovalPrompt?> = _removing.asStateFlow()

    /** Sets the native language, unless it is [learningCode], the language being learned. */
    fun setNativeLanguage(code: String, learningCode: String?) = viewModelScope.launch {
        if (code != learningCode) settings.update { it.copy(nativeLanguage = code) }
    }

    fun setInterfaceLanguage(code: String) = viewModelScope.launch { settings.update { it.copy(uiLanguage = code) } }

    /** Asks whether to remove the language with [languageId], counting what would go. */
    fun askToRemove(languageId: Long, name: String) {
        _removing.value = RemovalPrompt(languageId, name)
        viewModelScope.launch {
            val counted = removal?.preview(languageId) ?: return@launch
            _removing.update { if (it?.languageId == languageId) it.copy(removal = counted) else it }
        }
    }

    fun dismissRemoval() {
        if (_removing.value?.removing != true) _removing.value = null
    }

    fun remove() {
        val prompt = _removing.value ?: return
        if (prompt.removing) return
        _removing.value = prompt.copy(removing = true)
        viewModelScope.launch {
            val problems = removal?.remove(prompt.languageId).orEmpty()
            _removing.value = if (problems.isEmpty()) null else prompt.copy(removing = false, problems = problems)
        }
    }
}
