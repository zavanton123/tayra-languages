package com.tayra.languages.core.domain.media

/**
 * Turns parsed cues into the lines a transcript shows: markup gone, one line of text per cue, no
 * empty or repeated cues, and (for automatic captions) fragments joined into sentence-sized lines.
 */
object CueNormalizer {

    private val HTML_TAG = Regex("""</?[a-zA-Z][^>]*>""")
    private val VTT_TIMESTAMP = Regex("""<\d{1,2}(:\d{2}){1,2}\.\d{1,3}>""")
    private val ASS_OVERRIDE = Regex("""\{[^}]*\}""")
    private val SOUND_TAG = Regex("""\[[^\]]*\]|\([^)]*\)|[♪♫]+""")
    private val LEADING_DASH = Regex("""^\s*[-–—]\s*""")
    private val SPACES = Regex("""[ \t ]+""")
    private val SENTENCE_END = Regex("""[.!?…。！？]["'»”)\]]*$""")

    /** The text of one cue as a single clean line. */
    fun cleanText(text: String, dropSoundTags: Boolean = true): String {
        var t = text
            .replace("\\N", "\n").replace("\\n", "\n").replace("\\h", " ")
            .replace(ASS_OVERRIDE, "")
            .replace(VTT_TIMESTAMP, "")
            .replace(HTML_TAG, "")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ").replace("&#39;", "'").replace("&quot;", "\"")
        if (dropSoundTags) t = t.replace(SOUND_TAG, " ")
        return t.lines()
            .map { it.replace(LEADING_DASH, "").replace(SPACES, " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }

    /**
     * Clean cues in order of time: markup removed, empty cues dropped, a cue that repeats the one
     * before (rolling captions) folded into it, and a cue overlapping the next ended where it begins.
     */
    fun normalize(cues: List<Cue>, dropSoundTags: Boolean = true): List<Cue> {
        val out = mutableListOf<Cue>()
        for (cue in cues.sortedBy { it.beginMs }) {
            val text = cleanText(cue.text, dropSoundTags)
            if (text.isEmpty() || cue.endMs < cue.beginMs) continue
            val last = out.lastOrNull()
            if (last != null && last.text == text && cue.beginMs - last.endMs <= 1000) {
                out[out.lastIndex] = last.copy(endMs = maxOf(last.endMs, cue.endMs))
                continue
            }
            out += cue.copy(text = text, translation = cue.translation?.let { cleanText(it, dropSoundTags) }?.ifEmpty { null })
        }
        return out.mapIndexed { i, cue ->
            val next = out.getOrNull(i + 1)
            if (next != null && cue.endMs > next.beginMs && next.beginMs > cue.beginMs) cue.copy(endMs = next.beginMs) else cue
        }
    }

    /**
     * Joins the short pieces automatic captions come in into lines that end at a sentence end, a
     * pause longer than [maxGapMs], or once [maxChars] would be passed.
     */
    fun mergeFragments(cues: List<Cue>, maxGapMs: Long = 700, maxChars: Int = 110): List<Cue> {
        val out = mutableListOf<Cue>()
        var current: Cue? = null
        for (cue in cues) {
            val open = current
            current = when {
                open == null -> cue
                SENTENCE_END.containsMatchIn(open.text) ||
                    cue.beginMs - open.endMs > maxGapMs ||
                    open.text.length + 1 + cue.text.length > maxChars -> {
                    out += open
                    cue
                }
                else -> open.copy(
                    endMs = maxOf(open.endMs, cue.endMs),
                    text = open.text + " " + cue.text,
                    translation = listOfNotNull(open.translation, cue.translation).joinToString(" ").ifEmpty { null },
                )
            }
        }
        current?.let { out += it }
        return out
    }

    /** Whether [cues] look like automatic captions: no sentence punctuation at all in most lines. */
    fun looksUnpunctuated(cues: List<Cue>): Boolean {
        if (cues.size < 4) return false
        val ended = cues.count { SENTENCE_END.containsMatchIn(it.text) }
        return ended * 5 < cues.size
    }
}
