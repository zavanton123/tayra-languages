package com.tayra.languages.core.data.dictionary

import app.cash.sqldelight.db.SqlDriver
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore

/** Platform storage for downloaded packs, which also opens them as read-only SQLite drivers. */
expect class DictionaryPackStorage : DictionaryPackStore {
    override suspend fun installedSize(pack: DictionaryPack): Long?
    override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit)
    override suspend fun remove(pack: DictionaryPack)
    /** A driver over the installed pack, or null when it is not installed. */
    suspend fun openDriver(pack: DictionaryPack): SqlDriver?
}
