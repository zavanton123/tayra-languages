package com.tayra.languages.feature.frequency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.frequency.VocabularyLevelService
import com.tayra.languages.core.domain.language.Tutorials
import com.tayra.languages.core.domain.parse.lowercase
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
) {
    val shownLevel: Int get() = picked ?: level
}

/** The vocabulary level of the language being learned: which of its most common words the reader knows. */
class VocabularySettingsViewModel(
    private val levels: VocabularyLevelService,
    private val languages: LanguageRepository,
    settings: SettingsRepository,
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
        }.flatMapLatest { loaded -> levels.observeChosenLevel(loaded.id).map { loaded to it } }
    }

    val state: StateFlow<VocabularySettingsUiState> = combine(language, picked, saving) { (loaded, chosen), pick, busy ->
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
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VocabularySettingsUiState())

    fun pick(level: Int) { picked.value = level }

    /** Closes without choosing, so the reader is asked again next time. */
    fun forgetPick() { picked.value = null }

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

    /** The start of the language's tutorial, split into words with their ranks. */
    private fun example(language: com.tayra.languages.core.domain.model.Language, list: FrequencyList): List<ExampleToken> {
        val story = Tutorials.forLanguage(language.name)?.text ?: return emptyList()
        val ranks = HashMap<String, Int>()
        for (word in list.words) {
            ranks.getOrPut(word.key) { word.rank }
            for (form in word.forms) ranks.getOrPut(form) { word.rank }
        }
        return tokens(story.take(EXAMPLE_LENGTH).substringBeforeLast(' ')).map { (text, isWord) ->
            ExampleToken(text, isWord, if (isWord) ranks[language.lowercase(text)] else null)
        }
    }

    private companion object {
        const val EXAMPLE_LENGTH = 700

        /** The text cut into runs of letters (words) and runs of everything else. */
        fun tokens(text: String): List<Pair<String, Boolean>> {
            val out = ArrayList<Pair<String, Boolean>>()
            var start = 0
            fun isWordChar(c: Char) = c.isLetter() || c == '́' || c == '\''
            while (start < text.length) {
                val word = isWordChar(text[start])
                var end = start + 1
                while (end < text.length && isWordChar(text[end]) == word) end++
                out += text.substring(start, end) to word
                start = end
            }
            return out
        }
    }
}
