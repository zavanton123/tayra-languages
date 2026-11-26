package com.idiomiko.feature.languages

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.BookRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.model.DictionaryType
import com.idiomiko.core.domain.model.DictionaryUse
import com.idiomiko.core.domain.language.LanguageDefinitions
import com.idiomiko.core.domain.model.LanguageDictionary
import com.idiomiko.core.domain.service.BookService
import com.idiomiko.core.domain.service.LanguageService
import com.idiomiko.core.ui.theme.IdiomikoTheme
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import kotlin.test.Test
import kotlin.test.assertEquals

/** The preferred dictionaries are dragged by their handles into the order lookups try them in. */
@OptIn(ExperimentalTestApi::class)
class ManageDictionariesScreenTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-dictionaries-ui", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = LanguageService(languages, BookService(BookRepositoryImpl(provider), languages), settings)

    private fun dictionary(url: String, order: Int, active: Boolean = true) =
        LanguageDictionary(useFor = DictionaryUse.TERMS, type = DictionaryType.POPUP, url = url, isActive = active, sortOrder = order)

    private fun preferredHosts(id: Long) = runBlocking { languages.getById(id)!! }.dictionaries
        .filter { it.useFor == DictionaryUse.TERMS && it.isActive }.sortedBy { it.sortOrder }
        .map { it.url.substringAfter("://").substringBefore("/") }

    @Test
    fun draggingAPreferredDictionaryChangesItsPriority() = runDesktopComposeUiTest(width = 1200, height = 1000) {
        val id = runBlocking {
            languages.save(
                LanguageDefinitions.all.first { it.name == "Portuguese" }.language.copy(
                    dictionaries = listOf(
                        dictionary("https://one.test/###", 1),
                        dictionary("https://two.test/###", 2),
                        dictionary("https://off.test/###", 3, active = false),
                        dictionary("https://three.test/###", 4),
                        LanguageDictionary(useFor = DictionaryUse.SENTENCES, type = DictionaryType.POPUP, url = "https://sentences.test/###", isActive = true, sortOrder = 1),
                    ),
                ),
            )
        }
        val vm = ManageDictionariesViewModel(id, languages, service, settings)
        setContent { IdiomikoTheme { ManageDictionariesScreen(id, onNavigate = {}, onBack = {}, viewModel = vm) } }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("drag-dictionary", useUnmergedTree = true).fetchSemanticsNodes().size == 3 }
        assertEquals(listOf("one.test", "two.test", "three.test"), preferredHosts(id))
        System.getenv("DICTIONARIES_SCREENSHOT")?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }

        // The third is dragged up past the other two.
        onAllNodesWithTag("drag-dictionary", useUnmergedTree = true)[2].performTouchInput {
            down(center)
            repeat(30) { moveBy(Offset(0f, -10f), delayMillis = 16) }
            up()
        }
        waitUntil(timeoutMillis = 5_000) { preferredHosts(id) == listOf("three.test", "one.test", "two.test") }
        // The one switched off keeps its place among the others.
        assertEquals(3, runBlocking { languages.getById(id)!! }.dictionaries.single { "off.test" in it.url }.sortOrder)
        assertEquals(1, onAllNodesWithText("All resources").fetchSemanticsNodes().size)
    }
}
