package com.tayra.languages.core.data.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.sql.DriverManager

internal actual suspend fun newSqliteFile(statements: List<String>): ByteArray = withContext(Dispatchers.IO) {
    val file = File.createTempFile("tayra-anki", ".anki2")
    try {
        file.delete()
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { connection ->
            connection.autoCommit = false
            connection.createStatement().use { statement -> for (sql in statements) statement.execute(sql) }
            connection.commit()
        }
        file.readBytes()
    } finally {
        file.delete()
    }
}

internal actual suspend fun readSqliteFile(file: ByteArray, queries: List<SqliteQuery>): List<List<List<String?>>> = withContext(Dispatchers.IO) {
    val copy = File.createTempFile("tayra-read", ".sqlite")
    try {
        copy.writeBytes(file)
        DriverManager.getConnection("jdbc:sqlite:${copy.absolutePath}").use { connection ->
            queries.map { query ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(query.sql).use { rows ->
                        buildList { while (rows.next()) add(List(query.columns) { rows.getString(it + 1) }) }
                    }
                }
            }
        }
    } finally {
        copy.delete()
    }
}
