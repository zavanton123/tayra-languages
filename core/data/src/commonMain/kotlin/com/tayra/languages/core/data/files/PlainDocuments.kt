package com.tayra.languages.core.data.files

import kotlin.io.encoding.Base64

/**
 * The prose of a Markdown file: headings, paragraphs, list items, quotes and table cells without
 * their markup; code, pictures and link targets left out. Lines wrapped inside one paragraph are
 * joined, since the app reads every line as a paragraph of its own.
 */
internal object MarkdownText {

    fun text(markdown: String): String {
        val out = TextBuilder()
        var lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        // Front matter (YAML between --- lines at the very start) is the file's settings.
        if (lines.firstOrNull()?.trim() == "---") {
            val end = lines.drop(1).indexOfFirst { it.trim() == "---" || it.trim() == "..." }
            if (end >= 0) lines = lines.drop(end + 2)
        }
        var fence: String? = null
        var paragraph = false
        lines.forEachIndexed { index, raw ->
            val line = raw.trimEnd()
            val trimmed = line.trim()
            fence?.let { open ->
                if (trimmed.startsWith(open)) fence = null
                return@forEachIndexed
            }
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                fence = trimmed.take(3)
                out.breakLine(); paragraph = false
                return@forEachIndexed
            }
            val next = lines.getOrNull(index + 1)?.trim().orEmpty()
            when {
                trimmed.isEmpty() -> { out.breakLine(); paragraph = false }
                referenceDefinition.matches(trimmed) -> Unit
                rule.matches(trimmed) -> { out.blankLine(); paragraph = false }
                // A setext heading's underline; the heading itself was its line above.
                setextUnderline.matches(trimmed) && !paragraph -> Unit
                heading.containsMatchIn(trimmed) -> {
                    out.blankLine()
                    out.text(inline(trimmed.trimStart('#').trimEnd('#').trim()))
                    out.breakLine(); paragraph = false
                }
                tableSeparator.matches(trimmed) -> Unit
                trimmed.startsWith("|") || (trimmed.count { it == '|' } >= 2 && tableSeparator.matches(next)) -> {
                    out.breakLine()
                    trimmed.trim('|').split('|').forEach { cell -> out.text(inline(cell.trim())); out.space() }
                    out.breakLine(); paragraph = false
                }
                else -> {
                    var content = trimmed
                    var block = false
                    while (content.startsWith(">")) { content = content.drop(1).trimStart(); block = true }
                    listMarker.find(content)?.let { content = content.substring(it.range.last + 1); block = true }
                    content = content.removePrefix("[ ] ").removePrefix("[x] ").removePrefix("[X] ")
                    if (block || setextUnderline.matches(next)) { out.breakLine(); paragraph = false }
                    out.text(inline(content))
                    out.wrap()
                    paragraph = true
                    // Two trailing spaces or a backslash break the line by hand.
                    if (raw.endsWith("  ") || line.endsWith("\\") || setextUnderline.matches(next)) { out.breakLine(); paragraph = false }
                }
            }
        }
        return out.build()
    }

    /** One line's text without its inline markup. */
    private fun inline(text: String): String {
        var s = text
        s = image.replace(s, "")
        s = link.replace(s) { it.groupValues[1] }
        s = referenceLink.replace(s) { it.groupValues[1] }
        s = autolink.replace(s, "")
        s = code.replace(s) { it.groupValues[1] }
        s = strong.replace(s) { it.groupValues[2] }
        s = emphasis.replace(s) { it.groupValues[2] }
        s = strike.replace(s) { it.groupValues[1] }
        s = htmlTag.replace(s, "")
        s = escape.replace(s) { it.groupValues[1] }
        return Markup.decodeEntities(s.removeSuffix("\\"))
    }

    private val heading = Regex("""^#{1,6}(\s|$)""")
    private val referenceDefinition = Regex("""^\[[^\]]+]:\s*\S+.*$""")
    private val rule = Regex("""^([-*_])(\s*\1){2,}$""")
    private val setextUnderline = Regex("""^(=+|-+)$""")
    private val tableSeparator = Regex("""^\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?$""")
    private val listMarker = Regex("""^([-*+]|\d{1,9}[.)])\s+""")
    private val image = Regex("""!\[[^\]]*]\([^)]*\)|!\[[^\]]*]\[[^\]]*]""")
    private val link = Regex("""\[([^\]]*)]\([^)]*\)""")
    private val referenceLink = Regex("""\[([^\]]+)]\[[^\]]*]""")
    private val autolink = Regex("""<(https?://|mailto:)[^>\s]+>""")
    private val code = Regex("""`+([^`]+)`+""")
    private val strong = Regex("""(\*\*|__)(?=\S)(.+?)(?<=\S)\1""")
    private val emphasis = Regex("""(?<![\p{L}\p{N}\\])([*_])(?=\S)(.+?)(?<=\S)\1(?![\p{L}\p{N}])""")
    private val strike = Regex("""~~(?=\S)(.+?)(?<=\S)~~""")
    private val htmlTag = Regex("""</?[A-Za-z][^>]*>""")
    private val escape = Regex("""\\([\\`*_{}\[\]()#+\-.!|>~])""")
}

