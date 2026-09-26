package com.tayra.languages.core.data.dictionary

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.tayra.languages.core.domain.dictionary.DictionaryAssets
import com.tayra.languages.core.domain.dictionary.DictionaryId

actual class DictionaryDriverFactory(private val context: Context) {
    actual suspend fun open(dictionary: DictionaryId, assets: DictionaryAssets): SqlDriver? {
        val name = "dictionary-${dictionary.name}.sqlite"
        if (!ensureDictionaryFile(context.getDatabasePath(name), dictionary, assets)) return null
        // The file's user_version equals the schema version, so the driver neither creates nor migrates.
        return AndroidSqliteDriver(schema = DictionaryDatabase.Schema.synchronous(), context = context, name = name)
    }
}
