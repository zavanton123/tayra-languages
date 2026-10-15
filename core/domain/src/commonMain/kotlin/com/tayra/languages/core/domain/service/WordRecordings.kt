package com.tayra.languages.core.domain.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * A person saying [word], as Yomitan finds them on Wikimedia Commons: [url] is an MP3 every platform
 * plays, [speaker] and [pageUrl] name who recorded it and where its licence is, [source] the project.
 */
data class WordRecording(val word: String, val url: String, val speaker: String, val source: String, val pageUrl: String)

/** Finds recordings of a word; empty when there are none or the network fails. */
fun interface WordRecordingSource {
    suspend fun recordings(word: String, languageCode: String): List<WordRecording>
}

/** Recordings of words, remembered for the session so a word opened again does not ask the network twice. */
class WordRecordings(private val source: WordRecordingSource) {
    private val lock = Mutex()
    private val cache = LinkedHashMap<String, List<WordRecording>>()

    suspend fun of(word: String, languageCode: String): List<WordRecording> {
        val trimmed = word.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_LENGTH) return emptyList()
        val key = "$languageCode|$trimmed"
        lock.withLock { cache[key] }?.let { return it }
        val found = try {
            source.recordings(trimmed, languageCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return emptyList()
        }
        lock.withLock {
            cache[key] = found
            if (cache.size > MAX_WORDS) cache.remove(cache.keys.first())
        }
        return found
    }

    private companion object {
        const val MAX_WORDS = 500
        /** Recordings are of words and short phrases. */
        const val MAX_LENGTH = 40
    }
}
