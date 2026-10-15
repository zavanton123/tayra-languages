package com.tayra.languages.core.data

import com.tayra.languages.core.data.parse.KuromojiBoundaries
import com.tayra.languages.core.domain.parse.JapaneseParser
import com.tayra.languages.core.domain.parse.Lexicon
import com.tayra.languages.core.domain.parse.Lexicons
import com.tayra.languages.core.domain.parse.Segmenters
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KuromojiBoundariesTest {

    private val boundaries = KuromojiBoundaries()

    private fun loaded(): KuromojiBoundaries {
        val deadline = System.currentTimeMillis() + 60_000
        while (boundaries.of("に") == null && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertNotNull(boundaries.of("に"), "Kuromoji loads its dictionary")
        return boundaries
    }

    @AfterTest
    fun forget() {
        Segmenters.set("ja", null)
        Lexicons.set("ja", null)
    }

    @Test
    fun mecabsBoundariesKeepTheLongestMatchFromRunningIntoTheNextWord() {
        Lexicons.set("ja", Lexicon(listOf("すもも", "もも", "も", "の", "うち", "かれ", "は", "はい", "はいしゃ", "いしゃ", "に", "なる", "食べる", "東京都", "住む", "この", "本", "とても", "面白い", "わたし", "学生", "です", "選ぶ", "くださる", "ください")))
        val parser = JapaneseParser()
        // The lexicon alone reads "peach peach peach" and "he, dentist".
        assertEquals(listOf("すもも", "もも", "もも", "もも", "の", "うち"), parser.segment("すもももももももものうち"))
        assertEquals(listOf("かれ", "はいしゃ", "に", "なった"), parser.segment("かれはいしゃになった"))
        Segmenters.set("ja", loaded())
        assertEquals(listOf("すもも", "も", "もも", "も", "もも", "の", "うち"), parser.segment("すもももももももものうち"))
        assertEquals(listOf("かれ", "は", "いしゃ", "に", "なった"), parser.segment("かれはいしゃになった"))
        // MeCab cuts an inflected verb into 食べ・まし・た; the reader's word is the whole of it.
        assertEquals(listOf("食べました"), parser.segment("食べました"))
        assertEquals(listOf("東京都", "に", "住んでいる"), parser.segment("東京都に住んでいる"))
        assertEquals(listOf("この", "本", "は", "とても", "面白かった"), parser.segment("この本はとても面白かった"))
        assertEquals(listOf("わたし", "は", "学生", "です"), parser.segment("わたしは学生です"))
        // A helper the inflection rules cannot undo is a word of its own.
        assertEquals(listOf("選んで", "ください"), parser.segment("選んでください"))
    }

    @Test
    fun aWordTheLexiconLacksIsMecabsPieceNotLetterByLetter() {
        Lexicons.set("ja", Lexicon(listOf("に", "行く")))
        Segmenters.set("ja", loaded())
        assertEquals(listOf("東京", "に", "行きます"), JapaneseParser().segment("東京に行きます"))
    }
}
