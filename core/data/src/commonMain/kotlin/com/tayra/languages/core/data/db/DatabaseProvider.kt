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
    private var database: TayraDatabase? = null

    @Volatile
    private var driver: SqlDriver? = null

    suspend fun database(): TayraDatabase {
        database?.let { return it }
        return mutex.withLock {
            database ?: createDatabase(driverFactory.createDriver().also { driver = it }).also { database = it }
        }
    }

    /** The raw driver, for bulk statements the generated queries cannot express. */
    suspend fun driver(): SqlDriver {
        database()
        return checkNotNull(driver)
    }

    private fun createDatabase(driver: SqlDriver): TayraDatabase = TayraDatabase(driver)
}
