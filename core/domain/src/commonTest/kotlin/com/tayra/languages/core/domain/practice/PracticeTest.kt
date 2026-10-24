package com.tayra.languages.core.domain.practice

import com.tayra.languages.core.domain.flashcards.Cloze
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PracticeTest {
    private val Exercise.kind get() = type to response
    private val gapChoice = ExerciseType.GAP to PracticeResponse.CHOOSE
    private val gapTyped = ExerciseType.GAP to PracticeResponse.TYPE
    private val hearChoice = ExerciseType.HEAR_WORD to PracticeResponse.CHOOSE
    private val dictation = ExerciseType.DICTATION to PracticeResponse.TYPE
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
        assertEquals(setOf(gapChoice, gapTyped, hearChoice, dictation), exercises.map { it.kind }.toSet())
        // A word's two questions are of different kinds and five questions apart.
        exercises.groupBy { it.word }.values.forEach { asked ->
            assertEquals(2, asked.size)
            assertTrue(asked[0].kind != asked[1].kind)
        }
        exercises.take(5).zip(exercises.drop(5)).forEach { (first, second) -> assertEquals(first.word, second.word) }
    }

    /** Only the kinds asked for come up: choices only, typing only, or one kind alone; a sentence from elsewhere comes as a gap, chosen or typed. */
    @Test
    fun onlyTheKindsAskedForComeUp() {
        val examples = mapOf("comprar" to PracticeExample("Vou comprar pão.", Cloze.range("Vou comprar pão.", "comprar")!!, "I will buy bread."))
        val choice = PracticeBuilder.build(words, pageWords, examples, Random(1), PracticeSetup(types = setOf(ExerciseType.GAP, ExerciseType.HEAR_WORD), responses = setOf(PracticeResponse.CHOOSE)))
        assertTrue(choice.isNotEmpty() && choice.all { it.isChoice }, "choice: ${choice.map { it.kind }}")
        assertTrue(choice.any { it.context == PracticeContext.EXAMPLE }, "a sentence from elsewhere is a choice too")
        val typing = PracticeBuilder.build(words, pageWords, examples, Random(1), PracticeSetup(types = setOf(ExerciseType.GAP, ExerciseType.DICTATION), responses = setOf(PracticeResponse.TYPE)))
        assertTrue(typing.isNotEmpty() && typing.none { it.isChoice }, "typing: ${typing.map { it.kind }}")
        assertTrue(typing.any { it.context == PracticeContext.EXAMPLE }, "and typed where only typing is asked")
        val dictations = PracticeBuilder.build(words, pageWords, examples, Random(1), PracticeSetup(types = setOf(ExerciseType.DICTATION)))
        assertTrue(dictations.isNotEmpty() && dictations.all { it.kind == dictation }, "dictation: ${dictations.map { it.kind }}")
        assertEquals(emptyList(), PracticeBuilder.build(words, pageWords, examples, Random(1), PracticeSetup(types = emptySet())))
        assertEquals(emptyList(), PracticeBuilder.build(words, pageWords, examples, Random(1), PracticeSetup(types = setOf(ExerciseType.DICTATION), responses = setOf(PracticeResponse.CHOOSE))), "dictation is never a choice")
    }

    /** Without a length every question comes for every word; with sentences from elsewhere only, only the words that have one are asked. */
    @Test
    fun theLengthAndTheSentencesAskedForAreKept() {
        assertEquals(words.size * 4, PracticeBuilder.build(words, pageWords, random = Random(1), setup = PracticeSetup(length = null)).size)
        assertEquals(5, PracticeBuilder.build(words, pageWords, random = Random(1), setup = PracticeSetup(length = 5)).size)
        val examples = mapOf("comprar" to PracticeExample("Vou comprar pão.", Cloze.range("Vou comprar pão.", "comprar")!!, "I will buy bread."))
        val elsewhere = PracticeBuilder.build(words, pageWords, examples, Random(1), PracticeSetup(contexts = setOf(PracticeContext.EXAMPLE)))
        assertEquals(listOf("Vou comprar pão."), elsewhere.map { it.sentence })
    }

    /** A setup goes into a route as text and comes back the same; the kinds earlier versions named still give their questions. */
    @Test
    fun aSetupIsWrittenAndReadAsText() {
        val setup = PracticeSetup(types = setOf(ExerciseType.GAP, ExerciseType.DICTATION), responses = setOf(PracticeResponse.TYPE), contexts = setOf(PracticeContext.ORIGINAL), length = null)
        assertEquals("types=GAP,DICTATION;responses=TYPE;contexts=ORIGINAL;length=all", setup.encode())
        assertEquals(setup, PracticeSetup.decode(setup.encode()))
        assertEquals(PracticeSetup(), PracticeSetup.decode(""), "nothing said: the defaults")
        assertEquals(setOf(ExerciseType.GAP), PracticeSetup.decode("types=GAP,SOMETHING_NEW").types, "names not known are left out")
        assertEquals(PracticeSetup.DEFAULT_LENGTH, PracticeSetup.decode("length=-3").length)
        assertEquals(listOf(gapChoice, hearChoice, gapTyped, dictation), PracticeSetup.ofKinds(emptyList()).rotation)
        assertEquals(listOf(gapChoice, hearChoice), PracticeSetup.ofKinds(listOf("GAP_CHOICE", "HEAR_CHOOSE")).rotation)
        assertEquals(listOf(dictation), PracticeSetup.ofKinds(listOf("DICTATION")).rotation)
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
        exercises.filter { it.type == ExerciseType.HEAR_WORD }.forEach { e ->
            assertEquals(e.word, e.spoken)
            assertEquals(e.wordTranslation, e.answer, "the choices are meanings when every word has one")
        }
        exercises.filter { it.type == ExerciseType.DICTATION }.forEach { e ->
            assertEquals(e.sentence, e.spoken)
            if (e.sentence == "Eu moro no Brasil.") {
                assertTrue(e.wholeSentence)
                assertEquals(e.sentence, e.answer)
            } else {
                assertEquals(e.word, e.answer)
            }
        }
        exercises.filter { it.kind == gapTyped }.forEach { assertEquals(it.word, it.answer) }
    }

    @Test
    fun withoutMeaningsListeningAsksForTheWordHeard() {
        val bare = words.map { it.copy(translation = null) }
        val heard = PracticeBuilder.build(bare, pageWords, random = Random(4)).filter { it.type == ExerciseType.HEAR_WORD }
        assertTrue(heard.isNotEmpty())
        heard.forEach { assertEquals(it.word, it.answer) }
    }

    @Test
    fun aWordWithASentenceFromElsewhereIsAskedInIt() {
        val example = "Vou comprar pão amanhã."
        val examples = mapOf("comprar" to PracticeExample(example, Cloze.range(example, "comprar")!!, "I will buy bread tomorrow."))
        val exercises = PracticeBuilder.build(words, pageWords, examples, Random(5))
        assertEquals(10, exercises.size)
        val fresh = exercises.single { it.context == PracticeContext.EXAMPLE }
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
