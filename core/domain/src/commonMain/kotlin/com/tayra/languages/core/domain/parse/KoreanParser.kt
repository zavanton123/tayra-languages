package com.tayra.languages.core.domain.parse

import kotlin.math.ln

/**
 * Korean: each space-separated word cut into the word and the particles after it (친구가 is 친구 + 가,
 * 집에도 is 집 + 에 + 도), which then read as words of their own, as Japanese particles do. Verbs and
 * adjectives stay whole (먹었어요): their endings are no particles, and lookup finds the verb.
 *
 * After a number (한 대가, 3개월에, 열한 시야) or a word of time (다음 주로), a word that is a counter with
 * particles is cut so: 대 + 가, not 대가 "price". A number written out stays whole (여든, not 여 + 든).
 *
 * A word is cut only after a word the lexicon knows, or such a word with the honorific 님 (사장님께). Of the
 * cuts and the word left whole, the one whose
 * pieces are together the most common wins, by the costs SegmentingParser gives Chinese words: a word the
 * frequency list has stays whole (사과; 나가, a form of 나가다). One only a dictionary has never stays
 * whole against a cut after a word of the list: the dictionary's forms tables are full of look-alikes
 * (집은 of 집다, 나는 of 날다), and the list has the words a reader meets.
 *
 * tools/build_frequency_list.py counts words cut the same way (korean_pieces); keep the two the same.
 */
class KoreanParser : SpaceDelimitedParser(), LexiconParser {

    override val name: String get() = "Korean"

    override fun words(word: String): List<String> = words(word, null)

    override fun words(word: String, previous: String?): List<String> {
        val lexicon = Lexicons[CODE] ?: return listOf(word)
        return counterCut(word, lexicon, previous) ?: cut(word, lexicon)
    }

    fun cut(word: String, lexicon: Lexicon): List<String> {
        if (NATIVE_NUMBER.matches(word)) return listOf(word)
        var best: Double? = null
        var cut = listOf(word)
        var afterListed = false
        for (end in word.length - 1 downTo 1) {
            val stem = word.substring(0, end)
            if (stem !in lexicon && !(stem.endsWith(HONORIFIC) && end > 1 && stem.dropLast(1) in lexicon)) continue
            for (stack in stacks(word.substring(end), stem, lexicon)) {
                val pieces = listOf(stem) + stack
                val cost = pieces.sumOf { ln((lexicon.rank(it) ?: UNRANKED).toDouble()) + WORD_COST }
                if (lexicon.rank(stem) != null) afterListed = true
                if (best == null || cost < best) {
                    best = cost
                    cut = pieces
                }
            }
        }
        if (best == null) return listOf(word)
        val whole = lexicon.rank(word) ?: if (word in lexicon && !afterListed) UNRANKED else null
        return if (whole != null && ln(whole.toDouble()) + WORD_COST <= best) listOf(word) else cut
    }

    /**
     * The word cut as a counter and its particles when [previous], the word before it, is a number or a word
     * of time before a unit of time; null otherwise, or when the word is a counter itself.
     */
    fun counterCut(word: String, lexicon: Lexicon, previous: String?): List<String>? {
        if (previous == null) return null
        val counters = when {
            isNumber(previous) || previous == HOW_MANY -> COUNTERS
            previous in TIME_WORDS -> TIME_COUNTERS
            else -> return null
        }
        for (counter in counters.filter { word.startsWith(it) }.sortedByDescending { it.length }) {
            if (counter == word) return null
            // A counter's stack may hold the copula's past: 다섯 개였어요 is 개 + 였어요, though 개이다 is a verb.
            val best = stacks(word.substring(counter.length), counter, null)
                .minByOrNull { stack -> stack.sumOf { ln((lexicon.rank(it) ?: UNRANKED).toDouble()) } }
            if (best != null) return listOf(counter) + best
        }
        return null
    }

    /** Whether [word] is a number: in digits, or written out, native (세, 열한, 여든) or Sino-Korean (오, 삼십). */
    private fun isNumber(word: String): Boolean =
        word.isNotEmpty() && word.all { it.isDigit() } || NATIVE_NUMBER.matches(word) || SINO_NUMBER.matches(word)

