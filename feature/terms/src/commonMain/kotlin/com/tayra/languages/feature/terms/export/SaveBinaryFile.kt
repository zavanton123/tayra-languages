package com.tayra.languages.feature.terms.export

/** Lets the user save bytes as a file (download in the browser); false when they chose not to. */
expect suspend fun saveBinaryFile(baseName: String, extension: String, bytes: ByteArray): Boolean
