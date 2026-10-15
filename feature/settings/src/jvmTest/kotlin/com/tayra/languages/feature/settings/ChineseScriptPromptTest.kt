package com.tayra.languages.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.language.LanguageDefinitions
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.ChineseScriptService
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ChineseScriptPromptTest {

    private class Languages(language: Language) : LanguageRepository {
        val all = MutableStateFlow(listOf(language))
        override fun observeAll(): Flow<List<Language>> = all
        override suspend fun getAll() = all.value
        override suspend fun getById(id: Long) = all.value.firstOrNull { it.id == id }
        override suspend fun findByName(name: String) = all.value.firstOrNull { it.name == name }
        override suspend fun save(language: Language): Long {
            all.value = all.value.map { if (it.id == language.id) language else it }
            return language.id
        }
        override suspend fun delete(id: Long) {}
    }

    private val languages = Languages(LanguageDefinitions.all.first { it.name == "Mandarin Chinese" }.language.copy(id = 7))

    @AfterTest
    fun stop() = stopKoin()

    @Test
    fun theChosenCharactersBecomeTheLanguagesScript() = runDesktopComposeUiTest(width = 700, height = 560) {
        startKoin { modules(module { single { ChineseScriptService(languages, tables = null) } }) }
        var closed = 0
        setContent { TayraTheme { ChineseScriptPrompt(7L, onClosed = { closed++ }) } }
        onNodeWithText("Which characters do you learn?").assertExists()
        System.getenv("SCRIPT_PROMPT_SCREENSHOT")?.let { path ->
            val roots = onAllNodes(isRoot())
            ImageIO.write(roots[roots.fetchSemanticsNodes().size - 1].captureToImage().toAwtImage(), "png", File(path))
        }
        onNodeWithTag("script-traditional").performClick()
        waitUntil(timeoutMillis = 3_000) { closed == 1 }
        assertEquals(ChineseScript.TRADITIONAL, languages.all.value.single().chineseScript)
    }

    @Test
    fun decidingLaterLeavesTheTextsAsWritten() = runDesktopComposeUiTest(width = 700, height = 560) {
        startKoin { modules(module { single { ChineseScriptService(languages, tables = null) } }) }
        var closed = 0
        setContent { TayraTheme { ChineseScriptPrompt(7L, onClosed = { closed++ }) } }
        onNodeWithText("Decide later").performClick()
        assertEquals(1, closed)
        assertEquals(null, languages.all.value.single().chineseScript)
    }
}
