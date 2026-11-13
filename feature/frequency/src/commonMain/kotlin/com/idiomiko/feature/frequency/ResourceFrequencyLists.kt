package com.idiomiko.feature.frequency

import com.idiomiko.core.domain.frequency.FrequencyList
import com.idiomiko.core.domain.frequency.FrequencyLists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.MissingResourceException
import idiomiko.feature.frequency.generated.resources.Res

/**
 * The frequency lists bundled with the app (files/frequency/<code>.tsv), and the word lists of the languages read without
 * spaces that have one (files/lexicon/<code>.txt, a word a line, # for comments), read once each.
 */
class ResourceFrequencyLists : FrequencyLists {
    private val lock = Mutex()
    private val loaded = mutableMapOf<String, FrequencyList?>()
    private val lexicons = mutableMapOf<String, List<String>?>()

    override suspend fun list(languageCode: String): FrequencyList? = lock.withLock {
        val code = languageCode.lowercase()
        if (code !in loaded) loaded[code] = read(code)
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

    private suspend fun read(code: String): FrequencyList? {
        val bytes = try {
            Res.readBytes("files/frequency/$code.tsv")
        } catch (e: MissingResourceException) {
            return null
        }
        return withContext(Dispatchers.Default) { FrequencyList.parse(bytes.decodeToString()) }
    }
}