/**
 * The page of a saved web page (mhtml, mht): the HTML part of its MIME archive, decoded from
 * quoted-printable or base64 in the charset it names, read as HTML.
 */
internal object MhtmlText {

    fun read(bytes: ByteArray): ExtractedText {
        val raw = latin1(bytes)
        val (headers, body) = split(raw)
        val type = header(headers, "content-type").orEmpty()
        val html = if (type.startsWith("multipart/", ignoreCase = true)) {
            val boundary = parameter(type, "boundary") ?: throw FileImportException("Could not read the web page: it is not a valid MHTML file")
            body.split("--$boundary").asSequence()
                .map { split(it.trimStart('\r', '\n')) }
                .firstOrNull { (partHeaders, _) -> header(partHeaders, "content-type")?.startsWith("text/html", ignoreCase = true) == true }
                ?.let { (partHeaders, partBody) -> decodePart(partHeaders, partBody) }
                ?: throw FileImportException("Could not read the web page: it has no HTML page")
        } else {
            decodePart(headers, body)
        }
        val text = HtmlText.text(html)
        if (text.isEmpty()) throw FileImportException("Could not read the web page: it has no text")
        return ExtractedText(text, HtmlText.title(html) ?: header(headers, "subject"))
    }

    private fun decodePart(headers: String, body: String): String {
        val encoding = header(headers, "content-transfer-encoding")?.lowercase()?.trim()
        val bytes = when (encoding) {
            "quoted-printable" -> quotedPrintable(body)
            "base64" -> Base64.decode(body.filterNot { it.isWhitespace() })
            else -> body.map { it.code.toByte() }.toByteArray()
        }
        return TextDecoding.decode(bytes, header(headers, "content-type")?.let { parameter(it, "charset") })
    }

    /** Headers and body of a MIME entity, apart at the first empty line. */
    private fun split(entity: String): Pair<String, String> {
        val text = entity.replace("\r\n", "\n")
        val end = text.indexOf("\n\n")
        return if (end < 0) text to "" else text.substring(0, end) to text.substring(end + 2)
    }

    /** A header's value, its folded continuation lines joined. */
    private fun header(headers: String, name: String): String? {
        val unfolded = headers.replace(Regex("\n[ \t]+"), " ")
        return unfolded.lines().firstOrNull { it.startsWith("$name:", ignoreCase = true) }?.substringAfter(':')?.trim()
    }

    private fun parameter(value: String, name: String): String? =
        Regex("""(?i)\b$name\s*=\s*(?:"([^"]*)"|([^;\s]+))""").find(value)?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }

    private fun quotedPrintable(text: String): ByteArray {
        val out = ArrayList<Byte>(text.length)
        var i = 0
        val source = text.replace("\r\n", "\n")
        while (i < source.length) {
            val c = source[i]
            if (c == '=') {
                if (i + 1 < source.length && source[i + 1] == '\n') { i += 2; continue }
                val code = if (i + 2 < source.length) source.substring(i + 1, i + 3).toIntOrNull(16) else null
                if (code != null) { out += code.toByte(); i += 3; continue }
            }
            out += c.code.toByte()
            i++
        }
        return out.toByteArray()
    }
}

/**
 * The text of a Rich Text Format document: its paragraphs, with the font and style tables,
 * pictures, headers, footers, notes and field codes left out. Characters come from \uN escapes, or
 * from \'hh bytes in the document's code page (windows-1251, 1252 and KOI8-R are known).
 */
internal object RtfText {

