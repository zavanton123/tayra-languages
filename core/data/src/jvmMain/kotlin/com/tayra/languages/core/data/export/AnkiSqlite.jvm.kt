package com.tayra.languages.core.data.export

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.sql.DriverManager

internal actual suspend fun ankiSqliteBytes(statements: List<String>): ByteArray = withContext(Dispatchers.IO) {
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
