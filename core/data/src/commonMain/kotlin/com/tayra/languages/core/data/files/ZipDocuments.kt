package com.tayra.languages.core.data.files

import com.tayra.languages.core.data.yomitan.ZipArchive
import com.tayra.languages.core.data.yomitan.ZipException

/** Opens a zip-based document (epub, docx, odt, fbz), naming it [kind] when the bytes are no zip. */
internal fun openZip(bytes: ByteArray, kind: String): ZipArchive = try {
    ZipArchive(bytes)
} catch (e: ZipException) {
    throw FileImportException("Could not read the file: it is not a valid $kind file", e)
}

/** The text of the entry [name], decoded by the encoding its XML declares; null when there is no such entry. */
internal suspend fun ZipArchive.xmlEntry(name: String): String? {
    val entry = entry(name) ?: entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return null
    val bytes = read(entry)
    return TextDecoding.decode(bytes, TextDecoding.xmlEncoding(bytes))
}

/** The text of the element [name] ("dc:title"), its markup and entities resolved; null when absent or empty. */
internal fun elementText(xml: String, name: String): String? {
    val tokens = Markup.tokens(xml).iterator()
    while (tokens.hasNext()) {
        val token = tokens.next()
        if (token is MarkupToken.Open && token.name == name && !token.selfClosing) {
            val text = StringBuilder()
            var depth = 1
            while (tokens.hasNext() && depth > 0) {
                when (val inner = tokens.next()) {
                    is MarkupToken.Open -> if (inner.name == name && !inner.selfClosing) depth++
                    is MarkupToken.Close -> if (inner.name == name) depth--
                    is MarkupToken.Text -> text.append(inner.text)
                }
            }
            return text.toString().replace(Regex("\\s+"), " ").trim().takeIf { it.isNotEmpty() }
        }
    }
    return null
}

/** The text of an EPUB: its documents in reading order (the spine), and the title its package gives. */
internal object EpubText {

    /**
     * The book's text, from the documents of its spine that [keep] accepts: each with its id in
     * the package file, its markup, and the ids of the whole spine in order.
     */
    suspend fun read(bytes: ByteArray, keep: (id: String, html: String, spine: List<String>) -> Boolean = { _, _, _ -> true }): ExtractedText {
        val zip = openZip(bytes, "EPUB")
        val opfPath = zip.xmlEntry("META-INF/container.xml")?.let { container ->
            Markup.tokens(container).filterIsInstance<MarkupToken.Open>().firstOrNull { it.name == "rootfile" }?.attributes?.get("full-path")
        } ?: zip.entries.firstOrNull { it.name.endsWith(".opf", ignoreCase = true) }?.name
            ?: throw FileImportException("Could not parse epub: missing package file")
        val opf = zip.xmlEntry(opfPath) ?: throw FileImportException("Could not parse epub: missing package file")
        val base = opfPath.substringBeforeLast('/', "")

        val manifest = HashMap<String, String>()
        val spine = mutableListOf<String>()
        for (token in Markup.tokens(opf)) {
            if (token !is MarkupToken.Open) continue
            val local = token.name.substringAfter(':')
            if (local == "item") {
                val id = token.attributes["id"] ?: continue
                val href = token.attributes["href"] ?: continue
                manifest[id] = resolve(base, href)
            } else if (local == "itemref" && token.attributes["linear"] != "no") {
                token.attributes["idref"]?.let { spine += it }
            }
        }

        val out = TextBuilder()
        var documents = 0
        for ((id, path) in spine.mapNotNull { id -> manifest[id]?.let { id to it } }.distinctBy { it.second }) {
            val html = zip.xmlEntry(path) ?: continue
            if (!keep(id, html, spine)) continue
            HtmlText.append(html, out)
            out.blankLine()
            documents++
        }
        val text = out.build()
        if (documents == 0 || text.isEmpty()) throw FileImportException("Could not parse epub: no readable content")
        return ExtractedText(text, elementText(opf, "dc:title"))
    }

    /** The path in the archive that [href] (relative, URL-encoded) points to from the folder [base]. */
    internal fun resolve(base: String, href: String): String {
        val parts = (if (base.isEmpty()) emptyList() else base.split('/')).toMutableList()
        for (part in percentDecode(href.substringBefore('#')).split('/')) {
            when (part) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                else -> parts += part
            }
        }
        return parts.joinToString("/")
    }

    private fun percentDecode(text: String): String {
        if ('%' !in text) return text
        val bytes = ArrayList<Byte>()
        var i = 0
        while (i < text.length) {
            val code = if (text[i] == '%' && i + 2 <= text.lastIndex) text.substring(i + 1, i + 3).toIntOrNull(16) else null
            if (code != null) {
                bytes += code.toByte()
                i += 3
            } else {
                val end = text.indexOf('%', i + 1).let { if (it < 0) text.length else it }
                text.substring(i, end).encodeToByteArray().forEach { bytes += it }
                i = end
            }
        }
        return bytes.toByteArray().decodeToString()
    }
}

/**
 * The text of a Word document (docx): its paragraphs in order, tables a paragraph per cell. Deleted
 * revisions, field codes and the fallback copies of text boxes are left out, as Word does not show them.
 */
internal object DocxText {

