package com.tayra.languages.core.domain.playlists

import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpokenNumbers
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random
import kotlin.time.Clock

/**
 * Where the playlist player is heard. Its calls come from the player's thread; the completions may
 * come from any thread.
 */
interface SpeechOutput {
    /** Plays WAV audio at [speed] (1 is normal), the pitch kept; [onDone] runs once when it ends, fails or is stopped. */
    fun play(wav: ByteArray, speed: Float, onDone: () -> Unit)

    /**
     * Speaks [text] with the system's voice at [rate] (1 is normal); [onWord] gets the characters
     * of each word as the voice reaches it, on voices that tell. [onDone] runs once when it ends,
     * fails or is stopped.
     */
    fun speak(text: String, languageCode: String?, rate: Float, onWord: (IntRange) -> Unit, onDone: () -> Unit)

    /** Holds the sound where it is, without ending it; false when it cannot be held. */
    fun pause(): Boolean
    fun resume()
    fun stop()
}

enum class PlayerStatus {
    /** Nothing is loaded. */
    IDLE,

    /** A text is being got ready, or a sentence synthesized, before it sounds. */
    LOADING,
    PLAYING,
    PAUSED,

    /** The text cannot be played; [PlayerState.problem] says why. */
    BLOCKED,

    /** Every text of the playlist was heard to the end. */
    COMPLETE,
}

enum class RepeatMode { OFF, ALL, ONE }

/** Why a text cannot be played. */
sealed interface PlaybackProblem {
    /** The speech engine chosen in the settings has no voice for the playlist's language on this device. */
    data class NoVoice(val engineName: String, val languageId: Long, val languageName: String) : PlaybackProblem

    /** The text has nothing to read aloud: no page, or only empty ones. */
    data class NoText(val title: String) : PlaybackProblem

    /**
     * The text being heard was deleted from the library, which took it out of the playlist too;
     * listening goes on with the next one. Said once, so the text is not missed without a word.
     */
    data class TextGone(val title: String) : PlaybackProblem
}

/** What the player has loaded and how far it has got. */
data class PlayerState(
    val status: PlayerStatus = PlayerStatus.IDLE,
    val playlist: Playlist? = null,
    val languageCode: String? = null,
    /** The playlist's items by id, in the order they are played: the playlist's, or shuffled. */
    val order: List<Long> = emptyList(),
    val itemId: Long? = null,
    /** The page of the current text, from 1. */
    val page: Int = 1,
    val pageCount: Int = 0,
    /** The page's spoken sentences, split as the reader splits them. */
    val sentences: List<String> = emptyList(),
    /** The sentence being heard, from 0. */
    val sentence: Int = 0,
    /** The characters of the sentence's word being spoken, when the voice tells. */
    val word: IntRange? = null,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val problem: PlaybackProblem? = null,
    /** How much of the current text, in characters, comes before the current sentence, and how long it is. */
    val heard: Int = 0,
    val textLength: Int = 0,
) {
    val item: PlaylistItem? get() = playlist?.items?.firstOrNull { it.id == itemId }

    /** The place of the current item in [order], from 0. */
    val queueIndex: Int get() = order.indexOf(itemId)

    val currentSentence: String? get() = sentences.getOrNull(sentence)

    /** Whether something is loaded, playing or not. */
    val isLoaded: Boolean get() = status != PlayerStatus.IDLE && playlist != null

    val isPlaying: Boolean get() = status == PlayerStatus.PLAYING || status == PlayerStatus.LOADING

    /** How far the current text has got, from 0 to 1. */
    val textProgress: Float get() = if (textLength <= 0) 0f else (heard.toFloat() / textLength).coerceIn(0f, 1f)

    val hasNext: Boolean get() = queueIndex in 0 until order.size - 1 || (repeat == RepeatMode.ALL && order.isNotEmpty())
}

/**
 * Plays playlists aloud, text after text, page after page, sentence after sentence, splitting pages
 * as the reader does. It lives as long as the app, so it keeps playing whatever screen is open, and
 * everything it does is in [state] and the commands below, which a phone's media controls can drive
 * too. Only the language learned is spoken.
 *
 * With a local engine (Piper, Kokoro) the next sentences are synthesized while one plays, so they
 * follow without a gap; the system voice speaks directly. Where listening got to is kept with the
 * playlist; it never counts as reading. Choosing another language to learn stops it.
 */
