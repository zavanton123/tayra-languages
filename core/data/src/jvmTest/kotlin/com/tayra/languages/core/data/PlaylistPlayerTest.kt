package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.CourseRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.PlaylistRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.playlists.ListeningPosition
import com.tayra.languages.core.domain.playlists.ListeningTime
import com.tayra.languages.core.domain.playlists.PlaybackProblem
import com.tayra.languages.core.domain.playlists.PlayerStatus
import com.tayra.languages.core.domain.playlists.PlaylistDraft
import com.tayra.languages.core.domain.playlists.PlaylistPlayer
import com.tayra.languages.core.domain.playlists.PlaylistService
import com.tayra.languages.core.domain.playlists.PlaylistSource
import com.tayra.languages.core.domain.playlists.RepeatMode
import com.tayra.languages.core.domain.playlists.SpeechOutput
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.service.TermService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Collections
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/** The playlist player reads texts aloud one after another, with fake voices standing in for the device's. */
class PlaylistPlayerTest {

    /** Records what is sounded; a clip or a sentence plays until the test finishes it. */
    private class FakeOutput : SpeechOutput {
        val heard: MutableList<String> = Collections.synchronizedList(mutableListOf())
        @Volatile var done: (() -> Unit)? = null
        @Volatile var onWord: ((IntRange) -> Unit)? = null
        @Volatile var paused = false
        @Volatile var rate = 1f

        override fun play(wav: ByteArray, onDone: () -> Unit) {
            heard += "wav:" + wav.decodeToString().substringAfter("|")
            done = onDone
        }

        override fun speak(text: String, languageCode: String?, rate: Float, onWord: (IntRange) -> Unit, onDone: () -> Unit) {
            heard += text
            this.rate = rate
            this.onWord = onWord
            done = onDone
        }

        override fun pause(): Boolean {
            paused = true
            return true
        }

        override fun resume() {
            paused = false
        }

        override fun stop() {
            done?.also { done = null }?.invoke()
        }

        /** Ends what is sounding, as the voice does at the end of a sentence. */
        fun finish() {
            done?.also { done = null }?.invoke()
        }
    }

    /** A local engine that speaks Portuguese only once a voice is "installed", making text-tagged clips. */
    private class FakePiper : LocalSpeechEngine {
        val made: MutableList<String> = Collections.synchronizedList(mutableListOf())
        @Volatile var installed = true
        @Volatile var speed = 0f
        override val engine = SpeechEngine.PIPER
        override val displayName = "Piper"
        override val description = ""
        override val packagesDescription = ""
        override val hasRuntimeSetup = false
        override val progress = MutableStateFlow<String?>(null)
        override suspend fun status() = "ready"
        override suspend fun isReady() = true
        override suspend fun setUp() = "ready"
        override suspend fun packages() = emptyList<SpeechPackage>()
        override suspend fun installPackage(id: String) {}
        override suspend fun removePackage(id: String) {}
        override suspend fun voices(languageCode: String) = if (installed && languageCode == "pt") listOf(SpeechVoice("pt", "Faber", "pt")) else emptyList()
        override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? {
            made += text
            this.speed = speed
            return "RIFF|$text".encodeToByteArray()
        }
    }

