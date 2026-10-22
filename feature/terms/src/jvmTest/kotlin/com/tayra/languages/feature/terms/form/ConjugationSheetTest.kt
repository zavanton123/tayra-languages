package com.tayra.languages.feature.terms.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.dictionary.InflectionTables
import com.tayra.languages.core.domain.dictionary.WiktionaryAudio
import com.tayra.languages.core.domain.dictionary.WiktionaryEntry
import com.tayra.languages.core.domain.dictionary.WiktionaryForm
import com.tayra.languages.core.domain.dictionary.WiktionaryPage
import com.tayra.languages.core.domain.dictionary.WiktionarySense
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ConjugationSheetTest {

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

    /** The Dictionary card's row opens the lemma's table; a verb's says conjugations. */
    @Test
    fun theRowNamesTheLemmaAndOpens() = runDesktopComposeUiTest(width = 420, height = 300) {
        var opened = 0
        setContent { Framed { ConjugationsRow("comer", verb = true) { opened++ } } }
        onNodeWithText("View conjugations").assertExists()
        onNodeWithText("comer").assertExists()
        onNodeWithTag("view-conjugations").performClick()
        assertEquals(1, opened)
    }

    /** The sheet opens on the pane's form, by mood and tense, with the pronouns; a form is read aloud on a click. */
    @Test
    fun theSheetOpensOnTheClickedTense() = runDesktopComposeUiTest(width = 900, height = 900) {
        val spoken = mutableListOf<String>()
        var shown by mutableStateOf(true)
        setContent { Framed { if (shown) ConjugationSheet(conjugation, "comer", "comi", "Portuguese", "pt", comer, onSpeak = { spoken += it }) { shown = false } } }
        onNodeWithTag("conjugation-sheet").assertExists()
        // "comi" is a preterite: the sheet opens there, the form tinted, beside its pronoun.
        onNodeWithTag("dialog-form-clicked", useUnmergedTree = true).assertTextEquals("comi")
        onNodeWithText("eu").assertExists()
        onNodeWithTag("wiktionary-credit").assertTextEquals("Wiktionary contributors · CC BY-SA 4.0")
        System.getenv("CONJUGATION_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot()).fetchSemanticsNodes().indices.last().let { last -> onAllNodes(isRoot())[last] }.captureToImage().toAwtImage(), "png", File(it)) }
        onNodeWithTag("tense-0").performClick()
        onNodeWithText("comemos").performClick()
        assertEquals(listOf("comemos"), spoken)
        onNodeWithText("Non-finite").performClick()
        onNodeWithText("comendo").assertExists()
        onNodeWithText("Done").performClick()
        waitUntil(timeoutMillis = 5_000) { !shown }
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
