package com.tayra.languages.core.data.db

import app.cash.sqldelight.db.SqlDriver
import kotlin.concurrent.Volatile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Lazily opens the database once. Repositories obtain the database through this so
 * that dependency wiring can stay synchronous even though opening is a suspend call.
 */
class DatabaseProvider(private val driverFactory: DatabaseDriverFactory) {
    private val mutex = Mutex()

    @Volatile
    private var opened: Pair<SqlDriver, TayraDatabase>? = null

    suspend fun database(): TayraDatabase = open().second

    /** The driver under [database], for statements over whole tables such as backups. */
    suspend fun driver(): SqlDriver = open().first

    private suspend fun open(): Pair<SqlDriver, TayraDatabase> {
        opened?.let { return it }
        return mutex.withLock {
            opened ?: driverFactory.createDriver().let { it to TayraDatabase(it) }.also { opened = it }
        }
    }
}