    private val file = File.createTempFile("tayra-player", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val bookService = BookService(books, languages)
    private val courses = CourseService(books, languages, bookService, CourseRepositoryImpl(provider), TestCourses.source)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val time = ListeningTime(settings)
    private val repository = PlaylistRepositoryImpl(provider)
    private val service = PlaylistService(repository, courses, languages, time)
    private val reading = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), TermService(terms, languages))
    private val piper = FakePiper()
    private val speech = LocalSpeech(listOf(piper))
    private val output = FakeOutput()
    private val thread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + thread)

    private fun newPlayer() = PlaylistPlayer(
        service, repository, books, languages, reading, SentenceAudio(speech, settings, MemorySpeechAudioCache()), speech, settings, output, time, scope,
        random = Random(7),
    )

    private val player = newPlayer()

    @AfterTest
    fun close() {
        scope.cancel()
        thread.close()
    }

    private fun until(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 10_000
        while (!condition()) {
            if (System.currentTimeMillis() > end) fail("Timed out waiting for $what; state ${player.state.value}, heard ${output.heard}")
            Thread.sleep(5)
        }
    }

    /** Waits for [text] to be sounding, then ends it. */
    private fun hear(text: String) {
        until("\"$text\"") { output.heard.lastOrNull() == text && output.done != null && player.state.value.status == PlayerStatus.PLAYING }
        output.finish()
    }

    private class Setup(val language: Long, val playlist: Long, val books: List<Long>)

    /** A playlist of a two-page text, "Um. Dois." then "Três.", and a one-page "Quatro. Cinco.". */
    private fun setUp(): Setup = runBlocking {
        val language = languages.save(Language(name = "Portuguese"))
        settings.update { it.copy(currentLanguageId = language) }
        val first = bookService.create(BookDraft(languageId = language, title = "Primeiro", text = "Um dois. Dois três.\n---\nTrês quatro."))
        val second = bookService.create(BookDraft(languageId = language, title = "Segundo", text = "Quatro cinco. Cinco seis."))
        val playlist = service.create(language, PlaylistDraft("Manhã"), listOf(PlaylistSource.Text(first), PlaylistSource.Text(second)))
        Setup(language, playlist, listOf(first, second))
    }

    @Test
    fun aPlaylistIsReadSentenceAfterSentencePageAfterPageAndTextAfterText() {
        val setup = setUp()
        assertEquals(2, runBlocking { books.pageCount(setup.books[0]) }, "the first text has two pages")
        player.play(setup.playlist)
        hear("Um dois.")
        until("the place to be kept") { runBlocking { service.playlist(setup.playlist)!!.position?.sentence } == 1 }
        // The voice reports the words it reaches.
        output.onWord!!(4..7)
        until("the word") { player.state.value.word == 4..7 }
        hear("Dois três.")
        until("the second page") { player.state.value.page == 2 }
        hear("Três quatro.")
        until("the first text to be finished") { runBlocking { service.playlist(setup.playlist)!!.items[0].finished } }
        assertEquals("Segundo", player.state.value.item?.title)
        hear("Quatro cinco.")
        hear("Cinco seis.")
        until("the end") { player.state.value.status == PlayerStatus.COMPLETE }
        val done = runBlocking { service.playlist(setup.playlist)!! }
        assertTrue(done.isComplete)
        assertNull(done.position, "a playlist heard to the end has no place left")
        // Listening is not reading: no page of either text was opened or read.
        assertTrue(runBlocking { setup.books.flatMap { books.getPages(it) } }.all { it.startDate == null && it.readDate == null })

        // Playing it again starts over.
        player.resume()
        hear("Um dois.")
        until("the texts to be unfinished") { runBlocking { service.playlist(setup.playlist)!!.finishedCount } == 0 }
    }

    @Test
    fun aLocalVoiceSynthesizesTheNextSentencesWhileOnePlays() {
        val setup = setUp()
        runBlocking { settings.update { it.copy(speechEngine = SpeechEngine.PIPER, playlistSpeed = 1.5f) } }
        player.play(setup.playlist)
        hear("wav:Um dois.")
        assertEquals(1.5f, piper.speed, "the clips are made at the player's speed")
        // While the first played, the rest of the page and the next page were made ready.
        until("the sentences ahead") { piper.made.containsAll(listOf("Um dois.", "Dois três.", "Três quatro.")) }
        hear("wav:Dois três.")
        assertEquals(1, piper.made.count { it == "Dois três." }, "a sentence made ahead is not made again")
    }

    @Test
    fun theControlsMoveThroughTheTextAndThePlaylist() {
        val setup = setUp()
        player.play(setup.playlist)
        until("the first sentence") { output.heard.lastOrNull() == "Um dois." }
        player.pause()
        until("the pause") { player.state.value.status == PlayerStatus.PAUSED && output.paused }
        player.resume()
        until("the resume") { player.state.value.status == PlayerStatus.PLAYING && !output.paused }

        player.nextSentence()
        hear("Dois três.")
        until("the next page") { output.heard.lastOrNull() == "Três quatro." }
        player.previousSentence()
        hear("Dois três.")
        player.next()
        hear("Quatro cinco.")
        // Previous from part-way into a text starts it again; from its start it goes back a text.
        until("the next sentence") { output.heard.lastOrNull() == "Cinco seis." }
        player.previous()
        until("the text's start") { output.heard.lastOrNull() == "Quatro cinco." && player.state.value.sentence == 0 && player.state.value.status == PlayerStatus.PLAYING }
        player.previous()
        until("the first text again") { output.heard.lastOrNull() == "Um dois." }
        // Seeking goes by sentence within the text: two thirds in is the second page.
        player.seek(0.7f)
        hear("Três quatro.")
        player.jumpTo(runBlocking { service.playlist(setup.playlist)!!.items[1].id })
        hear("Quatro cinco.")

        player.setSpeed(1.25f)
        hear("Cinco seis.")
        assertEquals(1.25f, settings.current.playlistSpeed)
    }

    @Test
    fun thePlayersSpeedIsItsOwnAndTheReadersStaysAsItWas() {
        val setup = setUp()
        runBlocking { settings.update { it.copy(speechSpeed = 0.8f) } }
        player.setSpeed(1.25f)
        until("the player's speed") { settings.current.playlistSpeed == 1.25f }
        assertEquals(0.8f, settings.current.speechSpeed, "the reader's read-aloud keeps its speed")
        player.play(setup.playlist)
        until("the first sentence") { output.heard.lastOrNull() == "Um dois." }
        assertEquals(1.25f, output.rate, "the system voice speaks at the player's speed")

        // The reader's speed changed, the player's is not.
        runBlocking { settings.update { it.copy(speechSpeed = 1.5f) } }
        output.finish()
        until("the next sentence") { output.heard.lastOrNull() == "Dois três." }
        assertEquals(1.25f, output.rate)
    }

    @Test
    fun shuffleKeepsTheCurrentTextAndRepeatPlaysAgain() {
        val setup = setUp()
        val third = runBlocking {
            bookService.create(BookDraft(languageId = setup.language, title = "Terceiro", text = "Sete oito.")).also { service.add(setup.playlist, listOf(PlaylistSource.Text(it))) }
        }
        player.play(setup.playlist, shuffle = true)
        until("the first sentence") { output.heard.lastOrNull() == "Um dois." }
        val order = player.state.value.order
        assertEquals(runBlocking { service.playlist(setup.playlist)!!.items[0].id }, order.first(), "the text playing stays first")
        assertEquals(3, order.toSet().size)

        player.setShuffle(false)
        player.setRepeat(RepeatMode.ONE)
        until("the playlist's order") { player.state.value.order == runBlocking { service.playlist(setup.playlist)!!.items.map { it.id } } }
        hear("Um dois.")
        hear("Dois três.")
        hear("Três quatro.")
        hear("Um dois.")
        player.setRepeat(RepeatMode.ALL)
        hear("Dois três.")
        hear("Três quatro.")
        hear("Quatro cinco.")
        hear("Cinco seis.")
        hear("Sete oito.")
        hear("Um dois.")
        assertTrue(third > 0)
    }

    @Test
    fun withoutAVoiceThePlayerSaysWhyAndCanUseTheSystemVoiceOrSkip() {
        val setup = setUp()
        piper.installed = false
        runBlocking { settings.update { it.copy(speechEngine = SpeechEngine.PIPER) } }
        player.play(setup.playlist)
        until("the problem") { player.state.value.status == PlayerStatus.BLOCKED }
        assertEquals(PlaybackProblem.NoVoice("Piper", setup.language, "Portuguese"), player.state.value.problem)
        assertTrue(output.heard.isEmpty())

        player.skip()
        until("the next text, still without a voice") { player.state.value.item?.title == "Segundo" && player.state.value.status == PlayerStatus.BLOCKED }
        player.useSystemVoice()
        hear("Quatro cinco.")
        assertTrue(piper.made.isEmpty())
        assertEquals(SpeechEngine.SYSTEM, settings.current.speechEngine, "the system voice is the settings' engine from now on")

        // It stays so after the player stops, even once the local voice is there again.
        until("the second sentence") { player.state.value.sentence == 1 }
        player.stop()
        until("the stop") { player.state.value.status == PlayerStatus.IDLE }
        piper.installed = true
        player.play(setup.playlist)
        until("the system voice again") { output.heard.lastOrNull() == "Cinco seis." }
        assertTrue(piper.made.isEmpty())
    }

    @Test
    fun anotherLanguageStopsThePlayerAndItsPlaceIsKept() {
        val setup = setUp()
        player.play(setup.playlist)
        hear("Um dois.")
        until("the second sentence") { output.heard.lastOrNull() == "Dois três." }
        val spanish = runBlocking { languages.save(Language(name = "Spanish")) }
        runBlocking { settings.update { it.copy(currentLanguageId = spanish) } }
        until("the stop") { player.state.value.status == PlayerStatus.IDLE }
        assertEquals(ListeningPosition(runBlocking { service.playlist(setup.playlist)!!.items[0].id }, 1, 1), runBlocking { service.playlist(setup.playlist)!!.position })

        // Another player, as after the app is opened again, goes on where listening got to.
        runBlocking { settings.update { it.copy(currentLanguageId = setup.language) } }
        val again = newPlayer()
        again.play(setup.playlist)
        until("the kept place") { output.heard.lastOrNull() == "Dois três." && again.state.value.sentence == 1 }
    }

    @Test
    fun theQueueFollowsChangesToThePlaylist() {
        val setup = setUp()
        player.play(setup.playlist)
        hear("Um dois.")
        until("the second sentence") { output.heard.lastOrNull() == "Dois três." }
        val items = runBlocking { service.playlist(setup.playlist)!!.items }
        // The text being heard is taken out: listening goes on with the next.
        runBlocking { service.remove(items[0].id) }
        until("the next text") { output.heard.lastOrNull() == "Quatro cinco." }
        assertEquals(listOf(items[1].id), player.state.value.order)
        // The playlist deleted, nothing is left to play.
        runBlocking { service.delete(setup.playlist) }
        until("the unload") { player.state.value.status == PlayerStatus.IDLE && player.state.value.playlist == null }
    }

    @Test
    fun aTextWithNothingToReadCanBeTakenOutOfThePlaylist() {
        val setup = setUp()
        runBlocking { books.getPages(setup.books[0]).forEach { books.updatePageText(it.id, "", 0) } }
        player.play(setup.playlist)
        until("the problem") { player.state.value.status == PlayerStatus.BLOCKED }
        assertEquals(PlaybackProblem.NoText("Primeiro"), player.state.value.problem)
        assertTrue(output.heard.isEmpty())

        player.removeCurrent()
        hear("Quatro cinco.")
        assertEquals(listOf("Segundo"), runBlocking { service.playlist(setup.playlist)!!.items.map { it.title } })
        assertNull(player.state.value.problem)
    }

    @Test
    fun aTextDeletedFromTheLibraryIsToldOnceAndListeningGoesOn() {
        val setup = setUp()
        player.play(setup.playlist)
        hear("Um dois.")
        runBlocking { bookService.delete(setup.books[0]) }
        until("the notice") { player.state.value.problem == PlaybackProblem.TextGone("Primeiro") }
        hear("Quatro cinco.")
        player.dismissProblem()
        until("the notice closed") { player.state.value.problem == null }
        assertTrue(player.state.value.isPlaying, "telling it does not pause the listening")
        hear("Cinco seis.")

        // Taken out of the playlist on purpose, a text is not reported as deleted.
        val other = runBlocking { service.create(setup.language, PlaylistDraft("Noite"), listOf(PlaylistSource.Text(setup.books[1]))) }
        val third = runBlocking { bookService.create(BookDraft(languageId = setup.language, title = "Terceiro", text = "Sete oito.")).also { service.add(other, listOf(PlaylistSource.Text(it))) } }
        player.play(other)
        hear("Quatro cinco.")
        runBlocking { service.remove(service.playlist(other)!!.items.first().id) }
        hear("Sete oito.")
        assertNull(player.state.value.problem)
        assertTrue(third > 0)
    }
}
