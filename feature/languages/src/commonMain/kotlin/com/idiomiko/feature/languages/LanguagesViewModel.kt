package com.idiomiko.feature.languages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idiomiko.core.domain.service.LanguageRemoval
import com.idiomiko.core.domain.service.LanguageRemovalService
import com.idiomiko.core.domain.service.RemovalJob
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.domain.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The native and interface languages, and the removal of a language being learned; the language
 * being learned is chosen as in the top bar, through `LocalLearningLanguage`.
 */
class LanguagesViewModel(private val settings: SettingsRepository, private val removal: LanguageRemovalService? = null) : ViewModel() {
    val state: StateFlow<UserSettings> = settings.settings

    /** A language asked about before it is removed, with what removing it deletes once counted. */
    data class RemovalPrompt(val languageId: Long, val name: String, val removal: LanguageRemoval? = null)

    private val _removing = MutableStateFlow<RemovalPrompt?>(null)
    val removing: StateFlow<RemovalPrompt?> = _removing.asStateFlow()

    /** The languages being removed in the background, under their English names. */
    val removals: StateFlow<List<String>> = (removal?.jobs ?: MutableStateFlow(emptyList<RemovalJob>()))
        .map { jobs -> jobs.filter { !it.done }.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
        _removing.value = null
    }

    /** Removes the language asked about in the background; the app says when it is done, wherever the reader is by then. */
    fun remove() {
        val prompt = _removing.value ?: return
        _removing.value = null
        removal?.startRemoval(prompt.languageId, prompt.name)
    }
}
