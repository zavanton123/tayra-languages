package com.tayra.languages.core.data.dictionary

import app.cash.sqldelight.db.SqlDriver
import com.tayra.languages.core.domain.dictionary.DictionaryAssets
import com.tayra.languages.core.domain.dictionary.DictionaryId

/** Opens a bundled dictionary file read-only, or returns null when it is not bundled. */
expect class DictionaryDriverFactory {
    suspend fun open(dictionary: DictionaryId, assets: DictionaryAssets): SqlDriver?
}
