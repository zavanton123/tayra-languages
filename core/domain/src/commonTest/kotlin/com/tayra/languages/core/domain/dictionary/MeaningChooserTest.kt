package com.tayra.languages.core.domain.dictionary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A word's meaning is the one its sentence's translation uses, as measured on lessons' new words. */
class MeaningChooserTest {
    private fun entry(word: String, pos: String, vararg glosses: String) = DictionaryEntry(word, pos, null, glosses.map { DictionarySense(listOf(it)) })

    // The app ranks the barter verb first: more meanings than the lorry.
    private val truck = DictionaryLookup(
        listOf(
            entry("truck", "verb", "обменивать, менять; получать в результате обмена", "вести меновую торговлю", "променять"),
            entry("truck", "noun", "амер. грузовой автомобиль, грузовик", "тележка, тачка"),
        ),
        emptyList(),
    )

    @Test
    fun theSentencesTranslationChoosesTheMeaning() {
        val choice = MeaningChooser.choose(truck, "truck", "Thank you for coming to the debut of our food truck!", "en", "Спасибо, что пришли на дебют нашего грузовика!", "грузовик")!!
        assertEquals(1 to 0, choice.entry to choice.sense)
        assertEquals("амер. грузовой автомобиль, грузовик", choice.gloss)
        assertEquals(true, choice.fits)
    }

    @Test
    fun aHeadwordCountsAboveAWordOfAnExplanation() {
        val attempt = DictionaryLookup(
            listOf(entry("attempt", "verb", "пытаться, стараться, пробовать, делать попытку"), entry("attempt", "noun", "попытка, проба")),
            emptyList(),
        )
        val choice = MeaningChooser.choose(attempt, "attempt", "Lily made her first attempt.", "en", "Лили сделала свою первую попытку.", "попытка")!!
        assertEquals(1 to 0, choice.entry to choice.sense)
    }

    @Test
    fun aspectPairsAndEndingsMatchWithoutAStemmer() {
        val refer = DictionaryLookup(
            listOf(entry("refer", "verb", "ссылаться, опираться (на кого-л.)", "направлять; отсылать (к кому-л.)")),
            emptyList(),
        )
        val choice = MeaningChooser.choose(refer, "refer", "I can refer you to the manager.", "en", "Я могу направить тебя к менеджеру.", "ссылаться")!!
        assertEquals(0 to 1, choice.entry to choice.sense)
        assertEquals(true, choice.fits)
    }

    @Test
    fun englishNeighboursTellANounFromAVerb() {
        val overload = DictionaryLookup(listOf(entry("overload", "noun", "перегрузка"), entry("overload", "verb", "перегружать")), emptyList())
        // Neither translation decides here; "can" before the word makes it a verb.
        val choice = MeaningChooser.choose(overload, "overload", "Too many commands can overload a dog.", "en", null, null)!!
        assertEquals(1 to 0, choice.entry to choice.sense)
        assertEquals(false, choice.fits)
    }

    @Test
    fun withNothingToGoOnTheFirstMeaningIsAGuess() {
        val choice = MeaningChooser.choose(truck, "truck", null, "en", null, null)!!
        assertEquals(0 to 0, choice.entry to choice.sense)
        assertEquals(false, choice.fits)
        assertNull(MeaningChooser.choose(DictionaryLookup.EMPTY, "truck", null, "en", null, null))
    }

    @Test
    fun theWordAloneDecidesWhenTheSentenceCannot() {
        val choice = MeaningChooser.choose(truck, "truck", "We bought a truck.", "pt", "Мы купили фуру.", "грузовик")!!
        assertEquals(1 to 0, choice.entry to choice.sense)
        assertEquals(false, choice.fits)
    }

    @Test
    fun chineseGlossesAreFoundWhole() {
        val bank = DictionaryLookup(listOf(entry("bank", "noun", "河岸", "银行")), emptyList())
        val choice = MeaningChooser.choose(bank, "bank", "I went to the bank.", "en", "我去了银行。", null)!!
        assertEquals(0 to 1, choice.entry to choice.sense)
    }
}
