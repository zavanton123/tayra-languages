package com.tayra.languages.core.data.dictionary

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.tayra.languages.core.domain.dictionary.DictionaryAssets
import com.tayra.languages.core.domain.dictionary.DictionaryId
import org.w3c.dom.Worker

private fun loadMessage(url: String): JsAny = js("({ action: 'load', url: url })")

/**
 * Runs the dictionary in its own sql.js worker, which fetches the bundled file into memory.
 * The worker script is served by the web app as dictionary.worker.js.
 */
actual class DictionaryDriverFactory {
    actual suspend fun open(dictionary: DictionaryId, assets: DictionaryAssets): SqlDriver? {
        val url = assets.uri(dictionary) ?: return null
        val worker = Worker("dictionary.worker.js")
        worker.postMessage(loadMessage(url))
        return WebWorkerDriver(worker)
    }
}
