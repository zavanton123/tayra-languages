package com.tayra.languages.core.domain.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranscriptCuesTest {
    private fun words(vararg w: Triple<Long, Long, String>) = w.map { HeardWord(it.first, it.second, it.third) }

    @Test
    fun aSegmentOfTwoSentencesBecomesTwoLines() {
        val segment = HeardSegment(
            0, 4_000, " O coelho acorda. Ele vê uma borboleta.",
            words(
                Triple(0, 400, " O"), Triple(400, 900, " coelho"), Triple(900, 1_500, " acorda."),
                Triple(1_600, 1_900, " Ele"), Triple(1_900, 2_200, " vê"), Triple(2_200, 2_500, " uma"), Triple(2_500, 4_000, " borboleta."),
            ),
        )
        val cues = TranscriptCues.of(listOf(segment))
        assertEquals(listOf("O coelho acorda.", "Ele vê uma borboleta."), cues.map { it.text })
        assertEquals(listOf(0L to 1_500L, 1_600L to 4_000L), cues.map { it.beginMs to it.endMs })
    }

    @Test
    fun aShortSentenceStaysWithTheNextAndAPauseEndsALine() {
        val segment = HeardSegment(
            0, 5_000, "",
            words(Triple(0, 300, " Sim."), Triple(300, 800, " Claro"), Triple(800, 1_200, " que"), Triple(2_200, 2_600, " vou"), Triple(2_600, 3_000, " lá")),
        )
        assertEquals(listOf("Sim. Claro que", "vou lá"), TranscriptCues.of(listOf(segment)).map { it.text })
    }

    @Test
    fun aLongLineEndsAtACommaAndALongerOneAnywhere() {
        val long = (0 until 30).map { i -> HeardWord(i * 500L, i * 500L + 450, if (i == 14) " vez," else " palavra") }
        val cues = TranscriptCues.of(listOf(HeardSegment(0, 15_000, "", long)))
        assertEquals(" vez,".trim(), cues.first().text.split(' ').last())
        assertEquals(7_450L, cues.first().endMs)
        cues.forEach { assertTrue(it.durationMs <= TranscriptCues.LONGEST_MS, "too long: $it") }
    }

    @Test
    fun withoutWordTimesALongSentenceIsCutAtCommasAndWhereItMust() {
        val text = (1..12).joinToString(", ") { "as mulheres que cantam na rua" } + " e " + List(40) { "palavra" }.joinToString(" ") + "."
        val cues = TranscriptCues.of(listOf(HeardSegment(5_000, 65_000, text)))
        assertTrue(cues.size > 3, "still long: $cues")
        cues.forEach { assertTrue(it.durationMs <= TranscriptCues.LONGEST_MS, "too long: $it") }
        assertTrue(cues.first().text.endsWith(","), "not at a comma: ${cues.first().text}")
        assertEquals(text, cues.joinToString(" ") { it.text })
        assertEquals(5_000L, cues.first().beginMs)
        assertEquals(65_000L, cues.last().endMs)
        cues.zipWithNext().forEach { (a, b) -> assertEquals(a.endMs, b.beginMs) }
    }

    @Test
    fun withoutWordTimesASegmentIsSplitAtItsSentencesByLength() {
        val cues = TranscriptCues.of(listOf(HeardSegment(10_000, 13_000, "Bom dia. Tudo bem com você?")))
        assertEquals(listOf("Bom dia.", "Tudo bem com você?"), cues.map { it.text })
        assertEquals(10_000L, cues[0].beginMs)
        assertEquals(cues[0].endMs, cues[1].beginMs)
        assertEquals(13_000L, cues[1].endMs)
        assertNull(cues[0].translation)
    }

    @Test
    fun emptySegmentsAreDroppedAndLinesComeInTimeOrder() {
        val cues = TranscriptCues.of(listOf(HeardSegment(5_000, 6_000, "Depois."), HeardSegment(3_000, 4_000, "  "), HeardSegment(0, 1_000, "Antes.")))
        assertEquals(listOf("Antes.", "Depois."), cues.map { it.text })
    }

    @Test
    fun whisperCodesFollowTheAppsLanguages() {
        assertEquals("pt", WhisperLanguages.code("pt"))
        assertEquals("no", WhisperLanguages.code("nb"))
        assertEquals("zh", WhisperLanguages.code("zh-TW"))
        assertNull(WhisperLanguages.code("grc"))
    }
}
