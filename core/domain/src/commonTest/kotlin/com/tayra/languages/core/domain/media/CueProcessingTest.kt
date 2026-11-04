package com.tayra.languages.core.domain.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CueProcessingTest {

    @Test
    fun cleaningDropsMarkupSoundTagsAndSpeakerDashes() {
        assertEquals("Tudo. E você?", CueNormalizer.cleanText("- Tudo.\n- E você?"))
        assertEquals("Bom dia", CueNormalizer.cleanText("<v Ana><i>Bom</i> <00:00:01.500>dia</v>"))
        assertEquals("Vamos!", CueNormalizer.cleanText("[Música] ♪ Vamos! (risos)"))
        assertEquals("(risos) Vamos!", CueNormalizer.cleanText("(risos) Vamos!", dropSoundTags = false))
        assertEquals("Tom & Jerry", CueNormalizer.cleanText("Tom &amp; Jerry"))
    }

    @Test
    fun normalizingDropsEmptyCuesFoldsRepeatsAndEndsOverlaps() {
        val cues = listOf(
            Cue(0, 1000, "[Música]"),
            Cue(1000, 3000, "Olá"),
            Cue(3000, 4000, "Olá"),
            Cue(3500, 6000, "Tudo bem?"),
            Cue(5000, 7000, "Sim."),
        )
        assertEquals(
            listOf(Cue(1000, 3500, "Olá"), Cue(3500, 5000, "Tudo bem?"), Cue(5000, 7000, "Sim.")),
            CueNormalizer.normalize(cues),
        )
    }

    @Test
    fun fragmentsJoinUntilASentenceEndsAPauseOrTheLengthCap() {
        val cues = listOf(
            Cue(0, 500, "eu acho"),
            Cue(500, 1000, "que sim."),
            Cue(1000, 1500, "mas talvez"),
            Cue(3000, 3500, "não"),
            Cue(3500, 4000, "seja assim"),
        )
        assertEquals(
            listOf(Cue(0, 1000, "eu acho que sim."), Cue(1000, 1500, "mas talvez"), Cue(3000, 4000, "não seja assim")),
            CueNormalizer.mergeFragments(cues),
        )
        assertEquals(3, CueNormalizer.mergeFragments(listOf(Cue(0, 1, "aaaa"), Cue(1, 2, "bbbb"), Cue(2, 3, "cccc")), maxChars = 6).size)
    }

    @Test
    fun captionsWithoutPunctuationAreRecognised() {
        assertTrue(CueNormalizer.looksUnpunctuated(List(10) { Cue(it * 1000L, it * 1000L + 900, "palavra $it") }))
        assertFalse(CueNormalizer.looksUnpunctuated(List(10) { Cue(it * 1000L, it * 1000L + 900, "Frase $it.") }))
    }

    @Test
    fun translationsGoToTheLineTheyOverlapMost() {
        val original = listOf(Cue(0, 2000, "Olá."), Cue(2000, 4000, "Tudo bem?"), Cue(4000, 6000, "Sim."), Cue(9000, 9500, "Fim."))
        val translation = listOf(
            Cue(100, 1900, "Hello."),
            Cue(1800, 3000, "How"),
            Cue(3000, 3900, "are you?"),
            Cue(6500, 8000, "Nothing here."),
            Cue(9000, 9200, "End."),
        )
        val aligned = CueAlignment.align(original, translation)
        assertEquals(listOf("Hello.", "How are you?", null, "End."), aligned.map { it.translation })
    }

    @Test
    fun translationsByIndexNeedTheSameNumberOfLines() {
        val original = listOf(Cue(0, 1, "a"), Cue(1, 2, "b"))
        assertEquals(listOf("A", null), CueAlignment.byIndex(original, listOf("A", " "))!!.map { it.translation })
        assertNull(CueAlignment.byIndex(original, listOf("A")))
    }

    @Test
    fun youtubeIdsAreFoundInEveryCommonLink() {
        val id = "dQw4w9WgXcQ"
        listOf(
            id,
            "https://www.youtube.com/watch?v=$id",
            "https://www.youtube.com/watch?feature=share&v=$id&t=42s",
            "youtube.com/watch?v=$id",
            "https://youtu.be/$id?si=abc",
            "https://m.youtube.com/watch?v=$id",
            "https://www.youtube.com/shorts/$id",
            "https://www.youtube.com/embed/$id?start=3",
            "https://www.youtube-nocookie.com/embed/$id",
            "https://www.youtube.com/live/$id",
            "https://music.youtube.com/watch?v=$id&list=x",
        ).forEach { assertEquals(id, YouTubeIds.parse(it), it) }
        assertNull(YouTubeIds.parse("https://vimeo.com/123456"))
        assertNull(YouTubeIds.parse("https://www.youtube.com/@channel"))
        assertEquals(id, YouTubeIds.ofUri(YouTubeIds.uri(id)))
        assertNull(YouTubeIds.ofUri("file:///a.mp4"))
    }
}
