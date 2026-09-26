package com.tayra.languages.core.data.dictionary

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import java.io.File
import java.util.Properties

actual class DictionaryPackStorage(
    downloader: DictionaryDownloader,
    directory: File = File(DatabaseDriverFactory.dataDirectory(), "dictionaries"),
) : DictionaryPackStore {
    private val files = PackFiles(directory, downloader)

    actual override suspend fun installedSize(pack: DictionaryPack): Long? = files.installedSize(pack)

    actual override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) = files.install(pack, onProgress)

    actual override suspend fun remove(pack: DictionaryPack) = files.remove(pack)

    actual suspend fun openDriver(pack: DictionaryPack): SqlDriver? {
        if (files.installedSize(pack) == null) return null
        val properties = Properties().apply { put("open_mode", "1") } // SQLITE_OPEN_READONLY
        return JdbcSqliteDriver("jdbc:sqlite:${files.file(pack).absolutePath}", properties)
    }
}