    fun read(bytes: ByteArray): ExtractedText {
        val source = latin1(bytes)
        if (!source.trimStart().startsWith("{\\rtf")) throw FileImportException("Could not read the document: it is not a valid RTF file")
        val out = TextBuilder()
        var codePage = "windows-1252"
        // Each group's state: whether its text is skipped, and how many characters follow \uN as its fallback.
        val skipped = ArrayDeque<Boolean>().apply { addLast(false) }
        val fallbacks = ArrayDeque<Int>().apply { addLast(1) }
        var pendingFallback = 0
        val bytesRun = ArrayList<Byte>()
        var groupStart = false
        var i = 0

        fun flushBytes() {
            if (bytesRun.isEmpty()) return
            if (!skipped.last()) out.text(TextDecoding.decode(bytesRun.toByteArray(), codePage))
            bytesRun.clear()
        }
        fun emit(text: String) {
            flushBytes()
            if (!skipped.last()) out.text(text)
        }

        while (i < source.length) {
            val c = source[i]
            when (c) {
                '{' -> {
                    flushBytes()
                    skipped.addLast(skipped.last())
                    fallbacks.addLast(fallbacks.last())
                    pendingFallback = 0
                    groupStart = true
                    i++
                    continue
                }
                '}' -> {
                    flushBytes()
                    if (skipped.size > 1) { skipped.removeLast(); fallbacks.removeLast() }
                    pendingFallback = 0
                    i++
                }
                '\\' -> {
                    val next = source.getOrNull(i + 1) ?: break
                    when {
                        next == '\'' -> {
                            val code = source.substring(i + 2, minOf(source.length, i + 4)).toIntOrNull(16)
                            i += 4
                            if (pendingFallback > 0) { pendingFallback--; continue }
                            if (code != null) bytesRun += code.toByte()
                        }
                        next == '*' -> {
                            // An ignorable destination: a group this reader need not understand.
                            skipped[skipped.lastIndex] = true
                            i += 2
                        }
                        next.isLetter() -> {
                            var end = i + 1
                            while (end < source.length && source[end].isLetter()) end++
                            val word = source.substring(i + 1, end)
                            var numberEnd = end
                            if (numberEnd < source.length && (source[numberEnd] == '-' || source[numberEnd].isDigit())) {
                                numberEnd++
                                while (numberEnd < source.length && source[numberEnd].isDigit()) numberEnd++
                            }
                            val parameter = source.substring(end, numberEnd).toIntOrNull()
                            i = if (numberEnd < source.length && source[numberEnd] == ' ') numberEnd + 1 else numberEnd
                            if (groupStart && word in DESTINATIONS) skipped[skipped.lastIndex] = true
                            when (word) {
                                "ansicpg" -> codePage = parameter?.let { if (it == 20866) "koi8-r" else "windows-$it" } ?: codePage
                                "uc" -> fallbacks[fallbacks.lastIndex] = parameter ?: 1
                                "u" -> parameter?.let {
                                    emit(Markup.codePointString(if (it < 0) it + 65536 else it))
                                    pendingFallback = fallbacks.last()
                                }
                                "par", "line", "row", "cell" -> { flushBytes(); if (!skipped.last()) { if (word == "cell") out.space() else out.breakLine() } }
                                "sect", "page" -> { flushBytes(); if (!skipped.last()) out.blankLine() }
                                "tab" -> emit(" ")
                                "emdash" -> emit("—")
                                "endash" -> emit("–")
                                "bullet" -> emit("•")
                                "lquote" -> emit("‘")
                                "rquote" -> emit("’")
                                "ldblquote" -> emit("“")
                                "rdblquote" -> emit("”")
                                "emspace", "enspace", "qmspace" -> emit(" ")
                            }
                            groupStart = false
                            continue
                        }
                        else -> {
                            // A control symbol: an escaped brace or backslash, a hard space or hyphen.
                            when (next) {
                                '{', '}', '\\' -> emit(next.toString())
                                '~' -> emit(" ")
                                '_' -> emit("-")
                                '\n', '\r' -> { flushBytes(); if (!skipped.last()) out.breakLine() }
                            }
                            i += 2
                        }
                    }
                }
                '\r', '\n' -> i++
                else -> {
                    when {
                        pendingFallback > 0 -> pendingFallback--
                        // A byte written as it is rather than as \'hh is in the code page all the same.
                        c.code >= 0x80 -> bytesRun += c.code.toByte()
                        else -> emit(c.toString())
                    }
                    i++
                }
            }
            groupStart = false
        }
        flushBytes()
        val text = out.build()
        if (text.isEmpty()) throw FileImportException("Could not read the document: it has no text")
        return ExtractedText(text, null)
    }

    /** Groups that hold no text of the document: tables, metadata, pictures, notes, field instructions. */
    private val DESTINATIONS = setOf(
        "fonttbl", "colortbl", "stylesheet", "info", "pict", "header", "headerl", "headerr", "headerf", "footer", "footerl",
        "footerr", "footerf", "footnote", "annotation", "fldinst", "object", "objdata", "themedata", "colorschememapping",
        "datastore", "latentstyles", "listtable", "listoverridetable", "rsidtbl", "generator", "xmlnstbl", "mmathPr",
        "filetbl", "revtbl", "pgdsctbl", "bkmkstart", "bkmkend", "nonshppict", "shpinst", "sp", "listtext", "pntext",
        "pntxta", "pntxtb", "atnid", "atnauthor", "xe", "tc", "ftnsep", "ftnsepc", "aftnsep", "aftnsepc", "wgrffmtfilter",
        "operator", "author", "title", "subject", "keywords", "comment", "doccomm", "company", "category",
    )
}

/** Bytes read one to a character, for formats whose structure is ASCII whatever their text's encoding. */
internal fun latin1(bytes: ByteArray): String {
    val chars = CharArray(bytes.size)
    for (i in bytes.indices) chars[i] = (bytes[i].toInt() and 0xFF).toChar()
    return chars.concatToString()
}
