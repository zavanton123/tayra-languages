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
        val examples = mapOf("comprar" to listOf(PracticeExample("Vou comprar pão.", Cloze.range("Vou comprar pão.", "comprar")!!, "I will buy bread.")))
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
        val examples = mapOf("comprar" to listOf(PracticeExample("Vou comprar pão.", Cloze.range("Vou comprar pão.", "comprar")!!, "I will buy bread.")))
        val elsewhere = PracticeBuilder.build(words, pageWords, examples, Random(1), PracticeSetup(contexts = setOf(PracticeContext.EXAMPLE)))
        assertEquals(listOf("Vou comprar pão."), elsewhere.map { it.sentence })
    }

    /** The meaning is chosen among four, the others from other words; one sharing a word with it is never offered, and a word without a meaning gets another question. */
    @Test
    fun theMeaningIsChosenAmongOthersThatCannotAlsoBeRight() {
        val setup = PracticeSetup(types = setOf(ExerciseType.MEANING), length = null)
        val withHome = words + word("casa", "at home", "Eu fico em casa.")
        val asked = PracticeBuilder.build(withHome, pageWords, random = Random(1), setup = setup, meanings = listOf("home", "the house", "to go", "slowly"))
        assertEquals(withHome.size, asked.size)
        asked.forEach { e ->
            assertEquals(ExerciseType.MEANING, e.type)
            assertEquals(e.wordTranslation, e.answer)
            assertEquals(4, e.options.size)
            assertEquals(1, e.options.count { it == e.answer })
        }
        assertTrue("home" !in asked.single { it.word == "casa" }.options, "home could be read as right beside at home")
        // Without a meaning the word is asked as a gap instead, where the gap is asked too.
        val bare = listOf(word("moro", null, "Eu moro no Brasil.")) + words.drop(1)
        val mixed = PracticeBuilder.build(bare, pageWords, random = Random(2), setup = PracticeSetup(types = setOf(ExerciseType.MEANING, ExerciseType.GAP), responses = setOf(PracticeResponse.CHOOSE), length = null))
        assertTrue(mixed.filter { it.word == "moro" }.all { it.type == ExerciseType.GAP }, mixed.filter { it.word == "moro" }.toString())
        assertEquals(emptyList(), PracticeBuilder.build(listOf(word("moro", null, "Eu moro no Brasil.")), pageWords, setup = setup))
    }

    /** A translation question carries its sentence's translation; a word whose sentence has none is not asked it. */
    @Test
    fun translationQuestionsComeOnlyWithATranslation() {
        val translations = mapOf("Eu moro no Brasil." to "I live in Brazil.", long to "When we need to buy something, we stop at the supermarket near home.")
        val setup = PracticeSetup(types = setOf(ExerciseType.TRANSLATION), length = null)
        val asked = PracticeBuilder.build(words, pageWords, random = Random(1), setup = setup, translations = translations)
        assertEquals(setOf("moro", "comprar"), asked.map { it.word }.toSet())
        asked.forEach { e ->
            assertEquals(translations.getValue(e.sentence), e.sentenceTranslation)
            assertEquals(e.word, e.answer)
            if (e.response == PracticeResponse.CHOOSE) assertTrue(e.answer in e.options) else assertTrue(e.options.isEmpty())
        }
        assertEquals(setOf(PracticeResponse.CHOOSE, PracticeResponse.TYPE), asked.map { it.response }.toSet())
    }

    /** Hearing a sentence, the word with the meaning given is chosen among the sentence's other words, or typed; it needs the meaning. */
    @Test
    fun aHeardSentenceAsksForTheWordWithTheMeaningAmongItsOwnWords() {
        val setup = PracticeSetup(types = setOf(ExerciseType.HEAR_SENTENCE), length = null)
        val asked = PracticeBuilder.build(words + word("casa", null, "Eu fico em casa."), pageWords, random = Random(1), setup = setup)
        assertEquals(words.map { it.text }.toSet(), asked.map { it.word }.toSet(), "not the word without a meaning")
        asked.forEach { e ->
            assertEquals(e.sentence, e.spoken)
            assertEquals(e.word, e.answer)
        }
        val chosen = asked.first { it.word == "semana" && it.response == PracticeResponse.CHOOSE }
        assertEquals(4, chosen.options.size)
        assertTrue(chosen.options.all { it in chosen.sentence }, "all from the sentence heard: ${chosen.options}")
        assertTrue(asked.any { it.response == PracticeResponse.TYPE && it.options.isEmpty() })
    }

    /** A choice of word offers one as common as it and looking like it, when there are such words. */
    @Test
    fun aChoiceOffersAWordLikeTheRightOne() {
        val alikes = words.associate { it.key to listOf(it.text.dropLast(1) + "x") }
        val setup = PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.CHOOSE), length = null)
        PracticeBuilder.build(words, pageWords, random = Random(4), setup = setup, alikes = alikes).forEach { e ->
            assertTrue(alikes.getValue(e.word.lowercase()).single() in e.options, e.options.toString())
            assertEquals(1, e.options.count { it == e.answer })
        }
    }

    /** Built from tiles, a word comes with its own letters and two that are not in it; a word too long for tiles gets another question. */
    @Test
    fun tilesHoldTheWordsLettersAndTwoMore() {
        val setup = PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.TILES, PracticeResponse.TYPE), length = null)
        val long = word("supermercados", "supermarkets", "Há dois supermercados aqui.")
        val asked = PracticeBuilder.build(words + long, pageWords, random = Random(1), setup = setup)
        asked.filter { it.response == PracticeResponse.TILES }.forEach { e ->
            assertEquals(e.answer.length + 2, e.tiles.size)
            assertEquals(e.answer.map { it.toString() }.sorted(), (e.tiles - e.tiles.filter { t -> e.answer.none { it.toString().equals(t, ignoreCase = true) } }.toSet()).sorted())
        }
        assertTrue(asked.filter { it.word == "supermercados" }.none { it.response == PracticeResponse.TILES }, "too long for tiles")
        assertTrue(asked.any { it.response == PracticeResponse.TILES })
    }

    /** The right form is chosen among other forms of the word, written as it is, with the sentence's translation as the clue; without forms or a translation it is not asked. */
    @Test
    fun theRightFormIsChosenAmongTheWordsForms() {
        val families = mapOf("comprar" to WordFamily("comprar", setOf("comprar", "compro", "compra", "compramos", "compraram")), "moro" to WordFamily("morar", setOf("moro", "morar")))
        val translations = mapOf(long to "When we need to buy something, we stop at the supermarket near home.", "Eu moro no Brasil." to "I live in Brazil.")
        val setup = PracticeSetup(types = setOf(ExerciseType.FORM), responses = setOf(PracticeResponse.CHOOSE), length = null)
        val asked = PracticeBuilder.build(words, pageWords, random = Random(1), setup = setup, translations = translations, families = families)
        val form = asked.single()
        assertEquals("comprar", form.word)
        assertEquals("comprar", form.lemma)
        assertEquals(translations.getValue(long), form.sentenceTranslation)
        assertEquals(4, form.options.size)
        assertTrue(form.options.all { it in families.getValue("comprar").forms }, form.options.toString())
        assertEquals(PracticeNeed.WORD_FORMS, PracticePlanner.preview(words, setup, translations = translations, families = families).of(ExerciseType.FORM).need)
    }

    /** A matching round comes first, on up to five words whose meanings share no word; with fewer than four such words there is none. */
    @Test
    fun aMatchingRoundComesFirstOnWordsThatCannotBeConfused() {
        val setup = PracticeSetup(types = setOf(ExerciseType.MATCHING, ExerciseType.GAP))
        val asked = PracticeBuilder.build(words + word("casa", "at home", "Eu fico em casa.") + word("lar", "home", "O meu lar."), pageWords, random = Random(1), setup = setup)
        val matching = asked.first()
        assertEquals(ExerciseType.MATCHING, matching.type)
        assertTrue(asked.drop(1).none { it.type == ExerciseType.MATCHING })
        assertEquals(10, asked.size, "it counts as one of the ten questions")
        assertEquals(5, matching.pairs.size)
        assertTrue(!(matching.pairs.any { it.second == "home" } && matching.pairs.any { it.second == "at home" }), "home and at home could be taken for each other")
        assertEquals(listOf(ExerciseType.GAP), PracticeBuilder.build(words.take(3), pageWords, random = Random(1), setup = setup).map { it.type }.distinct())
    }

    /** A sentence of three to eight words with a translation is built from its words, shuffled out of order; others are not. */
    @Test
    fun aShortTranslatedSentenceIsBuiltFromItsWords() {
        val translations = words.associate { it.sentence to "[${it.sentence}]" }
        val setup = PracticeSetup(types = setOf(ExerciseType.BUILD), responses = setOf(PracticeResponse.TILES), length = null)
        val asked = PracticeBuilder.build(words, pageWords, random = Random(1), setup = setup, translations = translations)
        assertEquals(listOf("Eu moro no Brasil."), asked.map { it.sentence }, "the others are longer than eight words")
        val built = asked.single()
        assertTrue(built.wholeSentence)
        assertEquals(built.sentence, built.answer)
        assertEquals(listOf("Brasil.", "Eu", "moro", "no"), built.tiles.sorted())
        assertTrue(built.tiles != listOf("Eu", "moro", "no", "Brasil."))
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

    /** A word with sentences from elsewhere is asked in one in its second round, after its own sentence. */
    @Test
    fun aWordWithASentenceFromElsewhereIsAskedInIt() {
        val example = "Vou comprar pão amanhã."
        val examples = mapOf("comprar" to listOf(PracticeExample(example, Cloze.range(example, "comprar")!!, "I will buy bread tomorrow.")))
        val exercises = PracticeBuilder.build(words, pageWords, examples, Random(5))
        assertEquals(10, exercises.size)
        val fresh = exercises.single { it.context == PracticeContext.EXAMPLE }
        val own = exercises.first { it.word == "comprar" }
        assertTrue(exercises.indexOf(own) < exercises.indexOf(fresh), "its own sentence first")
        assertEquals(example, fresh.sentence)
        assertEquals("comprar", fresh.word)
        assertEquals("I will buy bread tomorrow.", fresh.sentenceTranslation)
        assertEquals("comprar", fresh.practised)
    }

    /** A sentence from elsewhere may use another form of the word: that form is the answer, the word saved is no wrong choice, and each sentence is asked once. */
    @Test
    fun anotherFormOfTheWordIsAskedAsItStands() {
        val first = "Nós compramos pão ontem."
        val second = "Eles compram tudo aqui."
        val examples = mapOf("comprar" to listOf(PracticeExample(first, Cloze.range(first, "compramos")!!, "We bought bread yesterday."), PracticeExample(second, Cloze.range(second, "compram")!!, null)))
        val setup = PracticeSetup(types = setOf(ExerciseType.GAP, ExerciseType.HEAR_SENTENCE), responses = setOf(PracticeResponse.CHOOSE), contexts = setOf(PracticeContext.EXAMPLE), length = null)
        val asked = PracticeBuilder.build(words, pageWords + "comprar", examples, Random(1), setup)
        assertEquals(listOf(first, second), asked.map { it.sentence }, "each sentence once, only the word that has some")
        asked.forEach { e ->
            assertTrue(e.otherForm)
            assertEquals("comprar", e.practised)
            assertEquals(e.sentence.substring(e.range), e.answer)
            assertTrue(e.answer in e.options && "comprar" !in e.options, e.options.toString())
        }
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

    /** A typed sentence is compared word by word: right, right but for accents, wrong in place of a word, or missing. */
    @Test
    fun aTypedSentenceIsComparedWordByWord() {
        assertEquals(
            listOf(
                WordCheck("Eu", WordResult.RIGHT, "eu"),
                WordCheck("moro", WordResult.WRONG, "mora"),
                WordCheck("no", WordResult.MISSING),
                WordCheck("Brasil.", WordResult.RIGHT, "Brasil"),
            ),
            PracticeChecker.compareWords("Eu moro no Brasil.", "eu mora Brasil"),
        )
        assertEquals(listOf(WordResult.RIGHT, WordResult.ACCENT), PracticeChecker.compareWords("minha família", "minha familia").map { it.result })
    }

    /** One slip of a letter inside a longer word is nearly right; one at its end, where words change their form, or in a short word, is not. */
    @Test
    fun aSlipOfALetterInsideAWordIsNearlyRight() {
        assertEquals(TypedResult.TYPO, PracticeChecker.check("supermercado", "supermrcado"), "missing")
        assertEquals(TypedResult.TYPO, PracticeChecker.check("supermercado", "supremercado"), "swapped")
        assertEquals(TypedResult.TYPO, PracticeChecker.check("supermercado", "supermarcado"), "wrong")
        assertEquals(TypedResult.TYPO, PracticeChecker.check("família", "fammília"), "extra")
        assertEquals(TypedResult.WRONG, PracticeChecker.check("família", "famílias"), "a plural is no slip")
        assertEquals(TypedResult.WRONG, PracticeChecker.check("comprar", "comprou"))
        assertEquals(TypedResult.WRONG, PracticeChecker.check("casa", "cosa"), "too short to be sure")
        assertEquals(TypedResult.WRONG, PracticeChecker.check("supermercado", "supermrcdo"), "two slips")
    }
}
