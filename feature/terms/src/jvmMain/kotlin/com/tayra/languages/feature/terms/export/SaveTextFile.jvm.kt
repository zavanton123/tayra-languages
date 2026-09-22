package com.tayra.languages.feature.terms.export

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.writeString

actual suspend fun saveTextFile(baseName: String, extension: String, content: String) {
    val file = FileKit.openFileSaver(suggestedName = baseName, defaultExtension = extension) ?: return
    file.writeString(content)
}
