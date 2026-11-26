package com.idiomiko.feature.frequency

import com.idiomiko.core.domain.frequency.FrequencyList
import com.idiomiko.core.domain.frequency.FrequencyListDownloads
import com.idiomiko.core.domain.frequency.FrequencyLists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.MissingResourceException
import idiomiko.feature.frequency.generated.resources.Res
import kotlinx.coroutines.CancellationException

/**
 * The frequency lists bundled with the app (files/frequency/<code>.tsv), or fetched by [downloads] where the build leaves
 * them out (Android), and the word lists of the languages read without spaces that have one (files/lexicon/<code>.txt,
 * a word a line, # for comments), read once each. A list that could not be fetched is tried again the next time.
 */
class ResourceFrequencyLists(private val downloads: FrequencyListDownloads? = null) : FrequencyLists {
    private val lock = Mutex()
    private val loaded = mutableMapOf<String, FrequencyList?>()
    private val lexicons = mutableMapOf<String, List<String>?>()

    override suspend fun list(languageCode: String): FrequencyList? = lock.withLock {
        val code = languageCode.lowercase()
        if (code !in loaded) {
            val bytes = bundled(code) ?: try {
                downloads?.fetch(code)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Offline, say: nothing is kept, so the list is fetched the next time it is wanted.
                return@withLock null
            }
            loaded[code] = bytes?.let { withContext(Dispatchers.Default) { FrequencyList.parse(it.decodeToString()) } }
        }
        loaded[code]
    }

    override suspend fun lexicon(languageCode: String): List<String>? = lock.withLock {
        val code = languageCode.lowercase()
        if (code !in lexicons) lexicons[code] = readLexicon(code)
        lexicons[code]
    }

    private suspend fun readLexicon(code: String): List<String>? {
        val bytes = try {
            Res.readBytes("files/lexicon/$code.txt")
        } catch (e: MissingResourceException) {
            return null
        }
        return withContext(Dispatchers.Default) {
            bytes.decodeToString().lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
        }
    }

    private suspend fun bundled(code: String): ByteArray? = try {
        Res.readBytes("files/frequency/$code.tsv")
    } catch (e: MissingResourceException) {
        null
    }
}