    /** Every way [rest] is a stack of particles after [stem]; with the [lexicon], no copula's past after a verb's stem in 이. */
    private fun stacks(rest: String, stem: String, lexicon: Lexicon?): List<List<String>> {
        val out = mutableListOf<List<String>>()
        val verbInI = lexicon != null && stem + COPULA_STEM in lexicon

        fun go(at: Int, last: Char, slot: Int, pieces: List<String>, place: Int) {
            if (at == rest.length) {
                out += pieces
                return
            }
            if (pieces.size == STACK) return
            if (pieces.isNotEmpty() && pieces.last() !in NOT_BEFORE_POLITE && rest.endsWith(POLITE) && at + POLITE.length == rest.length) {
                out += pieces + POLITE
            }
            if (slot <= 1 && (pieces.isNotEmpty() || stem !in NOT_BEFORE_COPULA)) {
                for ((form, sound) in COPULA) {
                    if (pieces.isEmpty() && verbInI && form.startsWith(COPULA_PAST)) continue
                    if (rest.endsWith(form) && at + form.length == rest.length && follows(last, sound)) out += pieces + form
                }
            }
            for (particle in PARTICLES) {
                if (!rest.startsWith(particle.form, at) || !follows(last, particle.sound)) continue
                if (particle.form == "가" && pieces.isEmpty() && stem in NOT_BEFORE_GA) continue
                if (particle.form == PER && (pieces.isNotEmpty() || stem !in COUNTERS)) continue
                // Two particles of place and manner may stack (에서부터, 까지만); otherwise each comes later than the last.
                if (particle.slot > slot || (particle.slot == slot && slot == 1 && place == 1)) {
                    go(at + particle.form.length, particle.form.last(), particle.slot, pieces + particle.form, place + if (particle.slot == 1) 1 else 0)
                }
            }
        }

        go(0, stem.last(), -1, emptyList(), 0)
        return out
    }

    /** Whether a particle that follows [sound] may come after the syllable [previous]. */
    private fun follows(previous: Char, sound: Sound): Boolean {
        val code = previous.code - 0xAC00
        if (code !in 0 until 11172) return false
        val final = code % 28
        return when (sound) {
            Sound.ANY -> true
            Sound.CONSONANT -> final != 0
            Sound.VOWEL -> final == 0
            Sound.VOWEL_OR_L -> final == 0 || final == FINAL_L
            Sound.CONSONANT_NOT_L -> final != 0 && final != FINAL_L
        }
    }

    /** The sound a particle follows. */
    private enum class Sound { ANY, CONSONANT, VOWEL, VOWEL_OR_L, CONSONANT_NOT_L }

    /** A particle, with the sound it follows and its place in a stack. */
    private class Particle(val form: String, val sound: Sound, val slot: Int)

