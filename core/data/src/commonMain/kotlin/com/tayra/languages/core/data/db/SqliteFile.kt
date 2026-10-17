package com.tayra.languages.core.data.db

/** A query on a database file and the number of columns it reads. */
internal class SqliteQuery(val sql: String, val columns: Int)

/** A new SQLite database with [statements] run on it, as a file's bytes. */
internal suspend fun newSqliteFile(statements: List<String>): ByteArray = newSqliteFile { execute -> for (sql in statements) execute(sql) }

/**
 * A new SQLite database, as a file's bytes, with the statements [write] runs as it makes them, in
 * one transaction: a large file need not have all its statements in memory at once.
 */
internal expect suspend fun newSqliteFile(write: suspend (execute: suspend (String) -> Unit) -> Unit): ByteArray

/** The rows of each query run on the database in [file], every value as text or null. */
internal expect suspend fun readSqliteFile(file: ByteArray, queries: List<SqliteQuery>): List<List<List<String?>>>
