package com.tayra.languages.core.data.dictionary

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.dictionary.DictionaryAssets
import com.tayra.languages.core.domain.dictionary.DictionaryId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Opens each bundled dictionary once and rejects files whose format the app does not know. */
class DictionaryDatabaseProvider(
    private val driverFactory: DictionaryDriverFactory,
    private val assets: DictionaryAssets,
) {
    private val mutex = Mutex()
    private val open = mutableMapOf<DictionaryId, DictionaryDatabase?>()

    suspend fun database(dictionary: DictionaryId): DictionaryDatabase? = mutex.withLock {
        if (dictionary in open) open[dictionary] else openChecked(dictionary).also { open[dictionary] = it }
    }

    private suspend fun openChecked(dictionary: DictionaryId): DictionaryDatabase? {
        val driver = try {
            driverFactory.open(dictionary, assets)
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
        return database
    }
}
