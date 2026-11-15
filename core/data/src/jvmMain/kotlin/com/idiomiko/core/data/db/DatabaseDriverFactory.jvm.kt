package com.idiomiko.core.data.db

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
            // The app and the `idiomiko` command may use the database at the same time: WAL lets one read while the
            // other writes, and the timeout makes a writer wait for the other's write instead of failing.
            put("journal_mode", "WAL")
            put("busy_timeout", "5000")
        }
        return JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}", properties, IdiomikoDatabase.Schema.synchronous())
    }

    companion object {
        fun defaultDatabaseFile(): File = File(dataDirectory(), "idiomiko.db")

        /** Set by `idiomiko --data-dir`; the variable does the same for the app and the command. */
        const val DATA_DIR_PROPERTY = "idiomiko.dataDir"
        const val DATA_DIR_VARIABLE = "IDIOMIKO_DATA_DIR"

        /** The data folder chosen in place of the usual one, if any. */
        fun customDataDirectory(): File? =
            (System.getProperty(DATA_DIR_PROPERTY) ?: System.getenv(DATA_DIR_VARIABLE))?.takeIf { it.isNotBlank() }?.let(::File)

        fun dataDirectory(): File = customDataDirectory() ?: usualDataDirectory

        private val usualDataDirectory: File by lazy {
            val home = System.getProperty("user.home")
            val os = System.getProperty("os.name").lowercase()
            val (parent, name, formerName) = when {
                os.contains("mac") -> Triple(File(home, "Library/Application Support"), "Idiomiko", "TayraLanguages")
                os.contains("win") -> Triple(File(System.getenv("APPDATA") ?: home), "Idiomiko", "TayraLanguages")
                else -> Triple(File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share"), "idiomiko", "tayra-languages")
            }
            File(parent, name).also { moveFormerLibrary(File(parent, formerName), it) }
        }

        /** The library of the app's former name, Tayra Languages, moves to this one's folder on first start. */
        internal fun moveFormerLibrary(former: File, folder: File) {
            if (folder.exists() || !former.isDirectory || !former.renameTo(folder)) return
            for (suffix in listOf("", "-wal", "-shm")) File(folder, "tayra.db$suffix").takeIf { it.isFile }?.renameTo(File(folder, "idiomiko.db$suffix"))
        }
    }
}

actual val databaseDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
