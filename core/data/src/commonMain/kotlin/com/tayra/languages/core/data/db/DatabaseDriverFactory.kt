package com.tayra.languages.core.data.db

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineDispatcher

/** Creates the platform SQLite driver; creation may be asynchronous (web worker). */
expect class DatabaseDriverFactory {
    suspend fun createDriver(): SqlDriver
}

/** Dispatcher used for database access. */
expect val databaseDispatcher: CoroutineDispatcher
