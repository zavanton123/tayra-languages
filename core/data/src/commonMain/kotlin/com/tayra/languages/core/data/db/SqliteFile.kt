package com.tayra.languages.core.data.db

/** A query on a database file and the number of columns it reads. */
internal class SqliteQuery(val sql: String, val columns: Int)

/** A new SQLite database with [statements] run on it, as a file's bytes. */
internal expect suspend fun newSqliteFile(statements: List<String>): ByteArray

/** The rows of each query run on the database in [file], every value as text or null. */
internal expect suspend fun readSqliteFile(file: ByteArray, queries: List<SqliteQuery>): List<List<List<String?>>>
