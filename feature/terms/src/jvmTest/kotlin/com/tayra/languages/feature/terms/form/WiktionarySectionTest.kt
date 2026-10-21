package com.tayra.languages.feature.terms.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.dictionary.InflectionTables
import com.tayra.languages.core.domain.dictionary.WiktionaryAudio
import com.tayra.languages.core.domain.dictionary.WiktionaryEntry
import com.tayra.languages.core.domain.dictionary.WiktionaryForm
import com.tayra.languages.core.domain.dictionary.WiktionaryLemma
import com.tayra.languages.core.domain.dictionary.WiktionaryPage
import com.tayra.languages.core.domain.dictionary.WiktionaryPronunciation
import com.tayra.languages.core.domain.dictionary.WiktionarySense
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class WiktionarySectionTest {

    private val comem = WiktionaryPage(
        "comem", "en", "pt",
        listOf(
            WiktionaryEntry(
                "verb",
                listOf(WiktionarySense(listOf("third-person plural present indicative of comer"), formOf = listOf(WiktionaryLemma("comer", "comer")))),
                listOf(WiktionaryPronunciation("/ˈkõ.mẽj̃/", listOf("Brazil")), WiktionaryPronunciation("/ˈkɔ.mɐ̃j̃/", listOf("Portugal"))),
            ),
        ),
    )

    private val comer = WiktionaryPage(
        "comer", "en", "pt",
        listOf(WiktionaryEntry("verb", listOf(WiktionarySense(listOf("to eat"), synonyms = listOf("alimentar"))))),
    )

    private val conjugation = InflectionTables.table(
        "verb",
        listOf("como" to "first-person", "comes" to "second-person", "come" to "third-person").flatMap { (form, person) ->
            listOf(WiktionaryForm(form, listOf(person, "singular", "present", "indicative"), "conjugation"))
        } + listOf("comemos" to "first-person", "comeis" to "second-person", "comem" to "third-person").map { (form, person) ->
            WiktionaryForm(form, listOf(person, "plural", "present", "indicative"), "conjugation")
        } + listOf("comi" to "first-person", "comeste" to "second-person").map { (form, person) ->
            WiktionaryForm(form, listOf(person, "singular", "preterite", "indicative"), "conjugation")
        } + WiktionaryForm("comendo", listOf("gerund"), "conjugation"),
    )

    @Composable
    private fun Framed(content: @Composable () -> Unit) {
        TayraTheme { Surface(color = MaterialTheme.colorScheme.surface) { content() } }
    }

    @Test
    fun aFormNamesItsLemmaWhichOpens() = runDesktopComposeUiTest(width = 420, height = 400) {
        val opened = mutableListOf<String>()
        setContent { Framed { WiktionarySection(WiktionaryUi(comem, tableWord = "comer"), "comem", "Portuguese", { opened += it }, {}, {}) } }
        onNodeWithText("third-person plural present indicative of comer").assertExists()
        onNodeWithTag("form-of-comer").performClick()
        assertEquals(listOf("comer"), opened)
        onNodeWithText("/ˈkɔ.mɐ̃j̃/").assertExists()
        onNodeWithText("Portugal").assertExists()
        onNodeWithTag("wiktionary-credit").assertTextEquals("Wiktionary contributors · CC BY-SA 4.0")
        // The lemma's table is closed until asked for.
        onAllNodesWithTag("cell").assertCountEquals(0)
    }

    @Test
    fun theTableOfTheLemmaTintsTheClickedForm() = runDesktopComposeUiTest(width = 520, height = 600) {
        var ui by mutableStateOf(WiktionaryUi(comem, tableWord = "comer"))
        var toggled = 0
        setContent { Framed { WiktionarySection(ui, "comem", "Portuguese", {}, { toggled++ }, {}) } }
        onNodeWithTag("wiktionary-table").performClick()
        assertEquals(1, toggled)
        ui = ui.copy(tableOpen = true, tablesLoading = true)
        onNodeWithText("Looking up the table...").assertExists()
        ui = ui.copy(tablesLoading = false, tables = listOf(conjugation))
        onNodeWithTag("cell-clicked").assertTextEquals("comem")
        onNodeWithText("3rd pl.").assertExists()
        onNodeWithText("present indicative").assertExists()
        onNodeWithText("comendo").assertExists()
        System.getenv("WIKTIONARY_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
        ui = ui.copy(tables = emptyList())
        onNodeWithText("Wiktionary has no table for “comer”.").assertExists()
    }

    @Test
    fun synonymsAndExpressionsOpenInThePane() = runDesktopComposeUiTest(width = 420, height = 500) {
        val opened = mutableListOf<String>()
        var ui by mutableStateOf(WiktionaryUi(comer, tableWord = "comer", tables = listOf(conjugation)))
        var toggled = 0
        setContent { Framed { WiktionarySection(ui, "comer", "Portuguese", { opened += it }, {}, { toggled++ }) } }
        onNodeWithTag("synonym-alimentar").performClick()
        onNodeWithTag("wiktionary-expressions").performClick()
        assertEquals(1, toggled)
        ui = ui.copy(expressionsOpen = true, expressions = listOf("comer com os olhos", "comer o chapéu"))
        onNodeWithTag("expression-comer o chapéu").performClick()
        assertEquals(listOf("alimentar", "comer o chapéu"), opened)
        ui = ui.copy(expressions = emptyList())
        onNodeWithText("No expressions found.").assertExists()
    }

    @Test
    fun theEntrysRecordingsJoinCommonsOnesOnce() {
        val page = comer.copy(
            entries = listOf(
                comer.entries.single().copy(
                    audio = listOf(
                        WiktionaryAudio("LL-Q5146 (por)-Santamarcanda-comer.wav", "https://upload.test/a.mp3", null, listOf("Portugal")),
                        WiktionaryAudio("Pt-comer.ogg", null, "https://upload.test/b.ogg"),
                    ),
                ),
            ),
        )
        val fromEntry = recordingsOf(page)
        assertEquals(listOf("Santamarcanda (Portugal)", "Pt-comer.ogg"), fromEntry.map { it.speaker })
        assertEquals("https://commons.wikimedia.org/wiki/File:LL-Q5146_(por)-Santamarcanda-comer.wav", fromEntry.first().pageUrl)
        val commons = fromEntry.first().copy(url = "https://upload.test/other.mp3", speaker = "Santamarcanda", source = "Lingua Libre")
        assertEquals(listOf("Santamarcanda", "Pt-comer.ogg"), withRecordings(listOf(commons), fromEntry).map { it.speaker })
    }
}
