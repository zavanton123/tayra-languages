package com.tayra.languages.core.data.export

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.JournalMode
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

/** The schema of an empty file: the statements build the Anki tables themselves. */
private object EmptySchema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long = 1
    override fun create(driver: SqlDriver): QueryResult.Value<Unit> = QueryResult.Unit
    override fun migrate(driver: SqlDriver, oldVersion: Long, newVersion: Long, vararg callbacks: AfterVersion): QueryResult.Value<Unit> = QueryResult.Unit
}

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun ankiSqliteBytes(statements: List<String>): ByteArray = withContext(Dispatchers.IO) {
    val directory = NSTemporaryDirectory().trimEnd('/')
    val name = "tayra-anki-${NSUUID().UUIDString}.anki2"
    val path = "$directory/$name"
    try {
        val driver = NativeSqliteDriver(
            schema = EmptySchema,
            name = name,
            // One plain file, with nothing left in a write-ahead log when it is read back.
            onConfiguration = { config: DatabaseConfiguration ->
                config.copy(journalMode = JournalMode.DELETE, extendedConfig = DatabaseConfiguration.Extended(basePath = directory))
            },
        )
        try {
            for (sql in statements) driver.execute(null, sql, 0)
        } finally {
            driver.close()
        }
        val data = NSData.dataWithContentsOfFile(path) ?: error("Could not read the Anki collection")
        val size = data.length.toInt()
        ByteArray(size).also { bytes -> if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }
    } finally {
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }
}
