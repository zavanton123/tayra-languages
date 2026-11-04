package com.tayra.languages.core.domain.media

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

enum class SubtitleFormat(val extensions: Set<String>) {
    SRT(setOf("srt")),
    VTT(setOf("vtt", "webvtt")),
    ASS(setOf("ass", "ssa")),
    YOUTUBE_JSON(setOf("json", "json3")),
    ;

    companion object {
        val allExtensions: Set<String> = entries.flatMap { it.extensions }.toSet()

        fun ofExtension(extension: String): SubtitleFormat? = entries.firstOrNull { extension.lowercase() in it.extensions }

        /** The format [text] is written in, judged by its first lines. */
        fun sniff(text: String): SubtitleFormat? {
            val head = text.trimStart('﻿', ' ', '\n', '\r', '\t').take(2000)
            return when {
                head.startsWith("WEBVTT") -> VTT
                head.startsWith("{") && "\"events\"" in head -> YOUTUBE_JSON
                head.startsWith("[Script Info]", ignoreCase = true) || "\nDialogue:" in head || "[Events]" in head -> ASS
                Regex("""\d{1,2}:\d{2}:\d{2}[,.]\d{1,3}\s*-->""").containsMatchIn(head) -> SRT
                Regex("""(\d{1,2}:)?\d{2}:\d{2}\.\d{1,3}\s*-->""").containsMatchIn(head) -> VTT
                else -> null
            }
        }
    }
}

/**
 * Reads subtitle files into cues. Markup is left in the text; [CueNormalizer] cleans it, so a parser
 * stays a plain reader of its format.
 */
object SubtitleFormats {

    /** The cues of [text], in the format given or else the one it looks like; empty when nothing is recognised. */
    fun parse(text: String, format: SubtitleFormat? = null): List<Cue> {
        val clean = text.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n')
        return when (format ?: SubtitleFormat.sniff(clean)) {
            SubtitleFormat.SRT -> parseSrt(clean)
            SubtitleFormat.VTT -> parseVtt(clean)
            SubtitleFormat.ASS -> parseAss(clean)
            SubtitleFormat.YOUTUBE_JSON -> parseYoutubeJson(clean)
            null -> emptyList()
        }.sortedBy { it.beginMs }
    }

    private val ARROW = Regex("""^\s*(\S+)\s*-->\s*(\S+)""")

    fun parseSrt(text: String): List<Cue> = blocks(text).mapNotNull { lines ->
        val timeAt = lines.indexOfFirst { "-->" in it }
        if (timeAt < 0) return@mapNotNull null
        val match = ARROW.find(lines[timeAt]) ?: return@mapNotNull null
        val begin = clockMs(match.groupValues[1]) ?: return@mapNotNull null
        val end = clockMs(match.groupValues[2]) ?: return@mapNotNull null
        Cue(begin, end, lines.drop(timeAt + 1).joinToString("\n"))
    }

    fun parseVtt(text: String): List<Cue> = blocks(text).mapNotNull { lines ->
        val first = lines.first().trim()
        if (first.startsWith("WEBVTT") || first.startsWith("NOTE") || first == "STYLE" || first == "REGION") {
            return@mapNotNull null
        }
        val timeAt = lines.indexOfFirst { "-->" in it }
        if (timeAt < 0 || timeAt > 1) return@mapNotNull null
        val match = ARROW.find(lines[timeAt]) ?: return@mapNotNull null
        val begin = clockMs(match.groupValues[1]) ?: return@mapNotNull null
        val end = clockMs(match.groupValues[2]) ?: return@mapNotNull null
        Cue(begin, end, lines.drop(timeAt + 1).joinToString("\n"))
    }

    fun parseAss(text: String): List<Cue> {
        var fields = listOf("Layer", "Start", "End", "Style", "Name", "MarginL", "MarginR", "MarginV", "Effect", "Text")
        var inEvents = false
        val cues = mutableListOf<Cue>()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("[")) {
                inEvents = line.equals("[Events]", ignoreCase = true)
                continue
            }
            if (!inEvents) continue
            when {
                line.startsWith("Format:", ignoreCase = true) ->
                    fields = line.substringAfter(':').split(',').map { it.trim() }
                line.startsWith("Dialogue:", ignoreCase = true) -> {
                    // The text is the last field and may itself hold commas.
                    val values = line.substringAfter(':').trimStart().split(',', limit = fields.size)
                    if (values.size < fields.size) continue
                    val begin = clockMs(values[fields.indexOfOrZero("Start")].trim()) ?: continue
                    val end = clockMs(values[fields.indexOfOrZero("End")].trim()) ?: continue
                    cues += Cue(begin, end, values.last())
                }
            }
        }
        return cues
    }

    /**
     * YouTube's "json3" timed text. Automatic captions come as word pieces with offsets inside
     * windows that roll on; events with only a line break between them are dropped.
     */
    fun parseYoutubeJson(text: String): List<Cue> {
        val root = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return emptyList()
        val events = root["events"] as? JsonArray ?: return emptyList()
        val raw = events.mapNotNull { element ->
            val event = element as? JsonObject ?: return@mapNotNull null
            val segs = event["segs"] as? JsonArray ?: return@mapNotNull null
            val start = event["tStartMs"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val duration = event["dDurationMs"]?.jsonPrimitive?.longOrNull ?: 0L
            val words = segs.joinToString("") { seg -> (seg as? JsonObject)?.get("utf8")?.jsonPrimitive?.content.orEmpty() }
            if (words.isBlank()) null else Cue(start, start + duration, words)
        }
        // Rolling automatic captions overlap the next window; a window ends where the next begins.
        return raw.mapIndexed { i, cue ->
            val next = raw.getOrNull(i + 1)
            if (next != null && next.beginMs in (cue.beginMs + 1) until cue.endMs) cue.copy(endMs = next.beginMs) else cue
        }
    }

    private fun List<String>.indexOfOrZero(name: String): Int = indexOfFirst { it.equals(name, ignoreCase = true) }.coerceAtLeast(0)

    private fun blocks(text: String): List<List<String>> {
        val out = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        for (line in text.lineSequence()) {
            if (line.isBlank()) {
                if (current.isNotEmpty()) out += current
                current = mutableListOf()
            } else {
                current += line
            }
        }
        if (current.isNotEmpty()) out += current
        return out
    }

    /**
     * Milliseconds of a clock time: "01:02:03,456" (SRT), "02:03.456" or "01:02:03.456" (WebVTT),
     * "1:02:03.45" (ASS, hundredths).
     */
    fun clockMs(value: String): Long? {
        val parts = value.trim().split(':')
        if (parts.size !in 2..3) return null
        val secondsPart = parts.last().replace(',', '.')
        val whole = secondsPart.substringBefore('.').toLongOrNull() ?: return null
        val fraction = secondsPart.substringAfter('.', "").takeIf { it.isNotEmpty() }
        val ms = when {
            fraction == null -> 0L
            fraction.all { it.isDigit() } -> (fraction + "00").take(3).toLong()
            else -> return null
        }
        val minutes = parts[parts.size - 2].toLongOrNull() ?: return null
        val hours = if (parts.size == 3) parts[0].toLongOrNull() ?: return null else 0L
        return ((hours * 60 + minutes) * 60 + whole) * 1000 + ms
    }
}