    private companion object {
        const val CODE = "ko"
        const val WORD_COST = 4.0
        const val UNRANKED = 50_000
        const val STACK = 4
        const val FINAL_L = 8

        /**
         * The polite 요, after a particle only (학교에서요): after a verb it is part of the verb (먹었어요). Nor
         * does it follow 와 or 과, which are of writing: 도와요 and 나와요 are forms of 돕다 and 나오다.
         */
        const val POLITE = "요"
        val NOT_BEFORE_POLITE = setOf("와", "과")

        /** 당 "per", right after a counter only (시간당, 개당; 부당 "unjust" is no 부 + 당). */
        const val PER = "당"

        /**
         * The copula's past after a vowel (였어요) is no copula after a stem that makes a verb in 이다
         * (모이다): 모였어요 is 모이다's.
         */
        const val COPULA_STEM = "이다"
        const val COPULA_PAST = "였"

        /** The honorific 님 after a word (사장님께 is 사장님 + 께 though the lexicon lacks 사장님). */
        const val HONORIFIC = "님"

        /** Number words before a counter (세 명, 열한 시, 여든 살), which stay whole: 여든 is no 여 + 든. */
        val NATIVE_NUMBER = Regex("(?:열|스물|스무|서른|마흔|쉰|예순|일흔|여든|아흔)(?:한|두|세|석|네|넉|다섯|여섯|일곱|여덟|아홉)?|한|두|세|석|네|넉|다섯|여섯|일곱|여덟|아홉")

        /**
         * Sino-Korean numbers (오 분, 만 원, 삼십 분), digits between their tens, hundreds, thousands and ten
         * thousands; but not 이 "this", 사 or 구 alone, which are more often words.
         */
        val SINO_NUMBER = Regex("(?:[이삼사오육칠팔구]?[십백천만])+[일이삼사오육칠팔구]?|[일삼오육칠팔]")

        const val HOW_MANY = "몇"

        /** The counters a number comes before. */
        val COUNTERS = setOf(
            "개", "명", "분", "시", "시간", "살", "번", "마리", "잔", "병", "권", "장", "대", "층", "원", "년", "월", "일", "주",
            "주일", "주년", "개월", "달", "가지", "그릇", "벌", "켤레", "송이", "번째", "째", "쪽", "초", "세", "인분", "칸", "줄",
            "통", "곡", "편", "채", "군데", "학년", "반", "호", "점", "퍼센트", "도", "배", "박", "박스", "봉지", "상자", "조각",
            "컵", "접시", "바퀴", "정거장", "킬로", "킬로미터", "미터", "킬로그램", "그램", "리터", "센티미터", "호선", "등", "사람",
            "인", "건", "회",
        )

        /** Words of time before a unit of time (다음 주, 지난 달, 이번 해), and those units. */
        val TIME_WORDS = setOf("다음", "이번", "지난", "저번", "매")
        val TIME_COUNTERS = setOf("주", "달", "해", "번")

        /** 가 does not follow these: before it they are 내, 제 (내가), so 나가 is a form of 나가다. */
        val NOT_BEFORE_GA = setOf("나", "저")

        /** Nor does the copula follow 아니, being 아니다's own: 아니라고 is a form of 아니다. */
        val NOT_BEFORE_COPULA = setOf("아니")

        /**
         * The particles, by their place in a stack (집에도 is 집 + 에 + 도): the plural 들 first, then
         * particles of place, manner and company (two may stack: 에서부터), then 도, 은/는 and their kind,
         * then the case endings 이/가, 을/를, 의.
         */
        val PARTICLES: List<Particle> = listOf(
            Particle("들", Sound.ANY, 0),
        ) + listOf(
            "에" to Sound.ANY, "에서" to Sound.ANY, "에게" to Sound.ANY, "에게서" to Sound.ANY, "한테" to Sound.ANY,
            "한테서" to Sound.ANY, "께" to Sound.ANY, "께서" to Sound.ANY, "로" to Sound.VOWEL_OR_L, "으로" to Sound.CONSONANT_NOT_L,
            "로서" to Sound.VOWEL_OR_L, "으로서" to Sound.CONSONANT_NOT_L, "로써" to Sound.VOWEL_OR_L, "으로써" to Sound.CONSONANT_NOT_L,
            "와" to Sound.VOWEL, "과" to Sound.CONSONANT, "하고" to Sound.ANY, "랑" to Sound.VOWEL, "이랑" to Sound.CONSONANT,
            "처럼" to Sound.ANY, "보다" to Sound.ANY, "까지" to Sound.ANY, "부터" to Sound.ANY, "마다" to Sound.ANY,
            "만큼" to Sound.ANY, "대로" to Sound.ANY, "만" to Sound.ANY, "밖에" to Sound.ANY, "뿐" to Sound.ANY,
            "조차" to Sound.ANY, "마저" to Sound.ANY, PER to Sound.ANY,
        ).map { (form, sound) -> Particle(form, sound, 1) } + listOf(
            "도" to Sound.ANY, "은" to Sound.CONSONANT, "는" to Sound.VOWEL, "이나" to Sound.CONSONANT, "나" to Sound.VOWEL,
            "이라도" to Sound.CONSONANT, "라도" to Sound.VOWEL, "이든" to Sound.CONSONANT, "든" to Sound.VOWEL,
            "이든지" to Sound.CONSONANT, "든지" to Sound.VOWEL,
        ).map { (form, sound) -> Particle(form, sound, 2) } + listOf(
            "이" to Sound.CONSONANT, "가" to Sound.VOWEL, "을" to Sound.CONSONANT, "를" to Sound.VOWEL, "의" to Sound.ANY,
        ).map { (form, sound) -> Particle(form, sound, 3) }

        /**
         * The copula 이다 after a noun (학생이에요, 의사예요), last in a stack and after no particle but those of
         * place and manner (때문이에요, 뿐이에요). After a vowel its 이 may drop (의사였어요), not in the forms
         * that would read as another word (다, 고, 면).
         */
        val COPULA: List<Pair<String, Sound>> = listOf(
            "이다", "입니다", "입니까", "이었다", "이었어", "이었어요", "이었습니다", "이었던", "이고", "이지만", "이라고", "이라서",
            "이라면", "이라는", "이란", "인데", "인데요", "인지", "인가", "인가요", "일까", "일까요", "이죠", "이지", "이지요", "이니까",
            "이네요", "이며", "인",
        ).map { it to Sound.ANY } + listOf("이에요" to Sound.CONSONANT, "이야" to Sound.CONSONANT) + listOf(
            "예요", "야", "였다", "였어", "였어요", "였습니다", "였던", "지만", "라고", "라서", "라면", "라는", "란", "죠", "니까", "네요",
        ).map { it to Sound.VOWEL }
    }
}
