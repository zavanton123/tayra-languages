package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/** Where synthesized sentence audio is kept until another text is opened. */
interface SpeechAudioCache {
    suspend fun read(key: String): ByteArray?
    suspend fun write(key: String, audio: ByteArray)
    suspend fun clear()
}

/** A [SpeechAudioCache] in memory, for platforms without a file system and for tests. */
class MemorySpeechAudioCache : SpeechAudioCache {
    private val lock = Mutex()
    private val entries = HashMap<String, ByteArray>()

    override suspend fun read(key: String): ByteArray? = lock.withLock { entries[key] }
    override suspend fun write(key: String, audio: ByteArray) = lock.withLock { entries[key] = audio }
    override suspend fun clear() = lock.withLock { entries.clear() }
}

/** How far a sentence's audio has got. */
enum class SentenceAudioState { PREPARING, READY }

/**
 * Synthesizes the sentences of the open text ahead of time with the local speech engine, so a
 * sentence plays the moment it is asked for. The system voice speaks directly and needs none of
 * this. Audio is cached per engine, voice, speed and language, and dropped when another text
 * (book) is opened.
 */
class SentenceAudio(
    private val localSpeech: LocalSpeech,
    private val settings: SettingsRepository,
    private val cache: SpeechAudioCache,
) {
    private val _states = MutableStateFlow<Map<String, SentenceAudioState>>(emptyMap())

    /** The sentences of the latest [prepare], by text; a sentence missing here plays as before. */
    val states: StateFlow<Map<String, SentenceAudioState>> = _states.asStateFlow()

    /** One synthesis at a time: the local engines run a single worker each. */
    private val synthesis = Mutex()
    private var bookId: Long? = null

    /**
     * Prepares [sentences] of book [bookId] one after another, in the caller's coroutine; the
     * caller cancels it when the page or the speech settings change. Opening another book first
     * empties the cache.
     */
    suspend fun prepare(bookId: Long, sentences: List<String>, languageCode: String?) {
        if (this.bookId != bookId) {
            this.bookId = bookId
            cache.clear()
        }
        val wanted = sentences.filter { it.any(Char::isLetter) }.distinct()
        if (languageCode == null || engine() == null) {
            _states.value = emptyMap()
            return
        }
        _states.value = wanted.associateWith { if (cache.read(keyOf(it, languageCode) ?: "") != null) SentenceAudioState.READY else SentenceAudioState.PREPARING }
        for (sentence in wanted) audioFor(sentence, languageCode)
    }

    /**
     * The sentence's audio from the cache, or synthesized now (waiting for one already being made);
     * null when no local engine is chosen or it cannot speak the sentence.
     */
    suspend fun audioFor(text: String, languageCode: String): ByteArray? {
        val key = keyOf(text, languageCode) ?: return null
        cache.read(key)?.let { return it }
        val engine = engine() ?: return null
        return synthesis.withLock {
            cache.read(key) ?: try {
                val prefs = settings.current
                engine.synthesize(text, languageCode, prefs.speechVoices["${engine.engine.name}:$languageCode"], prefs.speechSpeed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }?.also { cache.write(key, it) }
        }.also { audio ->
            _states.update { states ->
                if (text !in states) states
                else if (audio != null) states + (text to SentenceAudioState.READY)
                else states - text
            }
        }
    }

    private fun engine(): LocalSpeechEngine? = localSpeech.find(settings.current.speechEngine)

    private fun keyOf(text: String, languageCode: String): String? {
        val engine = engine() ?: return null
        val prefs = settings.current
        val voice = prefs.speechVoices["${engine.engine.name}:$languageCode"].orEmpty()
        return "${engine.engine.name}|$voice|${prefs.speechSpeed}|$languageCode|$text"
    }
}
