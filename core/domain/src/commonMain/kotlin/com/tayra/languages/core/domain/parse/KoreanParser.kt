package com.tayra.languages.core.domain.parse

import kotlin.math.ln

/**
 * Korean: each space-separated word cut into the word and the particles after it (친구가 is 친구 + 가,
 * 집에도 is 집 + 에 + 도), which then read as words of their own, as Japanese particles do. Verbs and
 * adjectives stay whole (먹었어요): their endings are no particles, and lookup finds the verb.
 *
 * A word is cut only after a word the lexicon knows. Of the cuts and the word left whole, the one whose
 * pieces are together the most common wins, by the costs SegmentingParser gives Chinese words: a word the
 * frequency list has stays whole (사과; 나가, a form of 나가다), one it lacks is cut (나는 is 나 + 는,
 * though the dictionary also has it as a form of 날다).
 *
 * tools/build_frequency_list.py counts words cut the same way (korean_pieces); keep the two the same.
 */
class KoreanParser : SpaceDelimitedParser(), LexiconParser {

    override val name: String get() = "Korean"

    override fun words(word: String): List<String> = Lexicons[CODE]?.let { cut(word, it) } ?: listOf(word)

    fun cut(word: String, lexicon: Lexicon): List<String> {
        var best: Double? = null
        var cut = listOf(word)
        for (end in word.length - 1 downTo 1) {
            val stem = word.substring(0, end)
            if (stem !in lexicon) continue
            for (stack in stacks(word.substring(end), stem)) {
                val pieces = listOf(stem) + stack
                val cost = pieces.sumOf { ln((lexicon.rank(it) ?: UNRANKED).toDouble()) + WORD_COST }
                if (best == null || cost < best) {
                    best = cost
                    cut = pieces
                }
            }
        }
        if (best == null) return listOf(word)
        val whole = lexicon.rank(word) ?: if (word in lexicon) UNRANKED else null
        return if (whole != null && ln(whole.toDouble()) + WORD_COST <= best) listOf(word) else cut
    }

    /** Every way [rest] is a stack of particles after [stem]. */
    private fun stacks(rest: String, stem: String): List<List<String>> {
        val out = mutableListOf<List<String>>()

        fun go(at: Int, last: Char, slot: Int, pieces: List<String>, place: Int) {
            if (at == rest.length) {
                out += pieces
                return
            }
            if (pieces.size == STACK) return
            if (pieces.isNotEmpty() && rest.endsWith(POLITE) && at + POLITE.length == rest.length) out += pieces + POLITE
            if (slot <= 1 && (pieces.isNotEmpty() || stem !in NOT_BEFORE_COPULA)) {
                for ((form, sound) in COPULA) {
                    if (rest.endsWith(form) && at + form.length == rest.length && follows(last, sound)) out += pieces + form
                }
            }
            for (particle in PARTICLES) {
                if (!rest.startsWith(particle.form, at) || !follows(last, particle.sound)) continue
                if (particle.form == "가" && pieces.isEmpty() && stem in NOT_BEFORE_GA) continue
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

        /** The polite 요, after a particle only (학교에서요): after a verb it is part of the verb (먹었어요). */
        const val POLITE = "요"

        /** 가 does not follow these: before it they are 내, 네, 제 (내가), so 나가 is a form of 나가다. */
        val NOT_BEFORE_GA = setOf("나", "너", "저")

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
            "조차" to Sound.ANY, "마저" to Sound.ANY,
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
            "이라면", "이라는", "이란", "인데", "인데요", "인지", "인가", "인가요", "일까", "일까요", "이죠", "이지요", "이니까",
            "이네요", "이며", "인",
        ).map { it to Sound.ANY } + listOf("이에요" to Sound.CONSONANT, "이야" to Sound.CONSONANT) + listOf(
            "예요", "야", "였다", "였어", "였어요", "였습니다", "였던", "지만", "라고", "라서", "라면", "라는", "란", "죠", "니까", "네요",
        ).map { it to Sound.VOWEL }
    }
}
