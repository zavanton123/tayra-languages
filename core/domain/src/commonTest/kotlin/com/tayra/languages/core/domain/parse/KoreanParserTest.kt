package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.language.LanguageDefinitions
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KoreanParserTest {
    private val language = LanguageDefinitions.all.first { it.name == "Korean" }.language

    /** Ranks as a frequency list gives them: particles first, then the words and verbs with their forms. */
    private val ranks = mapOf(
        "을" to 1, "이" to 2, "에" to 3, "가" to 5, "이다" to 4, "이에요" to 4, "예요" to 4, "입니다" to 4, "하다" to 6,
        "를" to 8, "는" to 9, "의" to 10, "은" to 11, "들" to 12, "도" to 14, "으로" to 15, "에서" to 16, "로" to 20,
        "과" to 26, "와" to 41, "만" to 46, "나" to 50, "에게" to 55, "까지" to 64, "저" to 72, "요" to 80, "부터" to 81,
        "한테" to 98, "처럼" to 171, "이나" to 230, "랑" to 231, "보다" to 25, "마다" to 125, "밖에" to 600, "께" to 2500,
        "에게서" to 3900, "이랑" to 2000, "께서" to 1900,
        "사람" to 34, "우리" to 35, "가다" to 39, "가는" to 39, "먹다" to 184, "먹었어요" to 184, "학교" to 280, "친구" to 164,
        "책" to 700, "집" to 121, "나가다" to 174, "나가" to 174, "사과" to 2600, "학생" to 520, "의사" to 1500,
        "서울" to 161, "선생님" to 400, "동생" to 900, "커피" to 1800, "때문" to 95, "너" to 38, "말" to 34,
        "아니다" to 29, "아니" to 131,
    )

    @BeforeTest
    fun lexicon() = Lexicons.set("ko", Lexicon(ranks.keys + listOf("나는", "날다", "사"), ranks))

    @AfterTest
    fun forget() = Lexicons.set("ko", null)

    private fun words(text: String) = language.parseTokens(text).filter { it.isWord }.map { it.token }

    @Test
    fun particlesAreCutOffTheWordBeforeThem() {
        assertEquals(listOf("학교", "에서"), words("학교에서"))
        assertEquals(listOf("친구", "가"), words("친구가"))
        assertEquals(listOf("책", "을"), words("책을"))
        assertEquals(listOf("저", "는"), words("저는"))
        assertEquals(listOf("사람", "들", "이"), words("사람들이"))
    }

    @Test
    fun stackedParticlesAreEachAWord() {
        assertEquals(listOf("집", "에", "도"), words("집에도"))
        assertEquals(listOf("학교", "에서", "부터"), words("학교에서부터"))
        assertEquals(listOf("친구", "에게", "만"), words("친구에게만"))
        // The polite 요 after a particle, never after a word (저요 stays whole).
        assertEquals(listOf("학교", "에서", "요"), words("학교에서요"))
        assertEquals(listOf("저요"), words("저요"))
    }

    @Test
    fun eachParticleTakesTheFormTheSoundBeforeItWants() {
        assertEquals(listOf("학생", "이"), words("학생이"))
        assertEquals(listOf("의사", "가"), words("의사가"))
        assertEquals(listOf("학생", "은"), words("학생은"))
        assertEquals(listOf("친구", "를"), words("친구를"))
        assertEquals(listOf("책", "과"), words("책과"))
        assertEquals(listOf("커피", "와"), words("커피와"))
        assertEquals(listOf("집", "으로"), words("집으로"))
        assertEquals(listOf("학교", "로"), words("학교로"))
        assertEquals(listOf("서울", "로"), words("서울로"))
        assertEquals(listOf("책", "이나"), words("책이나"))
        assertEquals(listOf("커피", "나"), words("커피나"))
        assertEquals(listOf("동생", "이랑"), words("동생이랑"))
        assertEquals(listOf("친구", "랑"), words("친구랑"))
        // A particle after the wrong sound is no particle: 학생가 is left as it is.
        assertEquals(listOf("학생가"), words("학생가"))
        assertEquals(listOf("서울으로"), words("서울으로"))
    }

    @Test
    fun theCopulaAfterANounIsAWord() {
        assertEquals(listOf("학생", "이에요"), words("학생이에요"))
        assertEquals(listOf("의사", "예요"), words("의사예요"))
        assertEquals(listOf("학생", "입니다"), words("학생입니다"))
        assertEquals(listOf("때문", "이에요"), words("때문이에요"))
        assertEquals(listOf("말", "이지"), words("말이지"))
        // 아니 takes no copula, being 아니다's stem: 아니라고 is a form of 아니다.
        assertEquals(listOf("아니라고"), words("아니라고"))
    }

    @Test
    fun theOtherParticlesAreCutToo() {
        assertEquals(listOf("선생님", "께"), words("선생님께"))
        assertEquals(listOf("선생님", "께서"), words("선생님께서"))
        assertEquals(listOf("친구", "한테"), words("친구한테"))
        assertEquals(listOf("친구", "에게서"), words("친구에게서"))
        assertEquals(listOf("서울", "까지"), words("서울까지"))
        assertEquals(listOf("우리", "의"), words("우리의"))
        assertEquals(listOf("친구", "처럼"), words("친구처럼"))
        assertEquals(listOf("친구", "보다"), words("친구보다"))
        assertEquals(listOf("집", "마다"), words("집마다"))
        assertEquals(listOf("친구", "밖에"), words("친구밖에"))
    }

    /** A word the list has stays whole; a verb's ending is no particle; an unknown beginning is cut from nothing. */
    @Test
    fun wordsAndVerbsStayWhole() {
        assertEquals(listOf("사과"), words("사과"))
        assertEquals(listOf("나가"), words("나가"))
        // 나 takes 가 only as 내 (내가), but 너가 is spoken for 네가.
        assertEquals(listOf("너", "가"), words("너가"))
        assertEquals(listOf("가는"), words("가는"))
        assertEquals(listOf("먹었어요"), words("먹었어요"))
        assertEquals(listOf("하늘에서"), words("하늘에서"))
        // The dictionary's 나는 (a form of 날다) is rarer than 나 and 는 together, so it is cut.
        assertEquals(listOf("나", "는"), words("나는"))
    }

    @Test
    fun spacesAndPunctuationStayBetweenTheWords() {
        val tokens = language.parseTokens("저는 학생이에요. 친구가 학교에서 와요!")
        assertEquals(
            listOf("저", "는", " ", "학생", "이에요", ". ", "친구", "가", " ", "학교", "에서", " ", "와요", "!"),
            tokens.map { it.token },
        )
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1), tokens.map { it.sentenceNumber })
    }

    @Test
    fun withoutALexiconAWordIsLeftWhole() {
        Lexicons.set("ko", null)
        assertEquals(listOf("학교에서"), words("학교에서"))
    }
}
