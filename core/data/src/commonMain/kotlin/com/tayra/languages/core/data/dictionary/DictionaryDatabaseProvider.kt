package com.tayra.languages.core.data.dictionary

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Opens each installed pack once and rejects files whose format the app does not know. */
class DictionaryDatabaseProvider(private val storage: DictionaryPackStorage) {
    private val mutex = Mutex()
    private val open = mutableMapOf<DictionaryId, Opened?>()

    private class Opened(val database: DictionaryDatabase, val close: () -> Unit)

    suspend fun database(dictionary: DictionaryId): DictionaryDatabase? = mutex.withLock {
        (if (dictionary in open) open[dictionary] else openChecked(dictionary).also { open[dictionary] = it })?.database
    }

    suspend fun close(dictionary: DictionaryId) {
        mutex.withLock { open.remove(dictionary)?.close?.invoke() }
    }

    private suspend fun openChecked(dictionary: DictionaryId): Opened? {
        val pack = DictionaryPacks.find(dictionary) ?: return null
        val driver = try {
            storage.openDriver(pack)
        } catch (e: Exception) {
            Logger.w(e) { "Could not open dictionary ${dictionary.name}" }
            null
        } ?: return null
        val database = DictionaryDatabase(driver)
        val format = try {
            database.dictionaryQueries.selectMeta("format").awaitAsOneOrNull()?.toIntOrNull()
        } catch (e: Exception) {
            Logger.w(e) { "Dictionary ${dictionary.name} is not readable" }
            null
        }
        if (format != DictionaryId.FORMAT) {
            Logger.w { "Dictionary ${dictionary.name} has format $format, expected ${DictionaryId.FORMAT}" }
            driver.close()
            return null
        }
        return Opened(database) { driver.close() }
    }
}
