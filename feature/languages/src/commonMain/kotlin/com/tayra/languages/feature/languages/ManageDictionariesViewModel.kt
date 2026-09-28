package com.tayra.languages.feature.languages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.language.LanguageOption
import com.tayra.languages.core.domain.language.OnlineDictionaries
import com.tayra.languages.core.domain.language.OnlineDictionary
import com.tayra.languages.core.domain.model.DictionaryUse
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.LanguageDictionary
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.LanguageService
import com.tayra.languages.core.domain.service.LanguageValidationException
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A dictionary shown on the manage screen: either stored on the language or offered from the catalog. */
data class DictionaryEntry(val name: String, val url: String, val stored: LanguageDictionary?, val catalog: OnlineDictionary?)

data class ManageDictionariesUiState(
    val loading: Boolean = true,
    val language: Language? = null,
    val source: LanguageOption? = null,
    val target: LanguageOption? = null,
    val preferred: List<DictionaryEntry> = emptyList(),
    val available: List<DictionaryEntry> = emptyList(),
    val message: String? = null,
)

class ManageDictionariesViewModel(
    private val languageId: Long,
    languages: LanguageRepository,
    private val languageService: LanguageService,
    settings: SettingsRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<ManageDictionariesUiState> = combine(
        languages.observeAll().map { list -> list.firstOrNull { it.id == languageId } },
        settings.settings.map { it.nativeLanguage },
        message,
    ) { language, native, msg ->
        if (language == null) return@combine ManageDictionariesUiState(loading = false, message = msg)
        val target = LanguageCatalog.nativeOption(native)
        val source = LanguageCodes.codeFor(language.name)?.let { LanguageOption(it, language.name) } ?: LanguageOption("en", language.name)
        val terms = language.dictionaries.filter { it.useFor == DictionaryUse.TERMS }.sortedBy { it.sortOrder }
        val labels = OnlineDictionaries.labels(terms, source, target)
        val stored = terms.map { DictionaryEntry(labels.getValue(it), it.url, it, OnlineDictionaries.match(it.url, source, target)) }
        val storedIds = stored.mapNotNull { it.catalog?.id }.toSet()
        val fromCatalog = OnlineDictionaries.all
            .filter { it.id !in storedIds }
            .mapNotNull { entry -> entry.url(source, target)?.let { DictionaryEntry(entry.displayName(source, target), it, null, entry) } }
        ManageDictionariesUiState(
            loading = false,
            language = language,
            source = source,
            target = target,
            preferred = stored.filter { it.stored!!.isActive },
            available = stored.filter { !it.stored!!.isActive } + fromCatalog,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageDictionariesUiState())

    fun enable(entry: DictionaryEntry) = update { language ->
        val stored = entry.stored
        if (stored != null) {
            language.copy(dictionaries = language.dictionaries.map { if (it === stored || (it.id != 0L && it.id == stored.id)) it.copy(isActive = true) else it })
        } else {
            if (language.dictionaries.any { it.url == entry.url }) return@update language
            val next = (language.dictionaries.maxOfOrNull { it.sortOrder } ?: 0) + 1
            language.copy(dictionaries = language.dictionaries + LanguageDictionary(useFor = DictionaryUse.TERMS, type = com.tayra.languages.core.domain.model.DictionaryType.POPUP, url = entry.url, isActive = true, sortOrder = next))
        }
    }

    fun disable(entry: DictionaryEntry) {
        val stored = entry.stored ?: return
        if (state.value.preferred.size <= 1) {
            message.value = "Keep at least one dictionary enabled."
            return
        }
        update { language ->
            language.copy(dictionaries = language.dictionaries.map { if (it === stored || (it.id != 0L && it.id == stored.id)) it.copy(isActive = false) else it })
        }
    }

    fun dismissMessage() { message.value = null }

    private fun update(transform: (Language) -> Language) {
        val language = state.value.language ?: return
        viewModelScope.launch {
            try {
                languageService.save(transform(language))
            } catch (e: LanguageValidationException) {
                message.value = e.message
            }
        }
    }
}
