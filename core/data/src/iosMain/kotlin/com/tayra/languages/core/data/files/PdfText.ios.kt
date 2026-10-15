package com.tayra.languages.core.data.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFDocumentTitleAttribute

internal actual val pdfSupported: Boolean = true

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
internal actual suspend fun pdfLines(bytes: ByteArray): Pair<String, String?> {
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    val document = PDFDocument(data = data)
    if (document.isLocked()) throw FileImportException("This PDF is protected by a password and cannot be imported")
    val pages = (0 until document.pageCount.toInt()).mapNotNull { document.pageAtIndex(it.toULong())?.string }
    val title = (document.documentAttributes?.get(PDFDocumentTitleAttribute) as? String)?.trim()?.takeIf { it.isNotEmpty() }
    return pages.joinToString("\n\n") to title
}
