package com.tayra.languages.core.data.files

/** Whether this platform reads PDF files (the browser does not). */
internal expect val pdfSupported: Boolean

/** The lines of a PDF's text layer as the platform's PDF library gives them, and the title it records. */
internal expect suspend fun pdfLines(bytes: ByteArray): Pair<String, String?>

/**
 * The text of a PDF: its text layer, its visual lines joined back into paragraphs. A scanned PDF has
 * no text layer and is refused; an encrypted one too.
 */
internal object PdfText {

    suspend fun read(bytes: ByteArray): ExtractedText {
        if (!pdfSupported) throw FileImportException("PDF import is not available in the browser; import the PDF in the app on a computer or phone")
        if (bytes.size < 5 || latin1(bytes.copyOfRange(0, 5)) != "%PDF-") throw FileImportException("Could not read the file: it is not a valid PDF file")
        val (raw, title) = pdfLines(bytes)
        val text = reflow(raw)
        if (text.isEmpty()) throw FileImportException("This PDF has no text layer (it may be scanned pages); Tayra cannot read it")
        return ExtractedText(text, title)
    }

    /**
     * Joins a PDF's lines into paragraphs. A paragraph ends at an empty line, or at a line that
     * ends a sentence and stops well short of the lines around it. A word broken by a hyphen at a
     * line's end is joined again when the next line goes on in lowercase.
     */
    internal fun reflow(text: String): String {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').replace('\u000C', '\n').split('\n').map { it.trim() }
        val lengths = lines.filter { it.isNotEmpty() }.map { it.length }.sorted()
        val typical = if (lengths.isEmpty()) 0 else lengths[(lengths.size * 0.8).toInt().coerceAtMost(lengths.lastIndex)]
        val out = StringBuilder()
        val paragraph = StringBuilder()
        fun end() {
            if (paragraph.isNotEmpty()) out.append(paragraph.toString().trim()).append('\n')
            paragraph.clear()
        }
        for (line in lines) {
            if (line.isEmpty()) { end(); continue }
            if (paragraph.isNotEmpty()) {
                val last = paragraph.last()
                when {
                    last == '-' && paragraph.length > 1 && paragraph[paragraph.length - 2].isLetter() && line.first().isLowerCase() ->
                        paragraph.setLength(paragraph.length - 1)
                    isUnspaced(last) && isUnspaced(line.first()) -> Unit
                    else -> paragraph.append(' ')
                }
            }
            paragraph.append(line)
            if (line.last() in SENTENCE_ENDS && line.length < typical * 0.8) end()
        }
        end()
        return out.toString().trim()
    }

    private const val SENTENCE_ENDS = ".!?:…\"»”’)。！？"
}
