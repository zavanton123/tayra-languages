package com.tayra.languages.feature.terms.export

/** Lets the user save text as a file (download in the browser). */
expect suspend fun saveTextFile(baseName: String, extension: String, content: String)