    suspend fun read(bytes: ByteArray): ExtractedText {
        val zip = openZip(bytes, "Word (docx)")
        val xml = zip.xmlEntry("word/document.xml") ?: throw FileImportException("Could not read the document: it has no text")
        val out = TextBuilder()
        val tokens = Markup.tokens(xml).iterator()
        var inText = false
        while (tokens.hasNext()) {
            when (val token = tokens.next()) {
                is MarkupToken.Open -> when (token.name) {
                    "w:del", "w:deltext", "w:instrtext", "mc:fallback", "w:footnotereference", "w:endnotereference" ->
                        if (!token.selfClosing) HtmlText.skip(tokens, token.name)
                    "w:t" -> inText = !token.selfClosing
                    "w:tab", "w:ptab" -> out.space()
                    "w:br", "w:cr" -> out.breakLine()
                    "w:nobreakhyphen" -> out.text("-")
                }
                is MarkupToken.Close -> when (token.name) {
                    "w:t" -> inText = false
                    "w:p" -> out.breakLine()
                    "w:tc" -> out.breakLine()
                }
                is MarkupToken.Text -> if (inText) out.text(token.text, preserve = true)
            }
        }
        val text = out.build()
        if (text.isEmpty()) throw FileImportException("Could not read the document: it has no text")
        return ExtractedText(text, zip.xmlEntry("docProps/core.xml")?.let { elementText(it, "dc:title") })
    }
}

/** The text of an OpenDocument text (odt, LibreOffice's own): its paragraphs and headings, without notes. */
internal object OdtText {

    suspend fun read(bytes: ByteArray): ExtractedText {
        val zip = openZip(bytes, "OpenDocument (odt)")
        val xml = zip.xmlEntry("content.xml") ?: throw FileImportException("Could not read the document: it has no text")
        val out = TextBuilder()
        val tokens = Markup.tokens(xml).iterator()
        var depth = 0
        while (tokens.hasNext()) {
            when (val token = tokens.next()) {
                is MarkupToken.Open -> when (token.name) {
                    "text:note", "office:annotation", "text:tracked-changes" -> if (!token.selfClosing) HtmlText.skip(tokens, token.name)
                    "text:p", "text:h" -> if (!token.selfClosing) depth++ else out.breakLine()
                    "text:s" -> out.space()
                    "text:tab" -> out.space()
                    "text:line-break" -> out.breakLine()
                }
                is MarkupToken.Close -> if (token.name == "text:p" || token.name == "text:h") {
                    depth = maxOf(0, depth - 1)
                    out.breakLine()
                }
                is MarkupToken.Text -> if (depth > 0) out.text(token.text)
            }
        }
        val text = out.build()
        if (text.isEmpty()) throw FileImportException("Could not read the document: it has no text")
        return ExtractedText(text, zip.xmlEntry("meta.xml")?.let { elementText(it, "dc:title") })
    }
}

/**
 * The text of a FictionBook (fb2), the format most Russian e-books come in: the main body's
 * sections, titles, paragraphs and verse, without the notes body, the cover and the pictures.
 * FB2 files are often in windows-1251, which their XML declaration names.
 */
internal object Fb2Text {

    fun read(bytes: ByteArray): ExtractedText {
        val xml = TextDecoding.decode(bytes, TextDecoding.xmlEncoding(bytes))
        val out = TextBuilder()
        val tokens = Markup.tokens(xml).iterator()
        var inBody = false
        var title: String? = null
        while (tokens.hasNext()) {
            when (val token = tokens.next()) {
                is MarkupToken.Open -> when (token.name) {
                    "description" -> {
                        title = descriptionTitle(tokens)
                    }
                    "binary" -> if (!token.selfClosing) HtmlText.skip(tokens, "binary")
                    "body" -> if (token.attributes["name"] in NOTE_BODIES) HtmlText.skip(tokens, "body") else inBody = true
                    "a" -> if (token.attributes["type"] == "note" && !token.selfClosing) HtmlText.skip(tokens, "a")
                    "section", "empty-line", "stanza", "poem", "epigraph", "cite" -> out.blankLine()
                    "p", "v", "subtitle", "text-author", "title", "tr" -> out.breakLine()
                    "td", "th" -> out.space()
                }
                is MarkupToken.Close -> when (token.name) {
                    "body" -> { inBody = false; out.blankLine() }
                    "section", "stanza", "poem", "epigraph", "cite", "title" -> out.blankLine()
                    "p", "v", "subtitle", "text-author", "tr" -> out.breakLine()
                    "td", "th" -> out.space()
                }
                is MarkupToken.Text -> if (inBody) out.text(token.text)
            }
        }
        val text = out.build()
        if (text.isEmpty()) throw FileImportException("Could not read the book: it has no text")
        return ExtractedText(text, title)
    }

    /** Reads the rest of <description>, answering the book's title from its title-info. */
    private fun descriptionTitle(tokens: Iterator<MarkupToken>): String? {
        var title: String? = null
        var inTitle = false
        var inTitleInfo = false
        val text = StringBuilder()
        while (tokens.hasNext()) {
            when (val token = tokens.next()) {
                is MarkupToken.Open -> when (token.name) {
                    "title-info" -> inTitleInfo = true
                    "book-title" -> if (inTitleInfo && title == null) inTitle = true
                }
                is MarkupToken.Close -> when (token.name) {
                    "description" -> return title
                    "title-info" -> inTitleInfo = false
                    "book-title" -> if (inTitle) {
                        inTitle = false
                        title = text.toString().replace(Regex("\\s+"), " ").trim().takeIf { it.isNotEmpty() }
                    }
                }
                is MarkupToken.Text -> if (inTitle) text.append(token.text)
            }
        }
        return title
    }

    /** Bodies that hold the notes or comments the text refers to, not the text. */
    private val NOTE_BODIES = setOf("notes", "comments")
}
