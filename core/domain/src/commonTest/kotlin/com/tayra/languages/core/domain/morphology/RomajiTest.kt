package com.tayra.languages.core.domain.morphology

import kotlin.test.Test
import kotlin.test.assertEquals

class RomajiTest {
    @Test
    fun kanaIsWrittenInHepburn() {
        assertEquals(
            listOf("shinbun", "kitto", "matcha", "kin'en", "kon'ya", "toukyou", "raamen", "chotto", "fan", "jishou", "tsukue", "rokku"),
            listOf("しんぶん", "きっと", "まっちゃ", "きんえん", "こんや", "とうきょう", "ラーメン", "ちょっと", "ファン", "じしょう", "つくえ", "ロック").map(Romaji::of),
        )
    }

    @Test
    fun whatIsNoKanaIsKept() {
        assertEquals("tabemasu。OK", Romaji.of("たべます。OK"))
    }
}
