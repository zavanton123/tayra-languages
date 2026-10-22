package com.tayra.languages.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.service.ChineseScriptService
import com.tayra.languages.core.domain.service.CutWordsMigration
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.DemoDataService
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.TranslationLanguageKeeper
import com.tayra.languages.core.domain.service.LearningTranslations
import com.tayra.languages.core.domain.service.LearningLanguageService
import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.books.BookPackService
import com.tayra.languages.core.domain.courses.CourseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BootstrapState {
    data object Loading : BootstrapState
    data object Ready : BootstrapState
    data class Failed(val message: String) : BootstrapState
}

/**
 * Opens the database and makes sure the catalog languages exist, with demo data on first start,
 * then keeps saved translations in the native language, and gives words being learned a
 * translation, for as long as the app runs.
 */
class AppBootstrapViewModel(
    private val demoData: DemoDataService,
    private val bookStats: BookStatsService,
    private val dictionaries: DictionaryService,
    private val translationLanguages: TranslationLanguageKeeper,
    private val learningTranslations: LearningTranslations,
    private val learningLanguage: LearningLanguageService,
    private val courses: CourseService,
    private val coursePacks: CoursePackService,
    private val bookPacks: BookPackService,
    /** Loads the script tables before texts are parsed, when a Chinese language is read in one script. */
    private val chineseScripts: ChineseScriptService? = null,
    /** Turns the terms saved as words a parser now cuts (Arabic's والكتاب) into the words it cuts them into. */
    private val cutWords: CutWordsMigration? = null,
) : ViewModel() {

    private val _state = MutableStateFlow<BootstrapState>(BootstrapState.Loading)
    val state: StateFlow<BootstrapState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                demoData.ensureLanguages()
                courses.seedSamples()
                coursePacks.refresh()
                bookPacks.refresh()
                learningLanguage.ensure()
                chineseScripts?.prepareIfUsed()
                cutWords?.migrate()
                _state.value = BootstrapState.Ready
                bookStats.refreshAll()
                dictionaries.refresh()
                translationLanguages.start(viewModelScope)
                learningTranslations.start(viewModelScope)
            } catch (e: Exception) {
                Logger.e(e) { "Bootstrap failed" }
                _state.value = BootstrapState.Failed(e.message ?: e.toString())
            }
        }
    }
}
