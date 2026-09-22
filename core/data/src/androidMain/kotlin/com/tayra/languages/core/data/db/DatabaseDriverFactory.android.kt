package com.tayra.languages.core.data.db

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual class DatabaseDriverFactory(private val context: Context) {
    actual suspend fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = TayraDatabase.Schema.synchronous(),
        context = context,
        name = "tayra.db",
        callback = object : AndroidSqliteDriver.Callback(TayraDatabase.Schema.synchronous()) {
            override fun onConfigure(db: SupportSQLiteDatabase) {
                super.onConfigure(db)
                db.setForeignKeyConstraintsEnabled(true)
            }
        },
    )
}

actual val databaseDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
