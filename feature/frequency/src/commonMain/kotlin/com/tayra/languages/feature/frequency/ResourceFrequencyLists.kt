package com.tayra.languages.feature.frequency

import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.frequency.FrequencyLists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.MissingResourceException
import tayra_languages.feature.frequency.generated.resources.Res

/** The frequency lists bundled with the app (files/frequency/<code>.tsv), read once each. */
class ResourceFrequencyLists : FrequencyLists {
    private val lock = Mutex()
    private val loaded = mutableMapOf<String, FrequencyList?>()

    override suspend fun list(languageCode: String): FrequencyList? = lock.withLock {
        val code = languageCode.lowercase()
        if (code !in loaded) loaded[code] = read(code)
        loaded[code]
    }

    private suspend fun read(code: String): FrequencyList? {
        val bytes = try {
            Res.readBytes("files/frequency/$code.tsv")
        } catch (e: MissingResourceException) {
            return null
        }
        return withContext(Dispatchers.Default) { FrequencyList.parse(bytes.decodeToString()) }
    }
}
