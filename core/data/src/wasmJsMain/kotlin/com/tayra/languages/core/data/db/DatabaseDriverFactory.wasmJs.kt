package com.tayra.languages.core.data.db

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.w3c.dom.Worker

private fun flushMessage(): JsAny = js("({ action: 'flush' })")

private fun pageHidden(): Boolean = js("document.hidden")

/**
 * The library lives in a sql.js worker (served by the web app as library.worker.js) that keeps it
 * in IndexedDB between visits: a library saved before is brought up to this version, a new one made.
 */
actual class DatabaseDriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        val worker = Worker("library.worker.js")
        val driver = WebWorkerDriver(worker)
        val version = driver.executeQuery(null, "PRAGMA user_version", { cursor ->
            QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
        }, 0).await()
        val schema = TayraDatabase.Schema
        when {
            version == 0L -> schema.create(driver).await()
            version < schema.version -> schema.migrate(driver, version, schema.version).await()
        }
        if (version < schema.version) driver.execute(null, "PRAGMA user_version = ${schema.version}", 0).await()
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).await()
        // A tab closing may not wait for the save after its last write: it is asked for as the page goes.
        document.addEventListener("visibilitychange", { if (pageHidden()) worker.postMessage(flushMessage()) })
        window.addEventListener("pagehide", { worker.postMessage(flushMessage()) })
        return driver
    }
}

actual val databaseDispatcher: CoroutineDispatcher = Dispatchers.Default
