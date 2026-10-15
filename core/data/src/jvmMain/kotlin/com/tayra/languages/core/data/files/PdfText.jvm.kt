package com.tayra.languages.core.data.files

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.text.PDFTextStripper
import java.io.IOException

internal actual val pdfSupported: Boolean = true

internal actual suspend fun pdfLines(bytes: ByteArray): Pair<String, String?> = try {
    Loader.loadPDF(bytes).use { document ->
        val stripper = PDFTextStripper().apply {
            sortByPosition = true
            lineSeparator = "\n"
            paragraphEnd = "\n\n"
            pageEnd = "\n"
        }
        stripper.getText(document) to document.documentInformation?.title?.trim()?.takeIf { it.isNotEmpty() }
    }
} catch (e: InvalidPasswordException) {
    throw FileImportException("This PDF is protected by a password and cannot be imported", e)
} catch (e: IOException) {
    throw FileImportException("Could not read the file: it is not a valid PDF file", e)
}
