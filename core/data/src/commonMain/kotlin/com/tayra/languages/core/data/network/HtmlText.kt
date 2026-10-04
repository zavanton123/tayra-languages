package com.tayra.languages.core.data.network

/** Plain text from HTML fragments, such as Wiktionary glosses, without a full parser. */
object HtmlText {
    private val tags = Regex("<[^>]+>")
    private val whitespace = Regex("\\s+")

    /** Strips tags and entities from an HTML fragment, collapsing whitespace. */
    fun toPlainText(fragment: String): String = clean(fragment)

    private fun clean(fragment: String): String = decodeEntities(tags.replace(fragment, " ")).replace(whitespace, " ").trim()

    private fun decodeEntities(text: String): String = text
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace(Regex("&#(\\d+);")) { m -> m.groupValues[1].toIntOrNull()?.let { codePointToString(it) } ?: m.value }
        .replace(Regex("&#x([0-9a-fA-F]+);")) { m -> m.groupValues[1].toIntOrNull(16)?.let { codePointToString(it) } ?: m.value }

    private fun codePointToString(codePoint: Int): String {
        if (codePoint < 0x10000) return codePoint.toChar().toString()
        val offset = codePoint - 0x10000
        val high = (0xD800 + (offset shr 10)).toChar()
        val low = (0xDC00 + (offset and 0x3FF)).toChar()
        return "$high$low"
    }
}
