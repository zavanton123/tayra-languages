package com.tayra.languages.core.data.backup

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.SqliteQuery
import com.tayra.languages.core.data.db.TayraDatabase
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.data.db.newSqliteFile
import com.tayra.languages.core.data.db.readSqliteFile
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.backup.Backup
import com.tayra.languages.core.domain.backup.BackupException
import com.tayra.languages.core.domain.backup.BackupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A backup is a SQLite file with the app's tables and rows as they were, plus two tables of its
 * own: `backup_info` (format, schema version, creation time) and `backup_settings` (stored settings
 * as text). Rows travel as SQL literals made by SQLite's `quote()`, so every value keeps its type
 * on every platform. A restore copies the columns both schemas share, which covers backups made
 * before columns were added.
 */
class BackupRepositoryImpl(
    private val provider: DatabaseProvider,
    private val settings: SettingsRepositoryImpl,
    private val files: BackupFiles,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : BackupRepository {

    override suspend fun list(): List<Backup> =
        files.list().filter { it.name.endsWith(EXTENSION) }
            .map { Backup(it.name, createdAt(it.name), it.sizeBytes) }
            .sortedWith(compareByDescending<Backup> { it.createdAt }.thenByDescending { it.name })

    override suspend fun create(): Backup {
        val now = clock.now()
        val bytes = newSqliteFile(snapshot(now))
        val name = fileName(now)
        files.write(name, bytes)
        return Backup(name, createdAt(name), bytes.size.toLong())
    }

    override suspend fun delete(name: String) = files.delete(name)

    override suspend fun read(name: String): ByteArray = files.read(name)

    override suspend fun import(file: ByteArray): Backup {
        val contents = open(file)
        val name = fileName(contents.createdAt)
        files.write(name, file)
        return Backup(name, createdAt(name), file.size.toLong())
    }

    override suspend fun restore(name: String): Backup {
        val file = files.read(name)
        val contents = open(file)
        val undo = create()
        withContext(databaseDispatcher) {
            val database = provider.database()
            val driver = provider.driver()
            val tables = tablesOf(driver)
            val copied = tables.filter { it in contents.columns }.associateWith { table ->
                columnsOf(driver, table).filter { it in contents.columns.getValue(table) }
            }.filterValues { it.isNotEmpty() }
            val rows = readFile(file, copied.map { (table, columns) -> SqliteQuery(literalRows(table, columns), 1) })
            database.transaction {
                // Rows go in table by table, so references are checked once all of them are in.
                driver.execute(null, "PRAGMA defer_foreign_keys = ON", 0).await()
                for (table in tables) driver.execute(null, "DELETE FROM ${quoted(table)}", 0).await()
                if (hasSequences(driver)) driver.execute(null, "DELETE FROM sqlite_sequence", 0).await()
                copied.entries.forEachIndexed { i, (table, columns) ->
                    for (insert in inserts(table, columns, rows[i].map { it.single()!! })) driver.execute(null, insert, 0).await()
                }
            }
            driver.notifyListeners(*tables.toTypedArray())
        }
        settings.restoreBackupValues(contents.settings)
        return undo
    }

    /** The statements that build the backup file: the schema, every row, and the backup's own tables. */
    private suspend fun snapshot(now: Instant): List<String> = withContext(databaseDispatcher) {
        val database = provider.database()
        val driver = provider.driver()
        database.transactionWithResult {
            val schema = driver.rows(
                "SELECT sql FROM sqlite_master WHERE sql IS NOT NULL AND type IN ('table', 'index') " +
                    "AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata' ORDER BY type DESC",
                1,
            ).map { it.single()!! }
            val data = tablesOf(driver).flatMap { table ->
                val columns = columnsOf(driver, table)
                inserts(table, columns, driver.rows(literalRows(table, columns), 1).map { it.single()!! })
            }
            val info = mapOf(
                INFO_FORMAT to FORMAT,
                INFO_SCHEMA to TayraDatabase.Schema.version.toString(),
                INFO_CREATED_AT to now.toEpochMilliseconds().toString(),
            )
            schema + data +
                "CREATE TABLE $INFO_TABLE (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)" +
                inserts(INFO_TABLE, KEY_VALUE, info.map { (k, v) -> "${literal(k)},${literal(v)}" }) +
                "CREATE TABLE $SETTINGS_TABLE (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)" +
                inserts(SETTINGS_TABLE, KEY_VALUE, settings.backupValues().map { (k, v) -> "${literal(k)},${literal(v)}" })
        }
    }

    private class Contents(val createdAt: Instant, val columns: Map<String, List<String>>, val settings: Map<String, String>)

    /** Reads what a restore needs from a backup file, refusing files that are not backups this version understands. */
    private suspend fun open(file: ByteArray): Contents {
        val notABackup = "This file is not a Tayra Languages backup"
        if (file.size < SQLITE_HEADER.length || file.copyOf(SQLITE_HEADER.length).decodeToString() != SQLITE_HEADER) throw BackupException(notABackup)
        val tables = readFile(file, listOf(SqliteQuery("SELECT name FROM sqlite_master WHERE type = 'table'", 1)))
            .single().map { it.single()!! }
        if (INFO_TABLE !in tables || SETTINGS_TABLE !in tables) throw BackupException(notABackup)
        val dataTables = tables.filter { it != INFO_TABLE && it != SETTINGS_TABLE && !it.startsWith("sqlite_") && it != "android_metadata" }
        val results = readFile(
            file,
            listOf(
                SqliteQuery("SELECT key, value FROM $INFO_TABLE", 2),
                SqliteQuery("SELECT key, value FROM $SETTINGS_TABLE", 2),
            ) + dataTables.map { SqliteQuery("PRAGMA table_info(${quoted(it)})", 2) },
        )
        val info = results[0].associate { it[0]!! to it[1]!! }
        if (info[INFO_FORMAT] != FORMAT) throw BackupException(notABackup)
        val schema = info[INFO_SCHEMA]?.toLongOrNull() ?: throw BackupException(notABackup)
        if (schema > TayraDatabase.Schema.version) throw BackupException("This backup was made by a newer version of Tayra Languages; update the app to restore it")
        val createdAt = info[INFO_CREATED_AT]?.toLongOrNull()?.let(Instant::fromEpochMilliseconds) ?: throw BackupException(notABackup)
        return Contents(
            createdAt = createdAt,
            columns = dataTables.withIndex().associate { (i, table) -> table to results[i + 2].map { it[1]!! } },
            settings = results[1].associate { it[0]!! to it[1]!! },
        )
    }

    private suspend fun readFile(file: ByteArray, queries: List<SqliteQuery>): List<List<List<String?>>> = try {
        readSqliteFile(file, queries)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw BackupException("This file is not a Tayra Languages backup", e)
    }

    private suspend fun tablesOf(driver: SqlDriver): List<String> = driver.rows(
        "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata' ORDER BY name",
        1,
    ).map { it.single()!! }

    private suspend fun hasSequences(driver: SqlDriver): Boolean =
        driver.rows("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'sqlite_sequence'", 1).isNotEmpty()

    private suspend fun columnsOf(driver: SqlDriver, table: String): List<String> =
        driver.rows("PRAGMA table_info(${quoted(table)})", 2, from = 1).map { it.single()!! }

    private fun fileName(at: Instant): String {
        val time = at.toLocalDateTime(timeZone())
        fun two(n: Int) = n.toString().padStart(2, '0')
        return "$PREFIX${time.year}-${two(time.month.ordinal + 1)}-${two(time.day)}_${two(time.hour)}-${two(time.minute)}-${two(time.second)}$EXTENSION"
    }

    private fun createdAt(name: String): LocalDateTime? {
        val match = NAME.matchEntire(name) ?: return null
        val (year, month, day, hour, minute, second) = match.destructured
        return runCatching { LocalDateTime(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt(), second.toInt()) }.getOrNull()
    }

    private companion object {
        const val PREFIX = "tayra-backup-"
        const val EXTENSION = ".sqlite"
        val NAME = Regex("""tayra-backup-(\d{4})-(\d{2})-(\d{2})_(\d{2})-(\d{2})-(\d{2})\.sqlite""")
        const val SQLITE_HEADER = "SQLite format 3"
        const val FORMAT = "tayra-languages-backup-1"
        const val INFO_TABLE = "backup_info"
        const val SETTINGS_TABLE = "backup_settings"
        const val INFO_FORMAT = "format"
        const val INFO_SCHEMA = "schema_version"
        const val INFO_CREATED_AT = "created_at"
        val KEY_VALUE = listOf("key", "value")

        /** Rows per INSERT, and the length past which a statement is cut, so no statement holds a large share of the data. */
        const val ROWS_PER_INSERT = 200
        const val CHARS_PER_INSERT = 400_000

        fun quoted(identifier: String) = "\"" + identifier.replace("\"", "\"\"") + "\""

        fun literal(text: String) = "'" + text.replace("'", "''") + "'"

        /** A query reading every row of [table] as one text of comma-separated SQL literals. */
        fun literalRows(table: String, columns: List<String>) =
            "SELECT ${columns.joinToString(" || ',' || ") { "quote(${quoted(it)})" }} FROM ${quoted(table)}"

        fun inserts(table: String, columns: List<String>, rows: List<String>): List<String> {
            if (rows.isEmpty()) return emptyList()
            val head = "INSERT INTO ${quoted(table)} (${columns.joinToString(",") { quoted(it) }}) VALUES "
            val statements = mutableListOf<String>()
            val current = StringBuilder()
            var count = 0
            for (row in rows) {
                if (count > 0 && (count >= ROWS_PER_INSERT || current.length + row.length > CHARS_PER_INSERT)) {
                    statements += current.toString()
                    current.clear()
                    count = 0
                }
                current.append(if (count == 0) head else ",").append('(').append(row).append(')')
                count++
            }
            statements += current.toString()
            return statements
        }

        /**
         * The text columns [from] until [until] of every row; the web driver cannot read other types as text.
         * Rows are read inside the mapper, before synchronous drivers close the cursor; the web
         * driver's cursor holds every row already.
         */
        suspend fun SqlDriver.rows(sql: String, until: Int, from: Int = 0): List<List<String?>> =
            executeQuery(null, sql, { cursor ->
                QueryResult.Value(buildList { while (cursor.next().value) add((from until until).map { cursor.getString(it) }) })
            }, 0).await()
    }
}
