package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.VideoRepositoryImpl
import com.tayra.languages.core.domain.media.Cue
import com.tayra.languages.core.domain.media.SubtitleFormats
import com.tayra.languages.core.domain.media.SubtitleKind
import com.tayra.languages.core.domain.media.SubtitleTrack
import com.tayra.languages.core.domain.media.TranslationSource
import com.tayra.languages.core.domain.media.VideoImport
import com.tayra.languages.core.domain.media.VideoImportException
import com.tayra.languages.core.domain.media.VideoKind
import com.tayra.languages.core.domain.media.VideoService
import com.tayra.languages.core.domain.model.Language
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Videos: made from subtitles into a transcript with timed lines, kept out of the books. */
class VideoRepositoryTest {
    private val file = File.createTempFile("tayra-videos", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val videos = VideoRepositoryImpl(provider)
    private val service = VideoService(videos, languages)

    private val srt = """
        1
        00:00:01,000 --> 00:00:02,500
        <i>Olá, tudo bem?</i>

        2
        00:00:03,000 --> 00:00:05,000
        [Música]

        3
        00:00:05,000 --> 00:00:07,000
        - O gato dorme.
        - E a casa?
    """.trimIndent()

    private suspend fun portuguese() = languages.save(Language(name = "Portuguese"))

    @Test
    fun aVideoIsATranscriptWithTimedLinesListedApartFromBooks() = runTest {
        val language = portuguese()
        val translation = listOf(Cue(1000, 2500, "Hello, how are you?"), Cue(5000, 7000, "The cat sleeps. And the house?"))
        val id = service.import(
            VideoImport(
                languageId = language,
                title = " O gato ",
                kind = VideoKind.LINK,
                media = "https://videos.example/gato.mp4",
                subtitles = SubtitleTrack(SubtitleFormats.parse(srt)),
                translation = translation,
                translationSource = TranslationSource.FILE,
                durationMs = 8000,
            ),
        )
        val video = assertNotNull(videos.video(id))
        assertEquals("O gato", video.title)
        assertEquals(VideoKind.LINK, video.kind)
        assertEquals(8000, video.durationMs)

        val cues = videos.cues(id)
        assertEquals(listOf("Olá, tudo bem?", "O gato dorme. E a casa?"), cues.map { it.cue.text })
        assertEquals(listOf(1000L, 5000L), cues.map { it.cue.beginMs })
        assertEquals(listOf("Hello, how are you?", "The cat sleeps. And the house?"), cues.map { it.cue.translation })
        assertEquals(listOf(TranslationSource.FILE, TranslationSource.FILE), cues.map { it.translationSource })
        assertEquals(listOf(1 to 0, 1 to 1), cues.map { it.pageOrder to it.lineIndex })

        assertEquals("Olá, tudo bem?\nO gato dorme. E a casa?", books.getPage(id, 1)!!.text)
        assertTrue(books.observeBooks(archived = false).first().none { it.id == id }, "videos are not listed with the books")
        assertEquals(listOf(id), videos.observeVideos().first().map { it.video.bookId })
        assertTrue(videos.isVideo(id))
        assertEquals(id, videos.findByMedia(VideoKind.LINK, "https://videos.example/gato.mp4", language))
    }

    @Test
    fun aLongVideoIsCutIntoPagesAtLineBoundaries() = runTest {
        val language = portuguese()
        val lines = List(VideoService.LINES_PER_PAGE + 5) { Cue(it * 1000L, it * 1000L + 900, "Linha número $it.") }
        val id = service.import(VideoImport(language, "Longo", VideoKind.LINK, "https://videos.example/a.webm", SubtitleTrack(lines)))
        assertEquals(2, books.pageCount(id))
        val last = videos.cues(id).last()
        assertEquals(2 to 4, last.pageOrder to last.lineIndex)
        assertEquals(5, books.getPage(id, 2)!!.text.lines().size)
    }

    @Test
    fun automaticCaptionsAreJoinedIntoLines() = runTest {
        val language = portuguese()
        val pieces = listOf(Cue(0, 400, "eu acho"), Cue(400, 900, "que sim"), Cue(2000, 2400, "mas talvez"), Cue(2400, 2900, "não"))
        val id = service.import(VideoImport(language, "Auto", VideoKind.YOUTUBE, "dQw4w9WgXcQ", SubtitleTrack(pieces, SubtitleKind.YOUTUBE_AUTO)))
        assertEquals(listOf("eu acho que sim", "mas talvez não"), videos.cues(id).map { it.cue.text })
        assertEquals("youtube:dQw4w9WgXcQ", books.getBook(id)!!.sourceUri)
        assertEquals("dQw4w9WgXcQ", videos.video(id)!!.youtubeId)
    }

    @Test
    fun subtitlesCanBeReplacedShiftedAndGivenATranslationLater() = runTest {
        val language = portuguese()
        val id = service.import(VideoImport(language, "O gato", VideoKind.LINK, "https://videos.example/g.mp4", SubtitleTrack(SubtitleFormats.parse(srt))))
        service.shift(id, -1500)
        assertEquals(listOf(0L to 1000L, 3500L to 5500L), videos.cues(id).map { it.cue.beginMs to it.cue.endMs })

        service.attachTranslation(id, listOf(Cue(3500, 5500, "The cat sleeps.")), TranslationSource.YOUTUBE)
        assertEquals(listOf(null, "The cat sleeps."), videos.cues(id).map { it.cue.translation })
        assertEquals(TranslationSource.YOUTUBE, videos.cues(id)[1].translationSource)

        service.replaceSubtitles(id, SubtitleTrack(listOf(Cue(0, 1000, "Uma linha só.")), SubtitleKind.TRANSCRIBED))
        assertEquals(listOf("Uma linha só."), videos.cues(id).map { it.cue.text })
        assertEquals("Uma linha só.", books.getPage(id, 1)!!.text)
        assertEquals(SubtitleKind.TRANSCRIBED, videos.video(id)!!.subtitleKind)

        service.removeTranslations(id)
        assertNull(videos.cues(id).single().cue.translation)
    }

    @Test
    fun watchingPlaceIsKeptAndDeletingTheBookDeletesTheVideo() = runTest {
        val language = portuguese()
        val id = service.import(VideoImport(language, "O gato", VideoKind.LINK, "https://videos.example/g.mp4", SubtitleTrack(SubtitleFormats.parse(srt))))
        videos.setPosition(id, 4200, final = false)
        videos.setDuration(id, 9000)
        videos.setMedia(id, "https://cdn.example/g.mp4")
        val video = videos.video(id)!!
        assertEquals(4200L to 9000L, video.positionMs to video.durationMs)
        assertEquals("https://cdn.example/g.mp4", video.media)
        books.deleteBook(id)
        assertNull(videos.video(id))
        assertEquals(emptyList(), videos.cues(id))
    }

    @Test
    fun subtitlesWithoutLinesOrATitleAreRefused() = runTest {
        val language = portuguese()
        assertFailsWith<VideoImportException> { service.import(VideoImport(language, "Vazio", VideoKind.LINK, "https://videos.example/v.mp4", SubtitleTrack(listOf(Cue(0, 1, "[Música]"))))) }
        assertFailsWith<VideoImportException> { service.import(VideoImport(language, " ", VideoKind.LINK, "https://videos.example/v.mp4", SubtitleTrack(SubtitleFormats.parse(srt)))) }
    }

    @Test
    fun aDatabaseFromBeforeVideosGetsThem() = runTest {
        portuguese()
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.pretendVersion(35) }
        val reopened = DatabaseProvider(DatabaseDriverFactory(file)).also { it.database() }
        val again = VideoService(VideoRepositoryImpl(reopened), LanguageRepositoryImpl(reopened))
        val language = LanguageRepositoryImpl(reopened).findByName("Portuguese")!!.id
        val id = again.import(VideoImport(language, "Depois", VideoKind.LINK, "https://videos.example/d.mp4", SubtitleTrack(SubtitleFormats.parse(srt))))
        assertEquals(2, VideoRepositoryImpl(reopened).cues(id).size)
    }

    @Test
    fun videoFilesOfOlderLibrariesAreRemoved() = runTest {
        val language = portuguese()
        val link = service.import(VideoImport(language, "Na rede", VideoKind.LINK, "https://videos.example/n.mp4", SubtitleTrack(SubtitleFormats.parse(srt))))
        val onDisk = service.import(VideoImport(language, "No disco", VideoKind.LINK, "/Users/me/aula.mp4", SubtitleTrack(SubtitleFormats.parse(srt))))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use {
            it.createStatement().execute("UPDATE videos SET kind = 'FILE' WHERE book_id = $onDisk")
            it.pretendVersion(37)
        }
        val reopened = DatabaseProvider(DatabaseDriverFactory(file)).also { it.database() }
        val again = VideoRepositoryImpl(reopened)
        assertNotNull(again.video(link))
        assertNull(again.video(onDisk))
        assertNull(BookRepositoryImpl(reopened).getBook(onDisk), "its transcript goes with it")
    }
}
