package com.tayra.languages.core.domain.flashcards

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
}
