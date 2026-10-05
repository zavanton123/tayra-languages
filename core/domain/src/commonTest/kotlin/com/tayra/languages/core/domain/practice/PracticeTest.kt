package com.tayra.languages.core.domain.practice

import com.tayra.languages.core.domain.flashcards.Cloze
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PracticeTest {
    private fun word(text: String, translation: String?, sentence: String) = PracticeWord(text, translation, sentence, Cloze.range(sentence, text)!!)

    private val long = "Quando precisamos comprar alguma coisa, passamos no supermercado perto de casa."
    private val words = listOf(
        word("comprar", "to buy", long),
        word("moro", "I live", "Eu moro no Brasil."),
        word("semana", "week", "Durante a semana, tenho uma rotina bastante organizada e muito cheia."),
        word("rotina", "routine", "Durante a semana, tenho uma rotina bastante organizada e muito cheia."),
        word("família", "family", "Eu moro no Brasil com a minha família e os meus dois irmãos mais novos."),
    )
    private val pageWords = listOf("Eu", "no", "Brasil", "com", "a", "minha", "durante", "tenho", "uma", "bastante", "organizada")

    @Test
    fun everyKindComesUpAndEveryWordIsAsked() {
        val exercises = PracticeBuilder.build(words, pageWords, random = Random(1))
        assertEquals(10, exercises.size)
        assertEquals(words.map { it.text }.toSet(), exercises.map { it.word }.toSet())
        assertEquals(setOf(ExerciseKind.GAP_CHOICE, ExerciseKind.GAP_TYPED, ExerciseKind.HEAR_CHOOSE, ExerciseKind.DICTATION), exercises.map { it.kind }.toSet())
        // A word's two questions are of different kinds and five questions apart.
        exercises.groupBy { it.word }.values.forEach { asked ->
            assertEquals(2, asked.size)
            assertTrue(asked[0].kind != asked[1].kind)
        }
        exercises.take(5).zip(exercises.drop(5)).forEach { (first, second) -> assertEquals(first.word, second.word) }
    }

    @Test
    fun choicesHoldTheAnswerOnceAmongFour() {
        val exercises = PracticeBuilder.build(words, pageWords, random = Random(2))
        exercises.filter { it.isChoice }.forEach { e ->
            assertEquals(4, e.options.size, e.toString())
            assertEquals(4, e.options.map { it.lowercase() }.toSet().size)
            assertEquals(1, e.options.count { it == e.answer })
        }
    }

    @Test
    fun listeningAsksForTheMeaningAndTypingForTheWordOrAShortSentence() {
        val exercises = PracticeBuilder.build(words, pageWords, random = Random(3))
        exercises.filter { it.kind == ExerciseKind.HEAR_CHOOSE }.forEach { e ->
            assertEquals(e.word, e.spoken)
            assertEquals(e.wordTranslation, e.answer, "the choices are meanings when every word has one")
        }
        exercises.filter { it.kind == ExerciseKind.DICTATION }.forEach { e ->
            assertEquals(e.sentence, e.spoken)
            if (e.sentence == "Eu moro no Brasil.") {
                assertTrue(e.wholeSentence)
                assertEquals(e.sentence, e.answer)
            } else {
                assertEquals(e.word, e.answer)
            }
        }
        exercises.filter { it.kind == ExerciseKind.GAP_TYPED }.forEach { assertEquals(it.word, it.answer) }
    }

    @Test
    fun withoutMeaningsListeningAsksForTheWordHeard() {
        val bare = words.map { it.copy(translation = null) }
        val heard = PracticeBuilder.build(bare, pageWords, random = Random(4)).filter { it.kind == ExerciseKind.HEAR_CHOOSE }
        assertTrue(heard.isNotEmpty())
        heard.forEach { assertEquals(it.word, it.answer) }
    }

    @Test
    fun aWordWithASentenceFromElsewhereIsAskedInIt() {
        val example = "Vou comprar pão amanhã."
        val examples = mapOf("comprar" to PracticeExample(example, Cloze.range(example, "comprar")!!, "I will buy bread tomorrow."))
        val exercises = PracticeBuilder.build(words, pageWords, examples, Random(5))
        assertEquals(10, exercises.size)
        val fresh = exercises.single { it.kind == ExerciseKind.NEW_CONTEXT }
        assertEquals(exercises.last(), fresh, "the new context comes last, well after the word's own sentence")
        assertEquals(example, fresh.sentence)
        assertEquals("comprar", fresh.answer)
        assertEquals("I will buy bread tomorrow.", fresh.sentenceTranslation)
        assertTrue(fresh.answer in fresh.options)
    }

    @Test
    fun oneWordStillMakesAShortPractice() {
        val exercises = PracticeBuilder.build(words.take(1), pageWords, random = Random(6))
        assertEquals(4, exercises.size)
        assertEquals(4, exercises.map { it.kind }.toSet().size)
        assertEquals(emptyList(), PracticeBuilder.build(emptyList(), pageWords))
    }

    @Test
    fun typedAnswersForgiveCasePunctuationAndNearlyAccents() {
        assertEquals(TypedResult.CORRECT, PracticeChecker.check("família", " Família "))
        assertEquals(TypedResult.ALMOST, PracticeChecker.check("família", "familia"))
        assertEquals(TypedResult.ALMOST, PracticeChecker.check("irmãos", "irmaos"))
        assertEquals(TypedResult.WRONG, PracticeChecker.check("família", "famílias"))
        assertEquals(TypedResult.WRONG, PracticeChecker.check("família", "  "))
        assertEquals(TypedResult.CORRECT, PracticeChecker.check("Eu moro no Brasil.", "eu moro no brasil"))
        assertEquals(TypedResult.CORRECT, PracticeChecker.check("d'água", "d’água"))
        assertEquals(TypedResult.WRONG, PracticeChecker.check("Eu moro no Brasil.", "eu moro em Brasil"))
    }
}
