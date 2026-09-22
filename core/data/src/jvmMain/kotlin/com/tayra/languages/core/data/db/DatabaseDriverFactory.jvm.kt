package com.tayra.languages.core.data.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.Properties

actual class DatabaseDriverFactory(private val databaseFile: File = defaultDatabaseFile()) {
    actual suspend fun createDriver(): SqlDriver {
        databaseFile.parentFile?.mkdirs()
        val properties = Properties().apply { put("foreign_keys", "true") }
        return JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}", properties, TayraDatabase.Schema.synchronous())
    }

    companion object {
        fun defaultDatabaseFile(): File = File(dataDirectory(), "tayra.db")

        fun dataDirectory(): File {
            val home = System.getProperty("user.home")
            val os = System.getProperty("os.name").lowercase()
            val base = when {
                os.contains("mac") -> File(home, "Library/Application Support/TayraLanguages")
                os.contains("win") -> File(System.getenv("APPDATA") ?: home, "TayraLanguages")
                else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "tayra-languages")
            }
            return base
        }
    }
}

actual val databaseDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
