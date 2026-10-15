package com.tayra.languages.core.data.files

internal actual val pdfSupported: Boolean = false

internal actual suspend fun pdfLines(bytes: ByteArray): Pair<String, String?> = "" to null
