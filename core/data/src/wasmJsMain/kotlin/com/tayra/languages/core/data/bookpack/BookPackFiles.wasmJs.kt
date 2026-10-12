package com.tayra.languages.core.data.bookpack

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.tayra.languages.core.data.dictionary.cacheInstall
import com.tayra.languages.core.data.dictionary.cacheRemove
import com.tayra.languages.core.data.dictionary.cacheSize
import com.tayra.languages.core.data.dictionary.loadMessage
import com.tayra.languages.core.domain.books.BookPack
import kotlinx.coroutines.await
import org.w3c.dom.Worker

/**
 * Browsers have no file system, so packs live in the Cache API under their download URL, next to
 * the dictionaries, and are inflated into the same sql.js worker (dictionary.worker.js) when opened.
 */
actual class BookPackFiles {
    actual suspend fun installedSize(pack: BookPack): Long? =
        (cacheSize(pack.url).await<JsAny?>() as? JsNumber)?.toDouble()?.toLong()

    actual suspend fun install(pack: BookPack, onProgress: (Float?) -> Unit) {
        onProgress(null)
        cacheInstall(pack.url).await<JsAny?>()
    }

    actual suspend fun remove(pack: BookPack) {
        cacheRemove(pack.url).await<JsAny?>()
    }

    actual suspend fun openDriver(pack: BookPack): SqlDriver? {
        if (installedSize(pack) == null) return null
        val worker = Worker("dictionary.worker.js")
        worker.postMessage(loadMessage(pack.url))
        return WebWorkerDriver(worker)
    }
}
