package com.idiomiko.feature.frequency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idiomiko.core.domain.courses.CourseRepository
import com.idiomiko.core.domain.frequency.FrequencyList
import com.idiomiko.core.domain.frequency.LevelEdge
import com.idiomiko.core.domain.frequency.LevelEdges
import com.idiomiko.core.domain.frequency.VocabularyLevelService
import com.idiomiko.core.domain.language.Tutorials
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.parse.PARAGRAPH_MARK
import com.idiomiko.core.domain.parse.lowercase
import com.idiomiko.core.domain.parse.parseTokens
import com.idiomiko.core.domain.service.LexiconService
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.ui.state.UiEvents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A word of the example text, or the text between words, with the rank of the word if the list has it. */
data class ExampleToken(val text: String, val isWord: Boolean, val rank: Int?)

data class VocabularySettingsUiState(
    val loading: Boolean = true,
    val languageId: Long = 0,
    val languageName: String = "",
    /** Null when the app has no frequency list for the language. */
    val list: FrequencyList? = null,
    /** The level set, 0 for none. */
    val level: Int = 0,
    /** Whether the reader has ever chosen a level for the language ("just starting out" counts). */
    val chosen: Boolean = false,
    /** The level picked in the list, not set yet; null while it is the one set. */
    val picked: Int? = null,
    val saving: Boolean = false,
    /** A sample text in the language, to show what the picked level knows. */
    val example: List<ExampleToken> = emptyList(),
    /** Starter and the European levels the list reaches, with the words known at each. */
    val edges: List<LevelEdge> = listOf(LevelEdge(null, 0)),
) {
    val shownLevel: Int get() = picked ?: level
}

/** The vocabulary level of the language being learned: which of its most common words the reader knows. */
class VocabularySettingsViewModel(
    private val levels: VocabularyLevelService,
    private val languages: LanguageRepository,
    settings: SettingsRepository,
    private val lexicons: LexiconService? = null,
    private val courses: CourseRepository? = null,
) : ViewModel() {
    private val picked = MutableStateFlow<Int?>(null)
    private val saving = MutableStateFlow(false)
    val events = UiEvents<String>()

    private class Loaded(val id: Long, val name: String, val list: FrequencyList?, val example: List<ExampleToken>)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val language = settings.settings.map { it.currentLanguageId }.distinctUntilChanged().flatMapLatest { id ->
        flow {
            picked.value = null
            val language = languages.getById(id)
            val list = levels.list(id)
            emit(Loaded(id, language?.name.orEmpty(), list, if (language != null && list != null) example(language, list) else emptyList()))
        }.flatMapLatest { loaded ->
            // The levels end where the language's courses do, which may arrive after the list.
            val edges = (courses?.observeCourses(loaded.id) ?: flowOf(emptyList()))
                .map { LevelEdges.of(it, loaded.list?.words?.size ?: 0) }
                .distinctUntilChanged()
            combine(levels.observeChosenLevel(loaded.id), edges) { chosen, levelEdges -> Triple(loaded, chosen, levelEdges) }
        }
    }

    val state: StateFlow<VocabularySettingsUiState> = combine(language, picked, saving) { (loaded, chosen, edges), pick, busy ->
        val level = chosen ?: 0
        VocabularySettingsUiState(
            loading = false,
            languageId = loaded.id,
            languageName = loaded.name,
            list = loaded.list,
            level = level,
            chosen = chosen != null,
            // A first choice may be the level already shown: "just starting out" is 0 too.
            picked = pick?.takeIf { it != level || chosen == null },
            saving = busy,
            example = loaded.example,
            edges = edges,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VocabularySettingsUiState())

    fun pick(level: Int) { picked.value = level }

    /** Sets the picked level: the words ranked up to it are saved as known. */
    fun save() {
        val level = state.value.picked ?: return
        if (saving.value) return
        saving.value = true
        viewModelScope.launch {
            try {
                levels.setLevel(state.value.languageId, level)?.let { events.send(it.message()) }
                picked.value = null
            } finally {
                saving.value = false
            }
        }
    }

    /** The start of the language's tutorial, split into words as the reader splits them, with their ranks. */
    private suspend fun example(language: Language, list: FrequencyList): List<ExampleToken> {
        val story = Tutorials.forLanguage(language.name)?.text ?: return emptyList()
        val ranks = HashMap<String, Int>()
        for (word in list.words) {
            ranks.getOrPut(word.key) { word.rank }
            for (form in word.forms) ranks.getOrPut(form) { word.rank }
        }
        lexicons?.prepare(language)
        // Cut at a space where the text has spaces; a text without them (Japanese, Thai) is cut where it is.
        val start = story.take(EXAMPLE_LENGTH)
        val text = start.lastIndexOf(' ').takeIf { it > EXAMPLE_LENGTH / 2 }?.let { start.substring(0, it) } ?: start
        return language.parseTokens(text).map { token ->
            if (token.token == PARAGRAPH_MARK) ExampleToken("\n\n", isWord = false, rank = null)
            else ExampleToken(token.token, token.isWord, if (token.isWord) ranks[language.lowercase(token.token)] else null)
        }
    }

    private companion object {
        const val EXAMPLE_LENGTH = 700
    }
}
