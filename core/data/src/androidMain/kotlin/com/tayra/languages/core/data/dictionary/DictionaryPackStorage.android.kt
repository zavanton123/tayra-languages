package com.tayra.languages.core.data.dictionary

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import java.io.File

actual class DictionaryPackStorage(private val context: Context, downloader: DictionaryDownloader) : DictionaryPackStore {
    private val files = PackFiles(File(context.filesDir, "dictionaries"), downloader)

    actual override suspend fun installedSize(pack: DictionaryPack): Long? = files.installedSize(pack.id.name, DictionaryId.FORMAT)

    actual override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) = files.install(pack.id.name, pack.url, DictionaryId.FORMAT, onProgress)

    actual override suspend fun remove(pack: DictionaryPack) = files.remove(pack.id.name)

    actual suspend fun openDriver(pack: DictionaryPack): SqlDriver? {
        if (files.installedSize(pack.id.name, DictionaryId.FORMAT) == null) return null
        // The file's user_version equals the schema version, so the driver neither creates nor migrates.
        return AndroidSqliteDriver(schema = DictionaryDatabase.Schema.synchronous(), context = context, name = files.file(pack.id.name).absolutePath)
    }
}
