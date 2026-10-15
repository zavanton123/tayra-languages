package com.tayra.languages.core.data.files

class FileImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** A file's text, one paragraph per line, and the title the file gives itself when it has one. */
data class ExtractedText(val text: String, val title: String? = null)

/**
 * Extracts the text of a book or lesson from a file: plain text and subtitles, e-books (epub, fb2,
 * mobi, azw3), documents (docx, odt, rtf, pdf), web pages (html, mhtml) and Markdown; a zip that
 * holds one such file (a downloaded "book.fb2.zip") is read through.
 */
object FileTextExtractor {

    /** The file extensions the picker offers. */
    val supportedExtensions: List<String> = listOfNotNull(
        "txt", "epub", "fb2", "fbz", "docx", "odt", "rtf", "pdf".takeIf { pdfSupported }, "mobi", "azw", "azw3",
        "html", "htm", "xhtml", "mhtml", "mht", "md", "markdown", "srt", "vtt", "zip",
    )

    /** The formats as people name them, for the form that offers the picker. */
    val formatNames: List<String> = listOfNotNull(
        "TXT", "EPUB", "FB2", "DOCX", "ODT", "RTF", "PDF".takeIf { pdfSupported }, "MOBI", "AZW3", "HTML", "MHTML", "MD", "SRT", "VTT",
    )

    suspend fun extract(fileName: String, bytes: ByteArray): ExtractedText {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val extracted = when (extension) {
            "txt" -> ExtractedText(decodeUtf(fileName, bytes))
            "srt" -> ExtractedText(subtitleText(decodeUtf(fileName, bytes)))
            "vtt" -> ExtractedText(vttText(decodeUtf(fileName, bytes)))
            "md", "markdown" -> ExtractedText(MarkdownText.text(decodeUtf(fileName, bytes)))
            "html", "htm", "xhtml" -> decodeHtml(fileName, bytes).let { ExtractedText(HtmlText.text(it), HtmlText.title(it)) }
            "mhtml", "mht" -> MhtmlText.read(bytes)
            "epub" -> EpubText.read(bytes)
            "fb2" -> Fb2Text.read(bytes)
            "fbz", "zip" -> fromArchive(bytes)
            "docx" -> DocxText.read(bytes)
            "odt" -> OdtText.read(bytes)
            "rtf" -> RtfText.read(bytes)
            "pdf" -> PdfText.read(bytes)
            "mobi", "azw", "azw3", "prc" -> MobiText.read(bytes)
            else -> throw FileImportException("Unknown file extension \"$extension\"")
        }
        val content = clean(extracted.text)
        if (content.isEmpty()) throw FileImportException("$fileName is empty.")
        return ExtractedText(content, extracted.title?.let(::clean)?.takeIf { it.isNotEmpty() })
    }

    /** The one book a zip holds (an fbz is a zipped FB2), read by its own extension. */
    private suspend fun fromArchive(bytes: ByteArray): ExtractedText {
        val zip = openZip(bytes, "zip")
        val inner = zip.entries.firstOrNull { entry ->
            val ext = entry.name.substringAfterLast('.', "").lowercase()
            !entry.name.endsWith("/") && ext in supportedExtensions && ext != "zip" && ext != "fbz"
        } ?: throw FileImportException("The zip holds no file Tayra can read")
        return extract(inner.name.substringAfterLast('/'), zip.read(inner))
    }

    /** Line ends made "\n"; soft hyphens, which only mark where a word may break, removed. */
    private fun clean(text: String): String =
        text.replace("\r\n", "\n").replace('\r', '\n').replace("­", "").trim()

    private fun decodeUtf(fileName: String, bytes: ByteArray): String {
        if (!TextDecoding.isUtf(bytes)) throw FileImportException("$fileName is not utf-8 encoding, please convert it to utf-8 first")
        return TextDecoding.utf(bytes)
    }

    /** A web page in UTF-8, or in the charset its <meta> names when it is not UTF-8. */
    private fun decodeHtml(fileName: String, bytes: ByteArray): String {
        if (TextDecoding.isUtf(bytes)) return TextDecoding.utf(bytes)
        val head = latin1(bytes.copyOfRange(0, minOf(bytes.size, 4096)))
        val charset = Regex("""(?i)<meta[^>]+charset\s*=\s*["']?([\w-]+)""").find(head)?.groupValues?.get(1)
            ?: throw FileImportException("$fileName is not utf-8 encoding, please convert it to utf-8 first")
        return TextDecoding.decode(bytes, charset)
    }

    private val timing = Regex("""^\d{1,2}:\d{2}:\d{2}[,.]\d{3}\s*-->""")
    private val vttTiming = Regex("""^(\d{1,2}:)?\d{2}:\d{2}\.\d{3}\s*-->""")
    private val cueTags = Regex("<[^>]+>")

    /** Subtitle lines from SRT: skips indexes and timings, joins each cue. */
    internal fun subtitleText(content: String): String = cues(content.replace("\r\n", "\n"), timing)

    internal fun vttText(content: String): String {
        var lines = content.replace("\r\n", "\n").split("\n")
        if (lines.size > 2 && lines[1].startsWith("Kind:") && lines[2].startsWith("Language:")) {
            lines = lines.take(1) + lines.drop(3)
        }
        val body = lines.dropWhile { it.startsWith("WEBVTT") || it.isBlank() }
            .filterNot { it.startsWith("NOTE") }
        return cues(body.joinToString("\n"), vttTiming)
    }

    private fun cues(content: String, timingPattern: Regex): String {
        val result = mutableListOf<String>()
        for (block in content.split(Regex("\n\\s*\n"))) {
            val lines = block.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            val start = lines.indexOfFirst { timingPattern.containsMatchIn(it) }
            if (start < 0) continue
            val text = lines.drop(start + 1).joinToString(" ") { cueTags.replace(it, "") }.trim()
            if (text.isNotEmpty()) result.add(text)
        }
        return result.joinToString("\n")
    }
}
