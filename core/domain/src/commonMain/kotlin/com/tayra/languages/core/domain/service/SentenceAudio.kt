package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

/**
 * Where synthesized sentence audio is kept, across texts and restarts, until it is emptied once a
 * week. It holds a bounded amount: past its limit the audio used longest ago goes first, and
 * reading audio counts as using it.
 */
interface SpeechAudioCache {
    suspend fun read(key: String): ByteArray?
    suspend fun write(key: String, audio: ByteArray)
    suspend fun clear()

    companion object {
        /** How much audio a cache on disk keeps: some hours of speech. */
        const val MAX_BYTES_ON_DISK: Long = 500L * 1024 * 1024

        /** How much a cache in memory keeps. */
        const val MAX_BYTES_IN_MEMORY: Long = 100L * 1024 * 1024
    }
}

/** A [SpeechAudioCache] in memory, for platforms without a file system and for tests. */
class MemorySpeechAudioCache(private val maxBytes: Long = SpeechAudioCache.MAX_BYTES_IN_MEMORY) : SpeechAudioCache {
    private val lock = Mutex()

    /** In order of use, the least recently used first. */
    private val entries = LinkedHashMap<String, ByteArray>()
    private var bytes = 0L

    override suspend fun read(key: String): ByteArray? = lock.withLock {
        entries.remove(key)?.also { entries[key] = it }
    }

    override suspend fun write(key: String, audio: ByteArray) = lock.withLock {
        entries.remove(key)?.let { bytes -= it.size }
        entries[key] = audio
        bytes += audio.size
        while (bytes > maxBytes && entries.size > 1) {
            val oldest = entries.keys.first()
            bytes -= entries.remove(oldest)!!.size
        }
    }

    override suspend fun clear() = lock.withLock {
        entries.clear()
        bytes = 0
    }
}

/** How far a sentence's audio has got. */
enum class SentenceAudioState { PREPARING, READY }

/**
 * Synthesizes sentences ahead of time with the local speech engine, so a sentence plays the
 * moment it is asked for: the reader's page and the examples of the term pane and the Examples
 * screen. The system voice speaks directly and needs none of this. Audio is made at normal speed,
 * the players playing it at the speed wanted, and cached per engine, voice and language, kept across
 * texts and restarts and emptied once a week.
 */
class SentenceAudio(
    private val localSpeech: LocalSpeech,
    private val settings: SettingsRepository,
    private val cache: SpeechAudioCache,
    /** Example recordings share the cache, so they are forgotten with it. */
    private val recordings: ExampleRecordings? = null,
    /** The time now, in epoch milliseconds. */
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val _states = MutableStateFlow<Map<String, SentenceAudioState>>(emptyMap())

    /** Prepared and preparing sentences, by text; a sentence missing here plays as before. */
    val states: StateFlow<Map<String, SentenceAudioState>> = _states.asStateFlow()

    /** One synthesis at a time: the local engines run a single worker each. */
    private val synthesis = Mutex()

    /** The engine and voices the states were made with; others make other audio. */
    private var configuration: String? = null

    /**
     * Prepares [sentences] one after another, in the caller's coroutine, which cancels it when what
     * it shows changes. Several callers may prepare at once; they take turns at the engine. The
     * cache is emptied first when a week has passed since it last was.
     */
    suspend fun prepare(sentences: List<String>, languageCode: String?) {
        emptyWeekly()
        val configuration = configuration()
        if (configuration != this.configuration) {
            this.configuration = configuration
            _states.value = emptyMap()
        }
        if (configuration == null || languageCode == null) return
        val wanted = sentences.filter { it.any(Char::isLetter) }.distinct()
        val marks = wanted.associateWith { if (cache.read(keyOf(it, languageCode) ?: "") != null) SentenceAudioState.READY else SentenceAudioState.PREPARING }
        _states.update { it + marks }
        try {
            for (sentence in wanted) audioFor(sentence, languageCode)
        } finally {
            // Whatever this run did not get to is no longer under way.
            _states.update { states -> states.filter { (text, state) -> text !in marks || state != SentenceAudioState.PREPARING } }
        }
    }

    /**
     * Of [sentences], those whose audio is not made yet, after the cache is emptied when its week is
     * up; none when no local engine is chosen or there is no [languageCode].
     */
    suspend fun missing(sentences: List<String>, languageCode: String?): List<String> {
        emptyWeekly()
        if (languageCode == null) return emptyList()
        return sentences.filter { it.any(Char::isLetter) }.distinct().filter { text -> keyOf(text, languageCode)?.let { cache.read(it) == null } == true }
    }

    /**
     * The sentence's audio at normal speed from the cache, or synthesized now (waiting for one
     * already being made); null when no local engine is chosen or it cannot speak the sentence.
     */
    suspend fun audioFor(text: String, languageCode: String): ByteArray? {
        val key = keyOf(text, languageCode) ?: return null
        cache.read(key)?.let { return it }
        val engine = engine() ?: return null
        return synthesis.withLock {
            cache.read(key) ?: try {
                val prefs = settings.current
                engine.synthesize(SpokenNumbers.normalize(text, languageCode), languageCode, prefs.speechVoices["${engine.engine.name}:$languageCode"], 1f)
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

    /** Empties the cache when a week has passed since it last was; the first week starts at its first use. */
    private suspend fun emptyWeekly() {
        val at = now()
        val last = settings.current.speechCacheClearedAt
        if (last != 0L && at - last < WEEK_MS) return
        if (last != 0L) {
            cache.clear()
            recordings?.reset()
            _states.value = emptyMap()
        }
        settings.update { it.copy(speechCacheClearedAt = at) }
    }

    private fun engine(): LocalSpeechEngine? = localSpeech.find(settings.current.speechEngine)

    private fun configuration(): String? {
        val engine = engine() ?: return null
        val prefs = settings.current
        return "${engine.engine.name}${engine.cacheVersion}|${prefs.speechVoices}"
    }

    /** Keyed by the text as spoken, so a clip cached before its numbers were rewritten is made again. */
    private fun keyOf(text: String, languageCode: String): String? {
        val engine = engine() ?: return null
        val voice = settings.current.speechVoices["${engine.engine.name}:$languageCode"].orEmpty()
        return "${engine.engine.name}${engine.cacheVersion}|$voice|$languageCode|${SpokenNumbers.normalize(text, languageCode)}"
    }

    private companion object {
        const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
    }
}
