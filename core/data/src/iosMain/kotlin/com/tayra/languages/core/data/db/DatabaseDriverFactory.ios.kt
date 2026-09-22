package com.tayra.languages.core.data.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual class DatabaseDriverFactory {
    actual suspend fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = TayraDatabase.Schema.synchronous(),
        name = "tayra.db",
        onConfiguration = { config: DatabaseConfiguration ->
            config.copy(extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true))
        },
    )
}

actual val databaseDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
