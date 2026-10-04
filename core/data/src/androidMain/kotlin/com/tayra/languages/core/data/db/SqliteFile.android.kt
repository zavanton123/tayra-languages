package com.tayra.languages.core.data.db

import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal actual suspend fun newSqliteFile(statements: List<String>): ByteArray = withContext(Dispatchers.IO) {
    val file = File.createTempFile("tayra-anki", ".anki2")
    try {
        file.delete()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
            database.beginTransaction()
            try {
                for (sql in statements) database.execSQL(sql)
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
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
        SQLiteDatabase.openDatabase(copy.path, null, SQLiteDatabase.OPEN_READWRITE).use { database ->
            queries.map { query ->
                database.rawQuery(query.sql, null).use { cursor ->
                    buildList { while (cursor.moveToNext()) add(List(query.columns) { if (cursor.isNull(it)) null else cursor.getString(it) }) }
                }
            }
        }
    } finally {
        copy.delete()
    }
}
