package com.idiomiko.core.data

import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import kotlinx.coroutines.runBlocking
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every column that points at another table's row has an index starting with it: deleting a row
 * otherwise reads each table pointing at it in full, once per row deleted, and removing a language
 * deletes thousands.
 */
class ForeignKeyIndexTest {

    @Test
    fun everyForeignKeyIsIndexed() = runBlocking {
        val file = File.createTempFile("idiomiko-foreign-keys", ".db").also { it.delete() }
        DatabaseProvider(DatabaseDriverFactory(file)).database()
        val missing = DriverManager.getConnection("jdbc:sqlite:${file.path}").use { c ->
            c.strings("SELECT name FROM sqlite_master WHERE type = 'table'").flatMap { table ->
                val leading = c.strings("PRAGMA index_list('$table')", "name").mapNotNull { c.strings("PRAGMA index_info('$it')", "name").firstOrNull() } +
                    c.rows("PRAGMA table_info('$table')") { if (it.getInt("pk") == 1) it.getString("name") else null }
                c.strings("PRAGMA foreign_key_list('$table')", "from").filter { it !in leading }.map { "$table.$it" }
            }
        }
        assertEquals(emptyList(), missing)
    }

    private fun Connection.strings(sql: String, column: String? = null): List<String> = rows(sql) { if (column == null) it.getString(1) else it.getString(column) }

    private fun <T : Any> Connection.rows(sql: String, read: (java.sql.ResultSet) -> T?): List<T> = createStatement().use { st ->
        st.executeQuery(sql).use { rs -> buildList { while (rs.next()) read(rs)?.let(::add) } }
    }
}
