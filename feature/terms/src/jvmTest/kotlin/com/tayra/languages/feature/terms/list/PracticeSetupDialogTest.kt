package com.tayra.languages.feature.terms.list

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.flashcards.Cloze
import com.tayra.languages.core.domain.practice.ExerciseType
import com.tayra.languages.core.domain.practice.PracticeContext
import com.tayra.languages.core.domain.practice.PracticeExample
import com.tayra.languages.core.domain.practice.PracticePreset
import com.tayra.languages.core.domain.practice.PracticeResponse
import com.tayra.languages.core.domain.practice.PracticeSetup
import com.tayra.languages.core.domain.practice.PracticeSkill
import com.tayra.languages.core.domain.practice.PracticeWord
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import com.tayra.languages.core.domain.practice.SavedPreset
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The dialog that sets up a practice: what each type can ask, presets, filters, and the setup it starts with. */
@OptIn(ExperimentalTestApi::class)
class PracticeSetupDialogTest {

    private fun word(text: String, translation: String?, sentence: String) = PracticeWord(text, translation, sentence, Cloze.range(sentence, text)!!)

    private val state = PracticeSetupState(
        loading = false,
        words = listOf(
            word("comprar", "to buy", "Quando precisamos comprar alguma coisa, passamos no supermercado."),
            word("moro", "I live", "Eu moro no Brasil."),
            word("semana", null, "Durante a semana, tenho uma rotina bastante organizada."),
        ),
        meanings = listOf("house", "slowly", "the day", "to go"),
        translations = mapOf("Eu moro no Brasil." to "I live in Brazil."),
        examples = mapOf("moro" to listOf(PracticeExample("Eu moro aqui.", 3..6, "I live here."))),
    )

    private var started: PracticeSetup? = null
    private val saved = mutableListOf<SavedPreset>()
    private val removed = mutableListOf<String>()

    private fun ComposeUiTest.show(initial: PracticeSetup = PracticePreset.DEFAULT.setup, own: List<SavedPreset> = emptyList(), state: PracticeSetupState = this@PracticeSetupDialogTest.state) {
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    PracticeSetupContent(state, initial, onStart = { started = it }, onDismiss = {}, ownPresets = own, onSavePreset = { name, setup -> saved += SavedPreset(name, setup) }, onRemovePreset = { removed += it })
                }
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.save(env: String) {
        System.getenv(env)?.let { ImageIO.write(onAllNodes(isRoot()).fetchSemanticsNodes().let { onAllNodes(isRoot())[it.lastIndex] }.captureToImage().toAwtImage(), "png", File(it)) }
    }

    private fun ComposeUiTest.text(tag: String) =
        onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString { it.text }

    /** Each type says how many of the words it can ask about, and what the others lack. */
    @Test
    fun eachTypeSaysWhatItCanAsk() = runDesktopComposeUiTest(width = 1300, height = 1000) {
        show()
        save("PRACTICE_SETUP_SCREENSHOT")
        assertEquals("For all 3 words", text("practice-availability-GAP"))
        assertEquals("For 2 of 3 words · Needs the word's translation", text("practice-availability-MEANING"))
        assertEquals("For 0 of 3 words · Needs other forms of the word", text("practice-availability-FORM"))
        assertEquals("Sentences kept for 1 of 3 words", text("practice-examples"))
        assertTrue(text("practice-summary").endsWith("min"), text("practice-summary"))
    }

    /** A skill's tick turns its types off and on together; a type's own tick turns it alone; the setup started with is what was ticked. */
    @Test
    fun skillsAndTypesAreTickedAndStartTheSetup() = runDesktopComposeUiTest(width = 1300, height = 1000) {
        show()
        onNodeWithTag("practice-skill-LISTENING").performScrollTo().performClick()
        onNodeWithTag("practice-type-MEANING").performScrollTo().performClick()
        onNodeWithTag("practice-response-TYPE").performClick()
        onNodeWithTag("practice-length-all").performClick()
        // Changed from the preset: the setup is a custom one now.
        assertEquals(1, onAllNodesWithText("Custom").fetchSemanticsNodes().size)
        onNodeWithTag("start-practice").performClick()
        assertEquals(
            PracticeSetup(
                types = ExerciseType.entries.filter { it.skill != PracticeSkill.LISTENING && it != ExerciseType.MEANING }.toSet(),
                responses = setOf(PracticeResponse.CHOOSE),
                contexts = PracticeContext.entries.toSet(),
                length = null,
                adaptive = true,
            ),
            started,
        )
        onNodeWithTag("practice-skill-LISTENING").performScrollTo().performClick()
        onNodeWithTag("practice-preset-QUICK_REVIEW").performScrollTo().performClick()
        onNodeWithTag("start-practice").performClick()
        assertEquals(PracticePreset.QUICK_REVIEW.setup.copy(length = null), started, "a preset keeps the length chosen")
    }

