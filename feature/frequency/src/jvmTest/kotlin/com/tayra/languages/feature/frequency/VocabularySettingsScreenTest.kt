package com.tayra.languages.feature.frequency

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.VocabularyLevelRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.frequency.VocabularyLevelService
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The vocabulary settings pick a level from sample rows, preview it on a text, and save its words as known. */
@OptIn(ExperimentalTestApi::class)
class VocabularySettingsScreenTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-vocabulary-settings", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val portuguese = runBlocking { languages.save(Language(name = "Portuguese")).also { id -> settings.update { it.copy(currentLanguageId = id) } } }
    private val viewModel = VocabularySettingsViewModel(
        VocabularyLevelService(ResourceFrequencyLists(), VocabularyLevelRepositoryImpl(provider), languages),
        languages,
        settings,
    )

    @Test
    fun pickingALevelAndSettingItSavesItsWordsAsKnown() = runDesktopComposeUiTest(width = 1586, height = 1100) {
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) { VocabularySettingsScreen(onNavigate = {}, viewModel = viewModel) }
                }
            }
        }
        waitUntil(timeoutMillis = 10_000) { viewModel.state.value.list != null }
        onNodeWithText("Not set").assertExists()
        onNodeWithText("I'm just starting out").assertExists()
        onNodeWithTag("set-vocabulary-level").assertIsNotEnabled()
        assertTrue(viewModel.state.value.example.count { it.isWord } > 50, "the example is a sample story")

        onNodeWithTag("level-500").performClick()
        onNodeWithTag("set-vocabulary-level").assertIsEnabled()
        waitForIdle()
        System.getenv("VOCABULARY_SETTINGS_SCREENSHOT")?.let { save(it) }

        onNodeWithTag("set-vocabulary-level").performScrollTo().performClick()
        onNodeWithText("Set your vocabulary level to 500?", substring = true).assertExists()
        onNodeWithText("Yes").performClick()
        waitUntil(timeoutMillis = 10_000) { viewModel.state.value.level == 500 && !viewModel.state.value.saving }
        assertEquals(TermStatus.WELL_KNOWN, runBlocking { terms.findByTextLc(portuguese, "dizer") }?.status)
        assertEquals(TermStatus.WELL_KNOWN, runBlocking { terms.findByTextLc(portuguese, "disse") }?.status)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Set to 500").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("set-vocabulary-level").assertIsNotEnabled()
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}
