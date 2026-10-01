package com.tayra.languages.core.domain.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/** Downloads a recording; null when the server refuses it or the network fails. */
fun interface RecordingFetcher {
    suspend fun fetch(url: String): ByteArray?
}

/** How far an example's recording has got. */
enum class RecordingState { DOWNLOADING, READY, FAILED }

/**
 * Downloads example recordings ahead of time into the speech cache, so they play the moment they
 * are asked for. A recording that cannot be downloaded is marked failed, and the example is read
 * by the speech engine instead. The cache is shared with [SentenceAudio] and emptied with it.
 */
class ExampleRecordings(
    private val fetcher: RecordingFetcher,
    private val cache: SpeechAudioCache,
) {
    private val _states = MutableStateFlow<Map<String, RecordingState>>(emptyMap())

    /** Recordings by URL; one missing here has not been asked for yet. */
    val states: StateFlow<Map<String, RecordingState>> = _states.asStateFlow()

    private val lock = Mutex()
    private val running = HashMap<String, CompletableDeferred<ByteArray?>>()

    /** Downloads [urls] one after another, in the caller's coroutine, skipping those already here. */
    suspend fun prefetch(urls: List<String>) {
        val wanted = urls.distinct().filter { _states.value[it] != RecordingState.FAILED }
        val marks = wanted.associateWith { if (cache.read(keyOf(it)) != null) RecordingState.READY else RecordingState.DOWNLOADING }
        _states.update { it + marks }
        try {
            for (url in wanted) audioFor(url)
        } finally {
            // Whatever this run did not get to is no longer under way.
            _states.update { states -> states.filter { (url, state) -> url !in marks || state != RecordingState.DOWNLOADING } }
        }
    }

    /** The recording from the cache, or downloaded now (sharing a download already under way). */
    suspend fun audioFor(url: String): ByteArray? {
        cache.read(keyOf(url))?.let { return it.also { markReady(url) } }
        var own = false
        val download = lock.withLock {
            running[url] ?: CompletableDeferred<ByteArray?>().also { running[url] = it; own = true }
        }
        if (!own) return download.await()
        _states.update { it + (url to RecordingState.DOWNLOADING) }
        val audio = try {
            fetcher.fetch(url)?.takeIf { it.isNotEmpty() }?.also { cache.write(keyOf(url), it) }
        } catch (e: CancellationException) {
            lock.withLock { running.remove(url) }
            download.cancel()
            throw e
        } catch (e: Exception) {
            null
        }
        lock.withLock { running.remove(url) }
        download.complete(audio)
        _states.update { it + (url to if (audio != null) RecordingState.READY else RecordingState.FAILED) }
        return audio
    }

    /** Forgets every recording, as when the cache is emptied for another text. */
    fun reset() {
        _states.value = emptyMap()
    }

    private fun markReady(url: String) = _states.update { if (it[url] == RecordingState.READY) it else it + (url to RecordingState.READY) }

    private fun keyOf(url: String) = "recording|$url"
}
