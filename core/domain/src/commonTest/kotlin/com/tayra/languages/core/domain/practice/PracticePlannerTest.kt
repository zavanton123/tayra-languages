package com.tayra.languages.core.domain.practice

import com.tayra.languages.core.domain.flashcards.Cloze
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PracticePlannerTest {
    private fun word(text: String, translation: String?, sentence: String) = PracticeWord(text, translation, sentence, Cloze.range(sentence, text)!!)

    private val words = listOf(
        word("comprar", "to buy", "Quando precisamos comprar alguma coisa, passamos no supermercado."),
        word("moro", "I live", "Eu moro no Brasil."),
        word("semana", null, "Durante a semana, tenho uma rotina bastante organizada."),
    )

    /** Each type counts the words it can be asked of and says what the others lack; the question count is what the practice will ask. */
    @Test
    fun eachTypeCountsTheWordsItCanAskAndWhy() {
        val translations = mapOf("Eu moro no Brasil." to "I live in Brazil.")
        val setup = PracticeSetup(types = ExerciseType.entries.toSet(), length = null)
        val preview = PracticePlanner.preview(words, setup, meanings = listOf("house", "slowly", "the day"), translations = translations)
        assertEquals(3, preview.words)
        assertEquals(3, preview.of(ExerciseType.GAP).words)
        assertNull(preview.of(ExerciseType.GAP).need)
        assertEquals(2, preview.of(ExerciseType.MEANING).words)
        assertEquals(PracticeNeed.WORD_MEANING, preview.of(ExerciseType.MEANING).need)
        assertEquals(2, preview.of(ExerciseType.HEAR_SENTENCE).words)
        assertEquals(PracticeNeed.SENTENCE_TRANSLATION, preview.of(ExerciseType.BUILD).need)
        assertEquals(PracticeBuilder.build(words, emptyList(), setup = setup, meanings = listOf("house", "slowly", "the day"), translations = translations).size, preview.questions)
    }

    /** Without enough meanings to choose from, the meaning question waits for more saved words. */
    @Test
    fun tooFewMeaningsAreSaid() {
        val preview = PracticePlanner.preview(words.take(2), PracticeSetup(types = setOf(ExerciseType.MEANING)))
        assertEquals(0, preview.of(ExerciseType.MEANING).words)
        assertEquals(PracticeNeed.OTHER_MEANINGS, preview.of(ExerciseType.MEANING).need)
        assertEquals(0, preview.questions)
    }

    /** The learner's own presets go into the settings as text and come back the same; the ready-made ones are told by their setup. */
    @Test
    fun presetsAreKeptAndKnown() {
        val own = listOf(SavedPreset("Evening", PracticeSetup(types = setOf(ExerciseType.DICTATION), length = 20)), SavedPreset("Bus", PracticePreset.QUICK_REVIEW.setup))
        assertEquals(own, SavedPreset.decode(SavedPreset.encode(own)))
        assertEquals(emptyList(), SavedPreset.decode(""))
        assertEquals(PracticePreset.WRITING, PracticePreset.of(PracticePreset.WRITING.setup))
        assertNull(PracticePreset.of(PracticeSetup(types = setOf(ExerciseType.GAP))))
        PracticePreset.entries.forEach { assertTrue(it.setup.rotation.isNotEmpty(), "$it asks something") }
    }
}