class PlaylistPlayer(
    private val service: PlaylistService,
    private val repository: PlaylistRepository,
    private val books: BookRepository,
    private val languages: LanguageRepository,
    private val reading: ReadingService,
    private val audio: SentenceAudio,
    private val local: LocalSpeech,
    private val settings: SettingsRepository,
    private val output: SpeechOutput,
    private val time: ListeningTime,
    /** Runs everything the player does, one thing at a time; the app's main thread. */
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
    private val random: Random = Random.Default,
) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** One command at a time, each with the reading loop stopped while it changes the place. */
    private val commands = Mutex()
    private var loop: Job? = null
    private var prefetch: Job? = null
    private var watch: Job? = null

    /** Set when the sound is held by the output, so play goes on from there instead of from the sentence's start. */
    private var heldInOutput = false

    /** The lengths of the current text's pages, for the place within it and seeking. */
    private var pageLengths: List<Int> = emptyList()

    /** Increases with every sentence sounded, so a late word of an earlier one changes nothing. */
    private var request = 0

    init {
        scope.launch {
            settings.settings.map { it.currentLanguageId }.distinctUntilChanged().drop(1).collect { id ->
                if (_state.value.playlist?.languageId != id) stop()
            }
        }
    }

    // Commands.

    /**
     * Plays the playlist with [playlistId]: from [itemId]'s start when given, else from where
     * listening got to, else from its first text; a playlist heard to the end starts over.
     * [shuffle] given sets shuffling for it. The playlist already loaded goes on where it is.
     */
    fun play(playlistId: Long, itemId: Long? = null, shuffle: Boolean? = null) = command {
        val loaded = _state.value
        if (loaded.isLoaded && loaded.playlist?.id == playlistId && itemId == null && shuffle == null && loaded.status != PlayerStatus.COMPLETE) {
            resumeLocked()
            return@command
        }
        halt()
        val playlist = service.playlist(playlistId) ?: return@command
        if (playlist.items.isEmpty()) return@command
        if (watch == null || loaded.playlist?.id != playlistId) startWatching(playlistId)
        val shuffled = shuffle ?: loaded.shuffle.takeIf { loaded.playlist?.id == playlistId } ?: false
        var fresh = playlist
        val position = when {
            itemId != null -> ListeningPosition(itemId, 1, 0)
            playlist.position != null -> playlist.position
            else -> {
                if (playlist.items.all { it.finished }) {
                    repository.clearFinished(playlist.id)
                    fresh = service.playlist(playlist.id) ?: return@command
                }
                ListeningPosition((fresh.items.firstOrNull { !it.finished } ?: fresh.items.first()).id, 1, 0)
            }
        }
        _state.value = PlayerState(
            status = PlayerStatus.LOADING,
            playlist = fresh,
            languageCode = service.languageCode(fresh.languageId),
            order = orderOf(fresh, shuffled, first = position.itemId),
            itemId = position.itemId,
            shuffle = shuffled,
            repeat = loaded.repeat,
        )
        if (load(position.itemId, position.page, position.sentence)) startLoop()
    }

    /** Goes on with what is loaded, or plays it again from the start when it was heard to the end. */
    fun resume() = command {
        if (_state.value.status == PlayerStatus.COMPLETE) playAgainLocked() else resumeLocked()
    }

    fun pause() = command {
        val s = _state.value
        if (!s.isPlaying) return@command
        if (s.status == PlayerStatus.PLAYING && output.pause()) {
            heldInOutput = true
        } else {
            halt()
        }
        _state.update { it.copy(status = PlayerStatus.PAUSED, word = null) }
    }

    fun toggle() {
        if (_state.value.isPlaying) pause() else resume()
    }

    /** Starts the loaded playlist over from its first text, as after it was heard to the end. */
    fun playAgain() = command { playAgainLocked() }

    /** Plays the next text from its start. */
    fun next() = command {
        val s = _state.value
        val next = s.order.getOrNull(s.queueIndex + 1) ?: s.order.firstOrNull()?.takeIf { s.repeat == RepeatMode.ALL } ?: return@command
        jump(next)
    }

    /** Plays the current text again from its start, or the text before it when it is at its start already. */
    fun previous() = command {
        val s = _state.value
        val id = s.itemId ?: return@command
        val atStart = s.page == 1 && s.sentence == 0
        jump(if (atStart) s.order.getOrNull(s.queueIndex - 1) ?: id else id)
    }

    /** Plays the item with [itemId] from its start. */
    fun jumpTo(itemId: Long) = command { jump(itemId) }

    fun nextSentence() = command {
        val s = _state.value
        if (!s.isLoaded) return@command
        if (s.sentence + 1 < s.sentences.size) moveTo(s.page, s.sentence + 1)
        else if (s.page < s.pageCount) moveTo(s.page + 1, 0)
    }

    fun previousSentence() = command {
        val s = _state.value
        if (!s.isLoaded) return@command
        when {
            s.sentence > 0 -> moveTo(s.page, s.sentence - 1)
            s.page > 1 -> moveTo(s.page - 1, Int.MAX_VALUE)
            else -> moveTo(1, 0)
        }
    }

    /** Moves to the sentence at [fraction] (0 to 1) of the current text. */
    fun seek(fraction: Float) = command {
        val s = _state.value
        if (!s.isLoaded || pageLengths.isEmpty()) return@command
        val target = (fraction.coerceIn(0f, 1f) * pageLengths.sum()).toInt()
        var before = 0
        var page = pageLengths.size
        for ((i, length) in pageLengths.withIndex()) {
            if (target < before + length || i == pageLengths.lastIndex) {
                page = i + 1
                break
            }
            before += length
        }
        val sentences = if (page == s.page) s.sentences else sentencesOf(s.item?.bookId ?: return@command, page)
        val within = (target - before).toFloat() / pageLengths[page - 1].coerceAtLeast(1)
        val total = sentences.sumOf { it.length }.coerceAtLeast(1)
        var sum = 0
        val sentence = sentences.indexOfFirst { sum += it.length; sum.toFloat() / total > within }.coerceAtLeast(0)
        moveTo(page, sentence, sentences)
    }

    fun setShuffle(on: Boolean) = command {
        val s = _state.value
        val playlist = s.playlist
        _state.update { it.copy(shuffle = on, order = if (playlist == null) it.order else orderOf(playlist, on, first = s.itemId)) }
    }

    fun setRepeat(mode: RepeatMode) = command { _state.update { it.copy(repeat = mode) } }

    /** The speed of speech, 1 for normal; the player's own, remembered apart from the reader's. */
    fun setSpeed(speed: Float) = command { settings.update { it.copy(playlistSpeed = speed) } }

    /** Passes over the text that cannot be played, to the next one. */
    fun skip() = command {
        val s = _state.value
        _state.update { it.copy(problem = null) }
        val next = s.order.getOrNull(s.queueIndex + 1) ?: s.order.firstOrNull()?.takeIf { s.repeat == RepeatMode.ALL }
        if (next == null) complete() else jump(next)
    }

    /**
     * Speaks with the system's voice, as the text has no voice of the engine chosen: it becomes the
     * speech engine of the settings, for the reader too, until another is chosen there.
     */
    fun useSystemVoice() = command {
        settings.update { it.copy(speechEngine = SpeechEngine.SYSTEM) }
        val s = _state.value
        _state.update { it.copy(problem = null) }
        s.itemId?.let { if (load(it, s.page, s.sentence)) startLoop() }
    }

    /** Takes the text that cannot be played out of the playlist; listening goes on with the next one. */
    fun removeCurrent() = command {
        val s = _state.value
        val itemId = s.itemId ?: return@command
        // Marked as going on, so the change to the playlist starts the next text rather than pausing on it.
        _state.update { it.copy(problem = null, status = PlayerStatus.LOADING) }
        service.remove(itemId)
    }

    /**
     * Closes the reason the text cannot be played, leaving it paused there; a text deleted from the
     * library only had to be told, and listening goes on as it was.
     */
    fun dismissProblem() = command {
        when (_state.value.problem) {
            null -> {}
            is PlaybackProblem.TextGone -> _state.update { it.copy(problem = null) }
            else -> _state.update { it.copy(problem = null, status = PlayerStatus.PAUSED) }
        }
    }

    /** Stops and unloads the playlist, keeping where listening got to. */
    fun stop() = command { unload() }

    // Internals; each runs inside a command or the loop, on the player's scope.

    private fun command(block: suspend () -> Unit) {
        scope.launch { commands.withLock { block() } }
    }

    private suspend fun resumeLocked() {
        val s = _state.value
        when {
            !s.isLoaded || s.isPlaying -> return
            s.status == PlayerStatus.BLOCKED -> s.itemId?.let { if (load(it, s.page, s.sentence)) startLoop() }
            heldInOutput -> {
                heldInOutput = false
                output.resume()
                _state.update { it.copy(status = PlayerStatus.PLAYING) }
            }
            else -> {
                _state.update { it.copy(status = PlayerStatus.LOADING) }
                // A text whose voice was missing is checked again, in case one was downloaded since.
                if (s.problem != null || s.status == PlayerStatus.PAUSED && s.sentences.isEmpty()) {
                    s.itemId?.let { if (load(it, s.page, s.sentence)) startLoop() }
                } else {
                    startLoop()
                }
            }
        }
    }

    private suspend fun playAgainLocked() {
        val playlist = _state.value.playlist ?: return
        halt()
        repository.clearFinished(playlist.id)
        val fresh = service.playlist(playlist.id) ?: return
        val shuffle = _state.value.shuffle
        val order = orderOf(fresh, shuffle, first = null)
        _state.update { it.copy(playlist = fresh, order = order, status = PlayerStatus.LOADING) }
        if (load(order.first(), 1, 0)) startLoop()
    }

    private suspend fun jump(itemId: Long) {
        val wasPaused = _state.value.status == PlayerStatus.PAUSED
        halt()
        if (load(itemId, 1, 0)) {
            if (wasPaused) _state.update { it.copy(status = PlayerStatus.PAUSED) } else startLoop()
        }
    }

    /** Moves within the current text, playing on from there if it was playing. */
    private suspend fun moveTo(page: Int, sentence: Int, known: List<String>? = null) {
        val s = _state.value
        val bookId = s.item?.bookId ?: return
        val playing = s.isPlaying
        halt()
        val sentences = known ?: if (page == s.page) s.sentences else sentencesOf(bookId, page)
        _state.update { it.copy(page = page, sentences = sentences, sentence = sentence.coerceIn(0, (sentences.size - 1).coerceAtLeast(0)), word = null) }
        updateHeard()
        savePosition()
        if (playing) startLoop() else _state.update { it.copy(status = PlayerStatus.PAUSED) }
    }

    /** Stops the reading loop and the sound, leaving the place as it is. */
    private suspend fun halt() {
        prefetch?.cancel()
        prefetch = null
        loop?.cancelAndJoin()
        loop = null
        heldInOutput = false
        output.stop()
    }

    private suspend fun unload() {
        halt()
        // The place reached is kept, unless the playlist was heard to the end or is gone.
        if (_state.value.status != PlayerStatus.COMPLETE && _state.value.status != PlayerStatus.IDLE) savePosition()
        watch?.cancel()
        watch = null
        pageLengths = emptyList()
        _state.value = PlayerState(shuffle = _state.value.shuffle, repeat = _state.value.repeat)
    }

    /**
     * Loads [page] of the item with [itemId], at [sentence]; false, with the player blocked or the
     * playlist complete, when it cannot be played.
     */
    private suspend fun load(itemId: Long, page: Int, sentence: Int): Boolean {
        val s = _state.value
        val playlist = s.playlist ?: return false
        val item = playlist.items.firstOrNull { it.id == itemId } ?: return false
        _state.update { it.copy(itemId = itemId, status = PlayerStatus.LOADING, word = null, problem = null) }
        problemFor(playlist.languageId, s.languageCode)?.let { problem ->
            _state.update { it.copy(status = PlayerStatus.BLOCKED, problem = problem, sentences = emptyList(), page = page, sentence = sentence) }
            return false
        }
        pageLengths = books.getPages(item.bookId).map { it.text.length }
        if (pageLengths.sum() == 0) {
            _state.update { it.copy(status = PlayerStatus.BLOCKED, problem = PlaybackProblem.NoText(item.title), sentences = emptyList(), page = 1, pageCount = 0, sentence = 0) }
            return false
        }
        val pageCount = pageLengths.size
        val at = page.coerceIn(1, pageCount.coerceAtLeast(1))
        val sentences = sentencesOf(item.bookId, at)
        _state.update {
            it.copy(page = at, pageCount = pageCount, sentences = sentences, sentence = sentence.coerceIn(0, (sentences.size - 1).coerceAtLeast(0)), textLength = pageLengths.sum())
        }
        updateHeard()
        return true
    }

    /** Why the playlist's language cannot be spoken now, or null when it can. */
    private suspend fun problemFor(languageId: Long, code: String?): PlaybackProblem? {
        val engine = engineInUse() ?: return null
        if (code == null) return null
        val speaks = try {
            engine.isReady() && engine.voices(code).isNotEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
        if (speaks) return null
        return PlaybackProblem.NoVoice(engine.displayName, languageId, languages.getById(languageId)?.name.orEmpty())
    }

    private fun engineInUse(): LocalSpeechEngine? = local.find(settings.current.speechEngine)

    /** The page's sentences that are read aloud: those with a letter, as the reader's play buttons have. */
    private suspend fun sentencesOf(bookId: Long, page: Int): List<String> =
        reading.sentenceTexts(bookId, page).orEmpty().filter { text -> text.any { it.isLetter() } }

    private fun startLoop() {
        loop = scope.launch { run() }
    }

    /** Reads on from the current sentence until the playlist ends or a command stops it. */
    private suspend fun run() {
        while (true) {
            val s = _state.value
            val text = s.currentSentence
            if (text == null) {
                if (!advance()) return
                continue
            }
            savePosition()
            speak(text)
            val now = _state.value
            if (now.sentence + 1 < now.sentences.size) {
                _state.update { it.copy(sentence = it.sentence + 1, word = null) }
                updateHeard()
            } else if (!advance()) {
                return
            }
        }
    }

    /** Turns to the next page, or the next text; false when there is nothing more to play. */
    private suspend fun advance(): Boolean {
        val s = _state.value
        val item = s.item ?: return false.also { complete() }
        if (s.page < s.pageCount) {
            val sentences = sentencesOf(item.bookId, s.page + 1)
            _state.update { it.copy(page = it.page + 1, sentences = sentences, sentence = 0, word = null) }
            updateHeard()
            return true
        }
        repository.setFinished(item.id, clock.now())
        val next = when {
            s.repeat == RepeatMode.ONE -> item.id
            s.queueIndex + 1 < s.order.size -> s.order[s.queueIndex + 1]
            s.repeat == RepeatMode.ALL -> {
                s.playlist?.let { repository.clearFinished(it.id) }
                if (s.shuffle) s.playlist?.let { p -> _state.update { it.copy(order = orderOf(p, true, first = null)) } }
                _state.value.order.firstOrNull()
            }
            else -> null
        }
        if (next == null) {
            complete()
            return false
        }
        return load(next, 1, 0)
    }

    private suspend fun complete() {
        val s = _state.value
        s.playlist?.let { repository.savePosition(it.id, null, clock.now()) }
        _state.update { it.copy(status = PlayerStatus.COMPLETE, word = null, sentence = 0) }
    }

    /** Sounds [text] and waits until it has been heard; synthesized ahead with a local engine, said by the system voice otherwise. */
    private suspend fun speak(text: String) {
        val code = _state.value.languageCode
        val token = ++request
        val engine = engineInUse()
        val speed = settings.current.playlistSpeed
        val wav = if (engine != null && code != null) {
            if (_state.value.status != PlayerStatus.PLAYING) _state.update { it.copy(status = PlayerStatus.LOADING) }
            audio.audioFor(text, code)
        } else {
            null
        }
        prepareAhead()
        _state.update { it.copy(status = PlayerStatus.PLAYING) }
        val done = CompletableDeferred<Unit>()
        try {
            if (wav != null) {
                // The clip is at normal speed, whatever speed it is played at.
                wavSeconds(wav)?.let { time.record(code!!, text.length, it, 1f) }
                output.play(wav, speed) { done.complete(Unit) }
            } else {
                // A local voice that failed on this sentence leaves it to the system voice, as the reader does.
                val spoken = SpokenNumbers.normalize(text, code)
                val onWord: (IntRange) -> Unit = if (spoken == text) { range -> scope.launch { if (request == token) _state.update { it.copy(word = range) } } } else { _ -> }
                output.speak(spoken, code, speed, onWord) { done.complete(Unit) }
            }
            done.await()
        } catch (e: CancellationException) {
            output.stop()
            throw e
        }
    }

    /** Synthesizes the sentences after the current one, on to the next page and text, while it plays. */
    private fun prepareAhead() {
        val s = _state.value
        val code = s.languageCode ?: return
        if (engineInUse() == null || prefetch?.isActive == true) return
        prefetch = scope.launch {
            val ahead = s.sentences.drop(s.sentence + 1).take(LOOK_AHEAD).toMutableList()
            if (ahead.size < LOOK_AHEAD) {
                val bookId = s.item?.bookId
                if (bookId != null && s.page < s.pageCount) {
                    ahead += sentencesOf(bookId, s.page + 1).take(LOOK_AHEAD - ahead.size)
                } else {
                    val nextItem = s.order.getOrNull(s.queueIndex + 1)?.let { id -> s.playlist?.items?.firstOrNull { it.id == id } }
                    if (nextItem != null) ahead += sentencesOf(nextItem.bookId, 1).take(LOOK_AHEAD - ahead.size)
                }
            }
            for (sentence in ahead) audio.audioFor(sentence, code)
        }
    }

    private fun updateHeard() {
        val s = _state.value
        val before = pageLengths.take(s.page - 1).sum()
        val page = pageLengths.getOrElse(s.page - 1) { 0 }
        val total = s.sentences.sumOf { it.length }
        val within = if (total == 0) 0 else page * s.sentences.take(s.sentence).sumOf { it.length } / total
        _state.update { it.copy(heard = before + within) }
    }

    private suspend fun savePosition() {
        val s = _state.value
        val playlist = s.playlist ?: return
        val itemId = s.itemId ?: return
        repository.savePosition(playlist.id, ListeningPosition(itemId, s.page, s.sentence), clock.now())
    }

    /** The items in playlist order, or shuffled with [first] kept in front. */
    private fun orderOf(playlist: Playlist, shuffle: Boolean, first: Long?): List<Long> {
        val ids = playlist.items.map { it.id }
        if (!shuffle) return ids
        val rest = ids.filter { it != first }.shuffled(random)
        return if (first != null && first in ids) listOf(first) + rest else rest
    }

    /** Follows changes to the loaded playlist: its texts added, removed and moved, and the playlist deleted. */
    private fun startWatching(playlistId: Long) {
        watch?.cancel()
        watch = scope.launch {
            service.observePlaylist(playlistId).drop(1).collect { playlist ->
                commands.withLock { followChange(playlist) }
            }
        }
    }

    private suspend fun followChange(playlist: Playlist?) {
        val s = _state.value
        if (s.playlist == null) return
        if (playlist == null) {
            unload()
            return
        }
        val ids = playlist.items.map { it.id }
        val order = if (s.shuffle) s.order.filter { it in ids } + ids.filter { it !in s.order } else ids
        val current = s.itemId
        _state.update { it.copy(playlist = playlist, order = order) }
        if (current != null && current !in ids) {
            // The text being heard was taken out: listening goes on with the one that came after it.
            val wasPlaying = s.isPlaying
            halt()
            // Deleted from the library rather than taken out of the playlist: that is said, once.
            val gone = s.item?.takeIf { books.getBook(it.bookId) == null }?.let { PlaybackProblem.TextGone(it.title) }
            val next = s.order.drop(s.queueIndex + 1).firstOrNull { it in ids }
            when {
                next == null -> if (ids.isEmpty()) unload() else complete()
                load(next, 1, 0) -> if (wasPlaying) startLoop() else _state.update { it.copy(status = PlayerStatus.PAUSED) }
            }
            if (gone != null && _state.value.problem == null && _state.value.playlist != null) _state.update { it.copy(problem = gone) }
        }
    }

    private companion object {
        /** How many sentences are synthesized ahead of the one playing. */
        const val LOOK_AHEAD = 3
    }
}
