package com.tayra.languages.feature.languages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.model.DictionaryType
import com.tayra.languages.core.domain.model.DictionaryUse
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.LanguageDictionary
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.LanguageService
import com.tayra.languages.core.domain.service.LanguageValidationException
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LanguageEditUiState(
    val loading: Boolean = true,
    val language: Language = Language(),
    val error: String? = null,
    val saving: Boolean = false,
) {
    val isNew: Boolean get() = language.id == 0L
}

sealed interface LanguageEditEvent {
    data object Saved : LanguageEditEvent
}

class LanguageEditViewModel(
    private val languageId: Long?,
    private val predefinedName: String?,
    private val languages: LanguageRepository,
    private val languageService: LanguageService,
) : ViewModel() {

    private val _state = MutableStateFlow(LanguageEditUiState())
    val state: StateFlow<LanguageEditUiState> = _state.asStateFlow()
    val events = UiEvents<LanguageEditEvent>()

    init {
        viewModelScope.launch {
            val language = when {
                languageId != null -> languages.getById(languageId) ?: Language()
                predefinedName != null -> languageService.predefined(predefinedName)?.language ?: Language()
                else -> Language(
                    dictionaries = listOf(
                        LanguageDictionary(useFor = DictionaryUse.TERMS, type = DictionaryType.EMBEDDED, url = ""),
                        LanguageDictionary(useFor = DictionaryUse.SENTENCES, type = DictionaryType.POPUP, url = ""),
                    ),
                )
            }
            _state.update { it.copy(loading = false, language = language) }
        }
    }

    fun update(transform: (Language) -> Language) {
        _state.update { it.copy(language = transform(it.language), error = null) }
    }

    fun updateDictionary(index: Int, transform: (LanguageDictionary) -> LanguageDictionary) = update { language ->
        language.copy(dictionaries = language.dictionaries.mapIndexed { i, d -> if (i == index) transform(d) else d })
    }

    fun addDictionary() = update { it.copy(dictionaries = it.dictionaries + LanguageDictionary(useFor = DictionaryUse.TERMS, type = DictionaryType.EMBEDDED, url = "")) }

    fun removeDictionary(index: Int) = update { it.copy(dictionaries = it.dictionaries.filterIndexed { i, _ -> i != index }) }

    fun moveDictionary(index: Int, delta: Int) = update { language ->
        val target = index + delta
        if (target !in language.dictionaries.indices) return@update language
        val list = language.dictionaries.toMutableList()
        val item = list.removeAt(index)
        list.add(target, item)
        language.copy(dictionaries = list)
    }

    fun save() {
        val language = _state.value.language.copy(
            dictionaries = _state.value.language.dictionaries.mapIndexed { i, d -> d.copy(sortOrder = i + 1) },
        )
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                languageService.save(language)
                events.send(LanguageEditEvent.Saved)
            } catch (e: LanguageValidationException) {
                _state.update { it.copy(saving = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = e.message ?: "Could not save language") }
            }
        }
    }
}
