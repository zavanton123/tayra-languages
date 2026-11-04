package com.tayra.languages.core.domain.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubtitleFormatsTest {

    @Test
    fun readsSrtWithIndexesWindowsLineEndsAndABom() {
        val srt = "﻿1\r\n00:00:01,000 --> 00:00:02,500\r\nOlá, tudo bem?\r\n\r\n2\r\n00:00:03,100 --> 00:00:05,000\r\n- Tudo.\r\n- E você?\r\n\r\n"
        val cues = SubtitleFormats.parse(srt)
        assertEquals(SubtitleFormat.SRT, SubtitleFormat.sniff(srt.removePrefix("﻿")))
        assertEquals(listOf(Cue(1000, 2500, "Olá, tudo bem?"), Cue(3100, 5000, "- Tudo.\n- E você?")), cues)
    }

    @Test
    fun readsWebVttWithShortTimesSettingsAndNotes() {
        val vtt = """
            WEBVTT
            Kind: captions

            NOTE a comment
            spanning lines

            intro
            00:01.000 --> 00:02.000 align:start position:0%
            <v Ana>Bom dia</v>

            01:00:00.250 --> 01:00:01.000
            Até logo
        """.trimIndent()
        val cues = SubtitleFormats.parse(vtt)
        assertEquals(listOf(Cue(1000, 2000, "<v Ana>Bom dia</v>"), Cue(3_600_250, 3_601_000, "Até logo")), cues)
    }

    @Test
    fun readsAssDialoguesWithCommasInTheTextAndCustomFieldOrder() {
        val ass = """
            [Script Info]
            Title: test

            [V4+ Styles]
            Format: Name, Fontname
            Style: Default,Arial

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Comment: 0,0:00:00.00,0:00:01.00,Default,,0,0,0,,ignored
            Dialogue: 0,0:00:01.50,0:00:03.00,Default,,0,0,0,,{\i1}Sim{\i0}, claro,\Nmeu amigo
        """.trimIndent()
        val cues = SubtitleFormats.parse(ass)
        assertEquals(listOf(Cue(1500, 3000, "{\\i1}Sim{\\i0}, claro,\\Nmeu amigo")), cues)
        assertEquals("Sim, claro, meu amigo", CueNormalizer.cleanText(cues.single().text))
    }

    @Test
    fun readsYoutubeJson3AndEndsRollingWindowsWhereTheNextBegins() {
        val json = """
            {"wireMagic":"pb3","events":[
              {"tStartMs":0,"dDurationMs":4000,"id":1,"wWinId":1,"segs":[{"utf8":"eu"},{"utf8":" acho","tOffsetMs":400}]},
              {"tStartMs":2000,"dDurationMs":10,"aAppend":1,"segs":[{"utf8":"\n"}]},
              {"tStartMs":2500,"dDurationMs":3000,"segs":[{"utf8":"que sim"}]},
              {"tStartMs":9000,"dDurationMs":1000}
            ]}
        """.trimIndent()
        assertEquals(SubtitleFormat.YOUTUBE_JSON, SubtitleFormat.sniff(json))
        assertEquals(listOf(Cue(0, 2500, "eu acho"), Cue(2500, 5500, "que sim")), SubtitleFormats.parse(json))
    }

    @Test
    fun readsClockTimesOfEveryFormat() {
        assertEquals(3_723_456, SubtitleFormats.clockMs("01:02:03,456"))
        assertEquals(123_456, SubtitleFormats.clockMs("02:03.456"))
        assertEquals(3_723_450, SubtitleFormats.clockMs("1:02:03.45"))
        assertEquals(5_000, SubtitleFormats.clockMs("00:00:05"))
        assertNull(SubtitleFormats.clockMs("5"))
        assertNull(SubtitleFormats.clockMs("aa:bb"))
    }

    @Test
    fun unknownTextGivesNoCues() {
        assertEquals(emptyList(), SubtitleFormats.parse("just some words\nand more"))
    }
}
