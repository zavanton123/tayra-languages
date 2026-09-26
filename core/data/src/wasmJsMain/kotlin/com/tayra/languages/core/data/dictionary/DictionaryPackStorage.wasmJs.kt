package com.tayra.languages.core.data.dictionary

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import kotlinx.coroutines.await
import org.w3c.dom.Worker
import kotlin.js.Promise

private fun cacheInstall(url: String): Promise<JsAny?> = js(
    """caches.open('tayra-dictionaries').then(function (cache) {
        return fetch(url).then(function (response) {
            if (!response.ok) throw new Error('Server answered ' + response.status);
            return cache.put(url, response);
        });
    }).then(function () { return null; })""",
)

private fun cacheSize(url: String): Promise<JsAny?> = js(
    """caches.open('tayra-dictionaries').then(function (cache) { return cache.match(url); })
        .then(function (response) { return response ? response.blob().then(function (blob) { return blob.size; }) : null; })""",
)

private fun cacheRemove(url: String): Promise<JsAny?> = js(
    """caches.open('tayra-dictionaries').then(function (cache) { return cache.delete(url); }).then(function () { return null; })""",
)

private fun loadMessage(url: String): JsAny = js("({ action: 'load', url: url })")

/**
 * Browsers have no file system, so packs live in the Cache API under their download URL and
 * are inflated into a sql.js worker (served by the web app as dictionary.worker.js) when opened.
 */
actual class DictionaryPackStorage : DictionaryPackStore {
    actual override suspend fun installedSize(pack: DictionaryPack): Long? =
        (cacheSize(pack.url).await<JsAny?>() as? JsNumber)?.toDouble()?.toLong()

    actual override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) {
        onProgress(null)
        cacheInstall(pack.url).await<JsAny?>()
    }

    actual override suspend fun remove(pack: DictionaryPack) {
        cacheRemove(pack.url).await<JsAny?>()
    }

    actual suspend fun openDriver(pack: DictionaryPack): SqlDriver? {
        if (installedSize(pack) == null) return null
        val worker = Worker("dictionary.worker.js")
        worker.postMessage(loadMessage(pack.url))
        return WebWorkerDriver(worker)
    }
}
