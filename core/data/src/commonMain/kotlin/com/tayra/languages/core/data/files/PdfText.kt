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
        val text = LineReflow.paragraphs(raw)
        if (text.isEmpty()) throw FileImportException("This PDF has no text layer (it may be scanned pages); Tayra cannot read it")
        return ExtractedText(text, title)
    }
}
