package com.tayra.languages.core.data.dictionary

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.domain.dictionary.DictionaryAssets
import com.tayra.languages.core.domain.dictionary.DictionaryId
import java.io.File
import java.util.Properties

actual class DictionaryDriverFactory(private val directory: File = File(DatabaseDriverFactory.dataDirectory(), "dictionaries")) {
    actual suspend fun open(dictionary: DictionaryId, assets: DictionaryAssets): SqlDriver? {
        val file = File(directory, "${dictionary.name}.sqlite")
        if (!ensureDictionaryFile(file, dictionary, assets)) return null
        val properties = Properties().apply { put("open_mode", "1") } // SQLITE_OPEN_READONLY
        return JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}", properties)
    }
}
