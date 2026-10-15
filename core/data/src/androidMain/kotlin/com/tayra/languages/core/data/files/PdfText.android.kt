package com.tayra.languages.core.data.files

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.koin.mp.KoinPlatform
import java.io.IOException

internal actual val pdfSupported: Boolean = true

internal actual suspend fun pdfLines(bytes: ByteArray): Pair<String, String?> {
    // PDFBox reads its fonts' glyph lists from the app's assets, through a context it is given once.
    if (!PDFBoxResourceLoader.isReady()) PDFBoxResourceLoader.init(KoinPlatform.getKoin().get<Context>())
    return try {
        PDDocument.load(bytes).use { document ->
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
}
