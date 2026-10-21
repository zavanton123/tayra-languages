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
        "아니다" to 29, "아니" to 131, "개" to 147, "아이" to 261, "수" to 23, "할" to 6, "있다" to 7,
        "야" to 4, "였어요" to 4, "돕다" to 483, "도와" to 483, "나오다" to 106, "모이다" to 953, "모" to 2518,
        "하자" to 6, "대" to 228, "대가" to 1297, "로봇" to 2279, "한" to 49, "시" to 153, "시야" to 3394, "몇" to 302,
        "오" to 183, "분" to 138, "분만" to 3152, "명" to 98, "당" to 388, "시간" to 92, "부" to 400, "여" to 814,
        "든" to 369, "살" to 131, "다음" to 272, "주" to 376, "주로" to 1512, "차" to 160, "라도" to 561, "길" to 323,
        "분야" to 662, "두" to 157, "지금" to 60, "쯤" to 741, "씩" to 934, "표" to 2288, "크다" to 140, "보이다" to 300,
        "익히다" to 4848, "익히" to 9653, "백" to 800,
        "다" to 70, "일" to 19, "고양이" to 1300, "문제" to 100, "흰색" to 5000, "방식" to 400, "결과" to 300, "이자" to 3000,
        "웬일" to 4000, "사흘" to 3500, "년" to 40, "천" to 900, "이번" to 130, "벽" to 2000, "할인" to 2400, "모자" to 1900,
        "가다" to 39, "누구" to 120, "엔" to 160, "에선" to 900, "무슨" to 200, "인" to 4,
    )

    /** Words a dictionary pack adds, without a rank. */
    private val packWords = listOf(
        "나는", "날다", "사", "집은", "집다", "개는", "아이가", "수도", "도와요", "나와요", "모였어요", "사장", "열한", "부당",
        "여든", "차라", "아이들", "길이", "개이다", "다섯", "정도", "한다", "먹이면", "먹", "크", "보", "원",
        "개다", "모자라다", "기념일", "기념", "조", "조이다", "것",
    )

    @BeforeTest
    fun lexicon() = Lexicons.set("ko", Lexicon(ranks.keys + packWords, ranks))

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

    /** The dictionary's look-alikes (집은 of 집다; 수도, "capital") give way to a word of the list and its particle. */
    @Test
    fun aWordOnlyTheDictionaryHasIsCutAfterAWordOfTheList() {
        assertEquals(listOf("우리", "집", "은"), words("우리 집은"))
        assertEquals(listOf("개", "는"), words("개는"))
        assertEquals(listOf("아이", "가"), words("아이가"))
        assertEquals(listOf("할", "수", "도", "있다"), words("할 수도 있다"))
    }

    /** A verb's form that looks like a word with particles stays whole: 요 follows no 와, nor the copula's past 모. */
    @Test
    fun verbFormsThatLookLikeParticlesStayWhole() {
        assertEquals(listOf("도와요"), words("도와요"))
        assertEquals(listOf("나와요"), words("나와요"))
        assertEquals(listOf("모였어요"), words("모였어요"))
        assertEquals(listOf("의사", "였어요"), words("의사였어요"))
        assertEquals(listOf("커피", "와"), words("커피와"))
        // A verb's form the list has (하자 of 하다) is no word of its own.
        assertEquals(listOf("하자"), words("하자"))
    }

    /** After a number or a word of time, a counter is cut from its particles, whatever word they spell. */
    @Test
    fun aCounterAfterANumberIsCutFromItsParticles() {
        assertEquals(listOf("로봇", "한", "대", "가"), words("로봇 한 대가"))
        assertEquals(listOf("대가"), words("대가"))
        assertEquals(listOf("열한", "시", "야"), words("열한 시야"))
        assertEquals(listOf("몇", "시", "야"), words("몇 시야"))
        assertEquals(listOf("시야"), words("시야"))
        assertEquals(listOf("오", "분", "만"), words("오 분만"))
        assertEquals(listOf("분", "만"), language.parseTokens("5분만").filter { it.isWord }.map { it.token })
        assertEquals(listOf("다음", "주", "로"), words("다음 주로"))
        assertEquals(listOf("주로"), words("주로"))
        // 야 follows a vowel: 두 분야 is two fields.
        assertEquals(listOf("두", "분야"), words("두 분야"))
        // 개이다 is a verb, but after a number 개 is the counter.
        assertEquals(listOf("다섯", "개", "였어요"), words("다섯 개였어요"))
        // Not across a comma.
        assertEquals(listOf("한", "대가"), words("한, 대가"))
    }

    @Test
    fun perFollowsACounter() {
        assertEquals(listOf("한", "명", "당"), words("한 명당"))
        assertEquals(listOf("시간", "당"), words("시간당"))
        assertEquals(listOf("부당"), words("부당"))
    }

    @Test
    fun numbersWrittenOutStayWhole() {
        assertEquals(listOf("여든", "살"), words("여든 살"))
        assertEquals(listOf("열한"), words("열한"))
    }

    @Test
    fun theHonorificIsCutOffAWordTheLexiconLacksWithIt() {
        assertEquals(listOf("사장", "님", "께"), words("사장님께"))
        assertEquals(listOf("사장", "님"), words("사장님"))
        assertEquals(listOf("사장", "님", "이"), words("사장님이"))
        // 선생님 is a word of the list.
        assertEquals(listOf("선생님", "께"), words("선생님께"))
        assertEquals(listOf("선생님"), words("선생님"))
    }

    @Test
    fun theCopulaInIfIsCutAfterANoun() {
        assertEquals(listOf("이", "정도", "면"), words("이 정도면"))
        assertEquals(listOf("학생", "이면"), words("학생이면"))
        assertEquals(listOf("친구", "들", "이면"), words("친구들이면"))
        // Not after a verb's stem or ending, which take 면 too, nor in a word of its own.
        assertEquals(listOf("가면"), words("가면"))
        assertEquals(listOf("한다면"), words("한다면"))
        assertEquals(listOf("하자면"), words("하자면"))
        assertEquals(listOf("먹이면"), words("먹이면"))
    }

    @Test
    fun aboutAndEachAreParticles() {
        assertEquals(listOf("세", "시", "쯤"), words("세 시쯤"))
        assertEquals(listOf("두", "개", "씩"), words("두 개씩"))
        assertEquals(listOf("지금", "쯤", "이면"), words("지금쯤이면"))
        assertEquals(listOf("표", "씩"), words("표씩"))
    }

    @Test
    fun moreFormsOfTheCopulaAreCut() {
        assertEquals(listOf("사람", "이었기"), words("사람이었기"))
        assertEquals(listOf("사람", "이었는지"), words("사람이었는지"))
        assertEquals(listOf("학생", "이구나"), words("학생이구나"))
        assertEquals(listOf("선생님", "이세요"), words("선생님이세요"))
        assertEquals(listOf("원", "이던데요"), words("원이던데요"))
        assertEquals(listOf("친구", "구나"), words("친구구나"))
        assertEquals(listOf("의사", "였는데"), words("의사였는데"))
    }

    /** Endings a verb shares with the copula stay the verb's after its stem (크구나, 보이던데요, 익히라고). */
    @Test
    fun aVerbsEndingIsNoCopula() {
        assertEquals(listOf("크구나"), words("크구나"))
        assertEquals(listOf("보이던데요"), words("보이던데요"))
        // A command quoted, where the verb is commoner than the word its stem spells; after a noun it is the copula.
        assertEquals(listOf("익히라고"), words("익히라고"))
        assertEquals(listOf("친구", "라고"), words("친구라고"))
    }

    @Test
    fun theCopulaAfterAVowelAndItsOtherFormsAreCut() {
        assertEquals(listOf("고양이", "다"), words("고양이다"))
        assertEquals(listOf("문제", "다"), words("문제다"))
        assertEquals(listOf("흰색", "이라"), words("흰색이라"))
        assertEquals(listOf("방식", "이어서"), words("방식이어서"))
        assertEquals(listOf("결과", "일"), words("결과일"))
        assertEquals(listOf("친구", "이자"), words("친구이자"))
        assertEquals(listOf("웬일", "이냐"), words("웬일이냐"))
        assertEquals(listOf("누구", "냐"), words("누구냐"))
        assertEquals(listOf("친구", "잖아"), words("친구잖아"))
        assertEquals(listOf("것", "이어서"), words("것이어서"))
    }

    /** Copula forms a verb takes too stay the verb's; one that is a word itself is no copula in a word of its own. */
    @Test
    fun theCopulasNewFormsGiveWayToVerbsAndWords() {
        // A verb whose stem the word before is (가다, 보이다, 모자라다), or a word the lexicon has (개다, 기념일).
        assertEquals(listOf("가냐"), words("가냐"))
        assertEquals(listOf("조여서"), words("조여서"))
        assertEquals(listOf("보이자"), words("보이자"))
        assertEquals(listOf("모자라서"), words("모자라서"))
        assertEquals(listOf("개다"), words("개다"))
        assertEquals(listOf("기념일"), words("기념일"))
        // Not after a word of one syllable (한일), one before a noun (무슨일 is 무슨 일), 에 (벽에다) or 할 (할인).
        assertEquals(listOf("벽", "에다"), words("벽에다"))
        assertEquals(listOf("할인"), words("할인"))
        assertEquals(listOf("무슨일"), words("무슨일"))
    }

    @Test
    fun ordinalAndWorthSuffixesAreParticles() {
        assertEquals(listOf("사흘", "째"), words("사흘째"))
        assertEquals(listOf("년", "째"), language.parseTokens("3년째").filter { it.isWord }.map { it.token })
        assertEquals(listOf("천", "원", "짜리"), words("천 원짜리"))
    }

    @Test
    fun particlesContractedWithTheTopicAreParticles() {
        assertEquals(listOf("이번", "엔"), words("이번엔"))
        assertEquals(listOf("집", "에선"), words("집에선"))
        assertEquals(listOf("친구", "한텐"), words("친구한텐"))
        assertEquals(listOf("친구", "에겐"), words("친구에겐"))
    }

    @Test
    fun longSinoKoreanNumbersStayWhole() {
        assertEquals(listOf("이십만"), words("이십만"))
        assertEquals(listOf("십만"), words("십만"))
        // Not a number in 이, which is as often the particle, nor 이만 "this much".
        assertEquals(listOf("백", "이"), words("백이"))
        assertEquals(listOf("이", "만"), words("이만"))
    }

    /** The dictionary's rare words give way to a word of the list and its particles (차라도, 아이들, 길이). */
    @Test
    fun rareDictionaryWordsGiveWayToTheListsWords() {
        assertEquals(listOf("차", "라도"), words("차라도"))
        assertEquals(listOf("아이", "들"), words("아이들"))
        assertEquals(listOf("길", "이"), words("길이"))
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
