package com.tayra.languages.core.data.db

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual class DatabaseDriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        val driver = createDefaultWebWorkerDriver()
        TayraDatabase.Schema.create(driver).await()
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).await()
        return driver
    }
}

actual val databaseDispatcher: CoroutineDispatcher = Dispatchers.Default