    /** With nothing that fits ticked, the dialog says so and does not start. */
    @Test
    fun nothingToAskDoesNotStart() = runDesktopComposeUiTest(width = 1300, height = 1000) {
        show(PracticeSetup(types = setOf(ExerciseType.MEANING), responses = setOf(PracticeResponse.TYPE), contexts = setOf(PracticeContext.ORIGINAL)))
        assertEquals("Not answered the ways ticked", text("practice-availability-MEANING"))
        assertEquals("None of the ticked questions fits these words", text("practice-summary"))
        onNodeWithTag("start-practice").assertIsNotEnabled()
        onNodeWithTag("practice-response-CHOOSE").performClick()
        onNodeWithTag("start-practice").assertIsEnabled()
    }

    /** On a phone the choices scroll in one column, and the count beside the buttons wraps so Start practice stays whole. */
    @Test
    fun aPhoneScrollsTheChoicesAndKeepsTheStartButtonWhole() = runDesktopComposeUiTest(width = 400, height = 850) {
        show()
        save("PRACTICE_SETUP_PHONE_SCREENSHOT")
        onNodeWithTag("practice-length-all").performScrollTo().performClick()
        val start = onNodeWithTag("start-practice").fetchSemanticsNode().size
        assertTrue(start.width > start.height * 2, "Start practice is ${start.width} wide and ${start.height} high")
        onNodeWithTag("start-practice").performClick()
        assertEquals(null, started?.length)
    }

    /** The setup can be saved as a preset under a name; an own preset is picked whole, length and all, and can be removed. */
    @Test
    fun ownPresetsAreSavedPickedAndRemoved() = runDesktopComposeUiTest(width = 1300, height = 1000) {
        val evening = SavedPreset("Evening", PracticeSetup(types = setOf(ExerciseType.DICTATION), length = 20))
        show(own = listOf(evening))
        onNodeWithTag("practice-own-preset-Evening").performClick()
        onNodeWithTag("start-practice").performClick()
        assertEquals(evening.setup, started)
        onNodeWithTag("save-practice-preset").performClick()
        onNode(hasSetTextAction()).performTextInput("Morning")
        onNodeWithText("Save").performClick()
        assertEquals(listOf(SavedPreset("Morning", evening.setup)), saved)
        onNodeWithContentDescription("Remove the preset Evening").performClick()
        assertEquals(listOf("Evening"), removed)
    }

    /** Saying answers is offered only where the words' language can be heard; elsewhere a setup to say them is typed. */
    @Test
    fun speakingIsOfferedOnlyWhereItCanBeHeard() = runDesktopComposeUiTest(width = 1300, height = 1000) {
        show(PracticePreset.SPEAKING.setup)
        listOf("practice-response-SPEAK", "practice-preset-SPEAKING", "practice-type-SHADOW").forEach { onAllNodesWithTag(it).assertCountEquals(0) }
        onNodeWithTag("start-practice").performClick()
        assertEquals(PracticePreset.SPEAKING.setup.withoutSpeaking(), started)
    }

    @Test
    fun speakingIsPickedWhereItCanBeHeard() = runDesktopComposeUiTest(width = 1300, height = 1000) {
        show(state = state.copy(speaking = true))
        onNodeWithTag("practice-type-SHADOW").performScrollTo().assertExists()
        onNodeWithTag("practice-preset-SPEAKING").performScrollTo().performClick()
        onNodeWithTag("start-practice").performClick()
        assertEquals(PracticePreset.SPEAKING.setup.copy(length = PracticePreset.DEFAULT.setup.length, adaptive = true), started)
    }

    /** Only lengths shorter than all the questions there are are offered; a longer one kept from before shows as All. */
    @Test
    fun lengthsLongerThanThePracticeAreLeftOut() = runDesktopComposeUiTest(width = 1300, height = 1000) {
        show(PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.CHOOSE), contexts = setOf(PracticeContext.ORIGINAL), length = 20))
        // Three words, one question each.
        assertEquals("3 questions · about 1 min", text("practice-summary"))
        listOf("practice-length-5", "practice-length-10", "practice-length-20").forEach { onAllNodesWithTag(it).assertCountEquals(0) }
        onNodeWithTag("practice-length-all").assertExists()
        onNodeWithTag("start-practice").performClick()
        assertEquals(20, started?.length, "the length chosen is kept for a practice with more")
    }
}
