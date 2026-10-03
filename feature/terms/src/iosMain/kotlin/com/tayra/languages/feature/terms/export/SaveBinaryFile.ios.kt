package com.tayra.languages.feature.terms.export

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

actual suspend fun saveBinaryFile(baseName: String, extension: String, bytes: ByteArray) {
    val file = FileKit.openFileSaver(suggestedName = baseName, defaultExtension = extension) ?: return
    file.write(bytes)
}
