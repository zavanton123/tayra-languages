package com.tayra.languages.core.data.files

actual fun extractEpubText(bytes: ByteArray): String =
    throw FileImportException("Epub import is not supported on this platform yet; please import a .txt file")
