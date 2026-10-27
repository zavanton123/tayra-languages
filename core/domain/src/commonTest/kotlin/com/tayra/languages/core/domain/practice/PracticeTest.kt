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

    /** The default setup with the ways of answering mixed rather than chosen by level, where the mixing is what is tested. */
    private val mixed = PracticeSetup(adaptive = false)
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
        val exercises = PracticeBuilder.build(words, pageWords, random = Random(1), setup = mixed)
        assertEquals(10, exercises.size)
        assertEquals(words.map { it.text }.toSet(), exercises.map { it.word }.toSet())
        assertEquals(setOf(gapChoice, gapTyped, hearChoice), exercises.map { it.kind }.toSet())
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
        val typing = PracticeBuilder.build(words, pageWords, examples, Random(1), PracticeSetup(types = setOf(ExerciseType.GAP, ExerciseType.HEAR_SENTENCE), responses = setOf(PracticeResponse.TYPE)))
        assertTrue(typing.isNotEmpty() && typing.none { it.isChoice }, "typing: ${typing.map { it.kind }}")
        assertTrue(typing.any { it.context == PracticeContext.EXAMPLE }, "and typed where only typing is asked")
        assertEquals(emptyList(), PracticeBuilder.build(words, pageWords, examples, Random(1), PracticeSetup(types = emptySet())))
    }

    /** Without a length every question comes for every word; with sentences from elsewhere only, only the words that have one are asked. */
    @Test
    fun theLengthAndTheSentencesAskedForAreKept() {
        assertEquals(words.size * 3, PracticeBuilder.build(words, pageWords, random = Random(1), setup = mixed.copy(length = null)).size)
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

    /** Hearing a sentence, the word with the meaning given is chosen among the sentence's other words, or typed; it needs the meaning. */
    @Test
    fun aHeardSentenceAsksForTheWordWithTheMeaningAmongItsOwnWords() {
        val setup = PracticeSetup(types = setOf(ExerciseType.HEAR_SENTENCE), length = null, adaptive = false)
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

    /** A missing word is chosen among two of its other forms, where it has them, and other words: only the right form fits. */
    @Test
    fun aMissingWordIsChosenAmongItsOtherFormsToo() {
        val comprar = setOf("comprar", "compro", "compra", "compramos", "compraram")
        val families = mapOf("comprar" to WordFamily("comprar", comprar), "moro" to WordFamily("morar", setOf("moro", "morar")))
        val setup = PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.CHOOSE), length = null)
        val asked = PracticeBuilder.build(words, pageWords, random = Random(1), setup = setup, families = families).associateBy { it.word }
        val buy = asked.getValue("comprar").options
        assertEquals(4, buy.size)
        assertEquals(1, buy.count { it == "comprar" })
        assertEquals(2, buy.count { it != "comprar" && it in comprar }, buy.toString())
        assertEquals(1, buy.count { it !in comprar }, "another word too, so the choice also takes knowing the word")
        val live = asked.getValue("moro").options
        assertEquals(listOf("morar"), live.filter { it != "moro" && it in setOf("moro", "morar") }, "a word with one other form has that one among its choices")
    }

    /** A matching round comes first, on up to five words whose meanings share no word; with fewer than four such words there is none. */
    @Test
    fun aMatchingRoundComesFirstOnWordsThatCannotBeConfused() {
        val setup = PracticeSetup(types = setOf(ExerciseType.MATCHING, ExerciseType.GAP), adaptive = false)
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
        val setup = PracticeSetup(types = setOf(ExerciseType.BUILD), responses = setOf(PracticeResponse.CHOOSE), length = null)
        val asked = PracticeBuilder.build(words, pageWords, random = Random(1), setup = setup, translations = translations)
        assertEquals(listOf("Eu moro no Brasil."), asked.map { it.sentence }, "the others are longer than eight words")
        val built = asked.single()
        assertTrue(built.wholeSentence)
        assertEquals(built.sentence, built.answer)
        assertEquals(listOf("Brasil.", "Eu", "moro", "no"), built.tiles.sorted())
        assertTrue(built.tiles != listOf("Eu", "moro", "no", "Brasil."))
    }

    /**
     * A word heard with three words spelled nearly like it is asked by its meaning or picked among
     * them, either way; without a meaning it is picked among them, and among others where they run short.
     */
    @Test
    fun aHeardWordIsPickedAmongLookAlikesOrAskedByItsMeaning() {
        val families = mapOf("comprar" to WordFamily("comprar", setOf("comprar", "compro", "compra", "compram", "compramos")))
        val alikes = mapOf("moro" to listOf("mora", "mole"))
        val setup = PracticeSetup(types = setOf(ExerciseType.HEAR_WORD), length = null)
        val asked = (1..20).map { seed -> PracticeBuilder.build(words, pageWords, random = Random(seed), setup = setup, alikes = alikes, families = families) }
        val comprar = asked.map { round -> round.single { it.word == "comprar" } }
        assertTrue(comprar.any { it.answer == "to buy" } && comprar.any { it.answer == "comprar" }, "asked both ways")
        comprar.filter { it.answer == "comprar" }.forEach { heard ->
            assertEquals("comprar", heard.spoken)
            assertEquals(4, heard.options.size)
            assertTrue(heard.options.all { it in families.getValue("comprar").forms })
        }
        assertTrue(asked.all { round -> round.single { it.word == "moro" }.answer == "I live" }, "two look-alikes are too few to ask by")

        val bare = words.map { it.copy(translation = null) }
        val moro = PracticeBuilder.build(bare, pageWords, random = Random(2), setup = setup, alikes = alikes, families = families).single { it.word == "moro" }
        assertEquals("moro", moro.answer)
        assertEquals(4, moro.options.size)
        assertTrue(moro.options.containsAll(listOf("moro", "mora")), moro.options.toString())
    }

    /** Adapting, a word is answered the ways its level calls for: a new one chosen, one further on typed. */
    @Test
    fun anAdaptingSetupAsksEachWordAtItsLevel() {
        val setup = PracticeSetup(types = setOf(ExerciseType.GAP), responses = PracticeResponse.entries.toSet(), length = null, adaptive = true)
        val asked = PracticeBuilder.build(words, pageWords, random = Random(1), setup = setup, levels = mapOf("moro" to 3, "semana" to 1))
        assertTrue(asked.filter { it.word == "moro" }.all { it.response == PracticeResponse.TYPE || it.response == PracticeResponse.SPEAK })
        assertTrue(asked.filter { it.word == "semana" }.all { it.response == PracticeResponse.CHOOSE || it.response == PracticeResponse.TYPE })
        assertTrue(asked.filter { it.word == "comprar" }.all { it.response == PracticeResponse.CHOOSE }, "a word never practised is recognised first")
        // A way the setup leaves out is not asked even where the level calls for it.
        val chooseOnly = PracticeBuilder.build(words, pageWords, random = Random(1), setup = setup.copy(responses = setOf(PracticeResponse.CHOOSE)), levels = mapOf("moro" to 3))
        assertTrue(chooseOnly.all { it.response == PracticeResponse.CHOOSE })
        assertTrue(asked.filter { it.word == "moro" }.all { it.termId == null })
    }

    /** A setup goes into a route as text and comes back the same; the kinds earlier versions named still give their questions. */
    @Test
    fun aSetupIsWrittenAndReadAsText() {
        val setup = PracticeSetup(types = setOf(ExerciseType.GAP, ExerciseType.HEAR_SENTENCE), responses = setOf(PracticeResponse.TYPE), contexts = setOf(PracticeContext.ORIGINAL), length = null)
        assertEquals("types=GAP,HEAR_SENTENCE;responses=TYPE;contexts=ORIGINAL;length=all", setup.encode())
        assertEquals(true, PracticeSetup.decode(setup.encode() + ";adaptive=0").adaptive, "practice always adapts now")
        assertEquals(setup, PracticeSetup.decode(setup.encode() + ";words=WEAKEST"), "a setup kept with a choice of words, which was dropped, asks about all of them")
        assertEquals(setup, PracticeSetup.decode(setup.encode()))
        assertEquals(PracticeSetup(), PracticeSetup.decode(""), "nothing said: the defaults")
        assertEquals(setOf(ExerciseType.GAP), PracticeSetup.decode("types=GAP,SOMETHING_NEW").types, "names not known are left out")
        assertEquals(setOf(ExerciseType.HEAR_WORD), PracticeSetup.decode("types=SOUND_ALIKE").types, "the sound-alike question is now asked within Hear the word")
        assertEquals(setOf(ExerciseType.GAP), PracticeSetup.decode("types=DICTATION").types, "dictation was the missing word, typed")
        assertEquals(setOf(ExerciseType.GAP), PracticeSetup.decode("types=SHADOW").types, "shadowing was the missing word's phrase, said")
        assertEquals(PracticeSetup.DEFAULT_LENGTH, PracticeSetup.decode("length=-3").length)
        assertEquals(listOf(gapChoice, hearChoice, gapTyped), PracticeSetup.ofKinds(emptyList()).rotation)
        assertEquals(listOf(gapChoice, hearChoice), PracticeSetup.ofKinds(listOf("GAP_CHOICE", "HEAR_CHOOSE")).rotation)
        assertEquals(listOf(gapTyped), PracticeSetup.ofKinds(listOf("DICTATION")).rotation)
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
    fun listeningAsksForTheMeaningAndTypingForTheMissingWord() {
        val exercises = PracticeBuilder.build(words, pageWords, random = Random(3))
        exercises.filter { it.type == ExerciseType.HEAR_WORD }.forEach { e ->
            assertEquals(e.word, e.spoken)
            assertEquals(e.wordTranslation, e.answer, "the choices are meanings when every word has one")
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
        assertEquals(null, fresh.credit)
        val credited = PracticeBuilder.build(words, pageWords, mapOf("comprar" to listOf(examples.getValue("comprar").single().copy(credit = "Tatoeba"))), Random(5))
        assertEquals("Tatoeba", credited.single { it.context == PracticeContext.EXAMPLE }.credit)
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
        val exercises = PracticeBuilder.build(words.take(1), pageWords, random = Random(6), setup = mixed)
        assertEquals(3, exercises.size)
        assertEquals(3, exercises.map { it.kind }.toSet().size)
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

    /** Said aloud, a gap keeps the phrase around it to say. */
    @Test
    fun spokenAnswersSayTheSentence() {
        val said = PracticeBuilder.build(words, pageWords, random = Random(1), setup = PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.SPEAK)))
        assertTrue(said.isNotEmpty() && said.all { it.response == PracticeResponse.SPEAK && !it.isChoice && it.tiles.isEmpty() }, "said: ${said.map { it.kind }}")
        // A gap is said as the phrase around it, never as the word alone.
        said.filter { it.type == ExerciseType.GAP }.forEach { e ->
            val say = e.say!!
            assertTrue(e.range.first >= say.first && e.range.last <= say.last, "${e.saying} holds ${e.word}")
            assertTrue(e.saying!!.split(" ").size >= 3, e.saying)
        }
        assertTrue(ExerciseType.entries.filter { PracticeResponse.SPEAK in it.responses }.all { it != ExerciseType.HEAR_SENTENCE }, "a word heard is not said alone")
    }

    /** Where nothing can be said, answers said are typed. */
    @Test
    fun withoutSpeakingAnswersAreTyped() {
        assertEquals(PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.TYPE)), PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.SPEAK)).withoutSpeaking())
        assertEquals(setOf(PracticeResponse.CHOOSE), PracticeSetup(responses = setOf(PracticeResponse.CHOOSE, PracticeResponse.SPEAK)).withoutSpeaking().responses)
        val plain = PracticeSetup()
        assertTrue(plain.withoutSpeaking() === plain)
    }

    @Test
    fun aSpokenPhraseCountsWithItsWordAndMostOfTheRest() {
        assertEquals(TypedResult.CORRECT, PracticeChecker.checkSpoken("Eu moro no Brasil", "moro", "Eu moro no Brasil."))
        assertEquals(TypedResult.CORRECT, PracticeChecker.checkSpoken("com a minha familia e os meus irmaos", "família", "com a minha família e os meus irmãos"), "accents are the recogniser's")
        assertEquals(TypedResult.CORRECT, PracticeChecker.checkSpoken("vou fazer compras por favor", "por favor", "vou fazer compras, por favor"), "several words")
        assertEquals(TypedResult.ALMOST, PracticeChecker.checkSpoken("eu moro no Brasil com a minha família", "moro", "Eu moro no Brasil com a minha família e"), "eight of nine, the word among them")
        assertEquals(TypedResult.WRONG, PracticeChecker.checkSpoken("moro", "moro", "Eu moro no Brasil."), "the word alone does not do")
        assertEquals(TypedResult.WRONG, PracticeChecker.checkSpoken("eu morei no Brasil", "moro", "Eu moro no Brasil."))
        assertEquals(TypedResult.WRONG, PracticeChecker.checkSpoken("  ", "moro", "Eu moro no Brasil."))
        assertEquals(TypedResult.WRONG, PracticeChecker.checkSpoken("eu amoroso no Brasil", "moro", "Eu moro no Brasil."), "a word, not part of one")
    }

    /** The phrase said is the stretch between punctuation that holds the word, or the sentence where that is very short, eight words at most. */
    @Test
    fun thePhraseSaidIsTheStretchAroundTheWord() {
        fun said(sentence: String, word: String) = sentence.substring(PracticeBuilder.phrase(sentence, Cloze.range(sentence, word)!!))
        assertEquals("Quando precisamos comprar alguma coisa,", said(long, "comprar"))
        assertEquals("passamos no supermercado perto de casa.", said(long, "supermercado"))
        assertEquals("Eu moro no Brasil.", said("Eu moro no Brasil.", "moro"))
        assertEquals("Sim, eu moro no Brasil.", said("Sim, eu moro no Brasil.", "Sim"), "one word before a comma is too short alone")
        val longClause = "Eu moro com a minha família e os meus dois irmãos mais novos numa casa grande."
        assertEquals("e os meus dois irmãos mais novos numa", said(longClause, "irmãos"))
        assertEquals("Eu moro com a minha família e os", said(longClause, "Eu"))
    }

    @Test
    fun aRepeatedSentenceCountsByTheShareOfItsWordsHeard() {
        val sentence = "Durante a semana, tenho uma rotina bastante organizada e muito cheia."
        assertEquals(TypedResult.CORRECT, PracticeChecker.checkSpoken("durante a semana tenho uma rotina bastante organizada e muito cheia", null, sentence))
        assertEquals(TypedResult.ALMOST, PracticeChecker.checkSpoken("durante a semana tenho uma rotina bastante organizada e cheia", null, sentence), "ten of eleven")
        assertEquals(TypedResult.WRONG, PracticeChecker.checkSpoken("durante a semana tenho", null, sentence))
    }

    /** A setup kept from when letter tiles were a way of answering keeps its other ways, or the default ones when tiles were all it had. */
    @Test
    fun setupsKeptWithLetterTilesStillOpen() {
        assertEquals(setOf(PracticeResponse.CHOOSE), PracticeSetup.decode("types=GAP;responses=CHOOSE,TILES;contexts=ORIGINAL;length=10").responses)
        assertEquals(PracticeSetup().responses, PracticeSetup.decode("types=GAP;responses=TILES;contexts=ORIGINAL;length=10").responses)
    }

    /** Likewise a setup kept with true or false, which was dropped. */
    @Test
    fun setupsKeptWithTrueOrFalseStillOpen() {
        assertEquals(setOf(ExerciseType.GAP), PracticeSetup.decode("types=GAP,TRUE_FALSE;responses=CHOOSE;contexts=ORIGINAL;length=10").types)
        assertEquals(PracticeSetup().types, PracticeSetup.decode("types=TRUE_FALSE;responses=CHOOSE;contexts=ORIGINAL;length=10").types)
    }

    /** A word is never asked the same question in the same sentence twice, even where its level leaves only one way of answering. */
    @Test
    fun noQuestionIsAskedTwice() {
        val setup = PracticeSetup(types = setOf(ExerciseType.GAP, ExerciseType.HEAR_WORD), responses = setOf(PracticeResponse.CHOOSE, PracticeResponse.TYPE), length = null)
        val asked = PracticeBuilder.build(words, pageWords, random = Random(1), setup = setup)
        val keys = asked.map { "${it.practised}|${it.type}|${it.response}|${it.sentence}" }
        assertEquals(keys.distinct(), keys)
        // New words are only chosen: the missing word and the word heard, once each.
        assertEquals(words.size * 2, asked.size)
        // A length is still filled where there are enough different questions.
        assertEquals(7, PracticeBuilder.build(words, pageWords, random = Random(1), setup = setup.copy(length = 7)).size)
    }

    @Test
    fun aHintIsGivenAsItsWords() {
        assertEquals(listOf("Eu", "moro", "no", "Brasil"), hintWords("Eu moro no Brasil."))
        assertEquals(listOf("d'água", "guarda-chuva"), hintWords("d'água, guarda-chuva!"))
        assertEquals(emptyList(), hintWords(null))
    }
}
