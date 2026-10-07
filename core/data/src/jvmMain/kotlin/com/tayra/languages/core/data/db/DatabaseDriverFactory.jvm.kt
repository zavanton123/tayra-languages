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
        val properties = Properties().apply {
            put("foreign_keys", "true")
            // The app and the `tayra` command may use the database at the same time: WAL lets one read while the
            // other writes, and the timeout makes a writer wait for the other's write instead of failing.
            put("journal_mode", "WAL")
            put("busy_timeout", "5000")
        }
        return JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}", properties, TayraDatabase.Schema.synchronous())
    }

    companion object {
        fun defaultDatabaseFile(): File = File(dataDirectory(), "tayra.db")

        /** Set by `tayra --data-dir`; the variable does the same for the app and the command. */
        const val DATA_DIR_PROPERTY = "tayra.dataDir"
        const val DATA_DIR_VARIABLE = "TAYRA_DATA_DIR"

        /** The data folder chosen in place of the usual one, if any. */
        fun customDataDirectory(): File? =
            (System.getProperty(DATA_DIR_PROPERTY) ?: System.getenv(DATA_DIR_VARIABLE))?.takeIf { it.isNotBlank() }?.let(::File)

        fun dataDirectory(): File {
            customDataDirectory()?.let { return it }
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
