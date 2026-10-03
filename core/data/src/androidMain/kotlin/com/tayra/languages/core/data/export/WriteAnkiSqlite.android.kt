package com.tayra.languages.core.data.export

import android.database.sqlite.SQLiteDatabase

internal actual fun writeAnkiSqlite(path: String, statements: List<String>) {
    SQLiteDatabase.openOrCreateDatabase(path, null).use { database ->
        database.beginTransaction()
        try {
            for (sql in statements) database.execSQL(sql)
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}
