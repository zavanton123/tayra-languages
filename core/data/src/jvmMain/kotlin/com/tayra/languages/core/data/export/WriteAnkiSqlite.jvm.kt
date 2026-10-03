package com.tayra.languages.core.data.export

import java.sql.DriverManager

internal actual fun writeAnkiSqlite(path: String, statements: List<String>) {
    DriverManager.getConnection("jdbc:sqlite:$path").use { connection ->
        connection.autoCommit = false
        connection.createStatement().use { statement -> for (sql in statements) statement.execute(sql) }
        connection.commit()
    }
}
