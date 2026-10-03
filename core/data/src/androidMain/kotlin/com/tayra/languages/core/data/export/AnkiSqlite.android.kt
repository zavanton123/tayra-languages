package com.tayra.languages.core.data.export

import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal actual suspend fun ankiSqliteBytes(statements: List<String>): ByteArray = withContext(Dispatchers.IO) {
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
