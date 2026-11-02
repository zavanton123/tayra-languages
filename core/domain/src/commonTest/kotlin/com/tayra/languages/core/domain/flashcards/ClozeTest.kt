package com.tayra.languages.core.domain.flashcards

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClozeTest {
    /** A word that only ends differently stands in for the card's word: a plural's singular, a verb's other ending. */
    @Test
    fun aNearFormIsFoundByItsStem() {
        assertEquals(4 until 9, Cloze.nearRange("Uma turma nova abriu.", "turmas"))
        assertEquals(4 until 10, Cloze.nearRange("Ela compra pão.", "comprar"))
        assertEquals(4 until 13, Cloze.nearRange("Nós compramos pão.", "comprar"))
    }

    @Test
    fun shortWordsAndPhrasesHaveNoNearForm() {
        assertNull(Cloze.nearRange("Eu caso amanhã.", "casa"), "a word of four letters")
        assertNull(Cloze.nearRange("Vou comprar pão.", "ir comprar"), "a phrase")
        assertNull(Cloze.nearRange("A escola fecha cedo.", "turmas"))
        assertNull(Cloze.nearRange("Os compradores chegaram.", "comprar"), "more than two letters longer")
    }

    /** A form the dictionary ties to the word is found either way round, or through a lemma they share. */
    @Test
    fun theDictionaryFindsAFormOfTheWord() = runTest {
        val lemmas = mapOf("temia" to listOf("temer"), "cabia" to listOf("caber"), "aceitei" to listOf("aceitar"))
        val lookup: suspend (String) -> List<String> = { lemmas[it].orEmpty() }
        assertEquals(12 until 17, Cloze.lemmaRange("O seu Jorge temia pelo café.", "temer", lookup))
        assertEquals(12 until 19, Cloze.lemmaRange("Não podemos aceitar tudo.", "aceitei", lookup))
        assertNull(Cloze.lemmaRange("O seu Jorge comia pelo café.", "temer", lookup))
    }

    /** The word beginning most like the card's is the one hidden, accents aside and the nearer length winning a tie. */
    @Test
    fun theWordBeginningMostAlikeIsFound() {
        assertEquals(12 until 17, Cloze.sharedBeginningRange("O seu Jorge temia pelo café.", "temer"))
        assertEquals(3 until 9, Cloze.sharedBeginningRange("Só sobrou um pouco.", "sobrar"))
        assertEquals(29 until 35, Cloze.sharedBeginningRange("Está claro que ninguém tem a propor.", "propõe"))
        assertEquals(8 until 14, Cloze.sharedBeginningRange("Ele foi eleito capitão.", "eleger"), "eleito over Ele")
    }

    @Test
    fun aWordBeginningOtherwiseIsNotTakenForIt() {
        assertNull(Cloze.sharedBeginningRange("Eu trabalho num posto de gasolina.", "pôr"), "two letters in common")
        assertNull(Cloze.sharedBeginningRange("O casamento foi ontem.", "casa"), "four letters in common are less than half of casamento")
        assertNull(Cloze.sharedBeginningRange("私は食べる", "食べた"), "a script written without spaces")
    }
}
