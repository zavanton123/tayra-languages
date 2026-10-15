package com.tayra.languages.feature.terms.form

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionarySense
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.dictionary.PackStatus
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class WordFinderDialogTest {

    private val pack = DictionaryPack(DictionaryId("en", "ru", glossary = true), "English → Russian", "https://example.org/en-ru-gloss.sqlite.gzip", 4_000_000)
    private val free = DictionaryLookup(
        listOf(
            DictionaryEntry("free", "adj", null, listOf(DictionarySense(listOf("свободный", "вольный"), listOf("not imprisoned")), DictionarySense(listOf("свободный", "незанятый"), listOf("not in use")))),
            DictionaryEntry("free", "adv", null, listOf(DictionarySense(listOf("бесплатно", "даром"), listOf("without needing to pay")))),
        ),
        emptyList(),
    )

    @Test
    fun aNativeWordGivesTheLearnedLanguagesWordsToPick() = runDesktopComposeUiTest(width = 700, height = 700) {
        val picked = mutableListOf<String>()
        setContent {
            TayraTheme {
                WordFinderDialog(
                    PackStatus(pack, PackState.Installed(4_000_000)), "English", "Russian",
                    onSearch = { if (it.trim() == "free") free else DictionaryLookup.EMPTY },
                    onDownload = {}, onPick = { picked += it }, onDismiss = {},
                )
            }
        }
        onNodeWithTag("word-finder-query").performTextInput("free")
        waitUntil(timeoutMillis = 3_000) { runCatching { onNodeWithTag("found-даром").assertExists() }.isSuccess }
        onNodeWithText("not in use").assertExists()
        System.getenv("WORD_FINDER_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot()).let { roots -> roots[roots.fetchSemanticsNodes().size - 1] }.captureToImage().toAwtImage(), "png", File(it)) }
        onNodeWithTag("found-незанятый").performClick()
        assertEquals(listOf("незанятый"), picked)
    }

    @Test
    fun aMissingFinderOffersItsDownload() = runDesktopComposeUiTest(width = 700, height = 500) {
        var downloads = 0
        setContent {
            TayraTheme {
                WordFinderDialog(PackStatus(pack, PackState.NotInstalled), "English", "Russian", onSearch = { DictionaryLookup.EMPTY }, onDownload = { downloads++ }, onPick = {}, onDismiss = {})
            }
        }
        onNodeWithTag("word-finder-download").performClick()
        assertEquals(1, downloads)
    }
}
