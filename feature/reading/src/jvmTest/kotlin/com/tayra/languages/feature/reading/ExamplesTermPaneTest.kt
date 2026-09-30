package com.tayra.languages.feature.reading

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.click
import androidx.compose.ui.text.TextLayoutResult
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.feature.terms.examples.ExamplesSearchScreen
import com.tayra.languages.feature.terms.examples.ExamplesSearchViewModel
import com.tayra.languages.feature.terms.form.TermFormKey
import com.tayra.languages.feature.terms.form.TermFormViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.io.File
import kotlin.test.Test

/** The Examples screen shows the term pane beside the results, following the clicked word. */
class ExamplesTermPaneTest {

    @get:Rule
    val rule = createComposeRule()

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun thePaneShowsTheSearchedTermThenTheClickedWord() {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-examples", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val languageId = runBlocking { languages.save(Language(name = "Portuguese")) }
        val examples = object : ExampleSentencesProvider {
            override suspend fun search(query: ExampleSearchQuery) =
                ExampleSearchResult(listOf(ExampleSentence(text = "O tempo voa depressa.", translation = "Время летит быстро.")), 1, null)
            override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        }
        val engine = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = null
        }
        val dictionaries = DictionaryService(
            object : DictionaryPackStore {
                override suspend fun installedSize(pack: DictionaryPack): Long? = null
                override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) = error("offline")
                override suspend fun remove(pack: DictionaryPack) {}
            },
            object : DictionaryRepository {
                override suspend fun isAvailable(dictionary: DictionaryId) = false
                override suspend fun entries(dictionary: DictionaryId, wordLc: String) = emptyList<DictionaryEntry>()
                override suspend fun lemmas(dictionary: DictionaryId, formLc: String) = emptyList<String>()
                override suspend fun close(dictionary: DictionaryId) {}
            },
        )
        val termService = TermService(terms, languages)
        startKoin {
            modules(module {
                single { LocalSpeech(emptyList()) }
                single<SettingsRepository> { settings }
                single { WordTranslationService(terms, dictionaries, engine, settings) }
                viewModel { (key: TermFormKey) -> TermFormViewModel(key, termService, terms, languages, settings, engine, examples, dictionaries, dictionaries) }
            })
        }
        val vm = ExamplesSearchViewModel(languageId, "tempo", languages, settings, examples)
        rule.setContent { ExamplesSearchScreen(languageId, "tempo", onNavigate = {}, onBack = {}, viewModel = vm) }

        rule.waitUntil(5_000) { termField("tempo") }
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("voa depressa", substring = true)).fetchSemanticsNodes().isNotEmpty() }

        val sentence = rule.onAllNodes(hasText("O tempo voa depressa.")).fetchSemanticsNodes().first { SemanticsActions.GetTextLayoutResult in it.config }
        val layouts = mutableListOf<TextLayoutResult>()
        sentence.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val box = layouts.single().getBoundingBox("O tempo voa depressa.".indexOf("voa") + 1)
        rule.onAllNodes(hasText("O tempo voa depressa."))[0].performMouseInput { click(Offset(box.center.x, box.center.y)) }
        rule.waitUntil(5_000) { termField("voa") }

        rule.onNodeWithContentDescription("Close").performClick()
        rule.waitUntil(5_000) { !termField("voa") }
    }

    private fun termField(text: String) = rule.onAllNodes(hasSetTextAction() and hasText(text)).fetchSemanticsNodes().isNotEmpty()
}
