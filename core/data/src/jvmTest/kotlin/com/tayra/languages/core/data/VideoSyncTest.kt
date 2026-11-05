package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.VideoRepositoryImpl
import com.tayra.languages.core.data.sync.SyncEngine
import com.tayra.languages.core.data.sync.SyncTransport
import com.tayra.languages.core.domain.media.Cue
import com.tayra.languages.core.domain.media.SubtitleKind
import com.tayra.languages.core.domain.media.SubtitleTrack
import com.tayra.languages.core.domain.media.TranslationSource
import com.tayra.languages.core.domain.media.VideoImport
import com.tayra.languages.core.domain.media.VideoKind
import com.tayra.languages.core.domain.media.VideoService
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.sync.PullResponse
import com.tayra.languages.sync.PushRequest
import com.tayra.languages.sync.PushResponse
import com.tayra.languages.sync.SyncMerge
import com.tayra.languages.sync.SyncRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Videos through the account: a video added on one device arrives with its lines and plays there from the same address. */
class VideoSyncTest {

    private class Account : SyncTransport {
        val records = linkedMapOf<Pair<String, String>, Pair<SyncRecord, Long>>()
        var seq = 0L
        override fun syncAccount() = "http://localhost me@example.com"
        override suspend fun push(request: PushRequest): PushResponse {
            for (incoming in request.records) {
                val id = incoming.kind to incoming.key
                val stored = records[id]?.first
                val merged = if (stored == null) incoming else SyncMerge.merge(stored, incoming)
                if (merged != stored) records[id] = merged to ++seq
            }
            return PushResponse(seq, System.currentTimeMillis())
        }
        override suspend fun pull(since: Long, limit: Int): PullResponse {
            val after = records.values.filter { it.second > since }.sortedBy { it.second }
            val page = after.take(limit)
            return PullResponse(page.map { it.first }, page.lastOrNull()?.second ?: since, more = after.size > limit)
        }
    }

    private class Device(account: Account) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-video-sync", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val videos = VideoRepositoryImpl(provider)
        val service = VideoService(videos, languages)
        val engine = SyncEngine(provider, account, MapSettings(), addLanguage = { if (it == "pt") portuguese() })

        suspend fun portuguese(): Long = languages.getAll().firstOrNull { it.name == "Portuguese" }?.id ?: languages.save(Language(name = "Portuguese"))
    }

    private val account = Account()
    private val phone = Device(account)
    private val desktop = Device(account)

    private suspend fun syncBoth() {
        phone.engine.syncNow()
        desktop.engine.syncNow()
        phone.engine.syncNow()
    }

    private val lines = listOf(Cue(1000, 2500, "Olá, tudo bem?", "Hello, how are you?"), Cue(3000, 5000, "O gato dorme."))

    @Test
    fun aYouTubeVideoArrivesWithItsLinesAndPlaysTheSameThere() = runBlocking<Unit> {
        desktop.portuguese()
        val id = phone.service.import(
            VideoImport(phone.portuguese(), "Notícias", VideoKind.YOUTUBE, "dQw4w9WgXcQ", SubtitleTrack(lines, SubtitleKind.YOUTUBE), translationSource = TranslationSource.YOUTUBE, channel = "Band", durationMs = 9000),
        )
        phone.videos.setPosition(id, 2600, final = true)
        syncBoth()

        val copy = desktop.videos.observeVideos().first().single().video
        assertEquals("Notícias", copy.title)
        assertEquals(VideoKind.YOUTUBE, copy.kind)
        assertEquals("dQw4w9WgXcQ", copy.media)
        assertEquals("Band", copy.channel)
        assertEquals(2600, copy.positionMs)
        assertEquals(SubtitleKind.YOUTUBE, copy.subtitleKind)
        val cues = desktop.videos.cues(copy.bookId)
        assertEquals(phone.videos.cues(id), cues)
        assertEquals("Hello, how are you?", cues.first().cue.translation)
        assertTrue(desktop.books.observeBooks(archived = false).first().none { it.id == copy.bookId }, "the video's text stays out of the books there too")
        assertEquals(0, desktop.engine.syncNow().changed, "nothing left to take")

        // Lines shifted on the desktop reach the phone; deleting the video there removes it here.
        desktop.service.shift(copy.bookId, 500)
        desktop.engine.syncNow()
        phone.engine.syncNow()
        assertEquals(listOf(1500L, 3500L), phone.videos.cues(id).map { it.cue.beginMs })
        desktop.books.deleteBook(copy.bookId)
        syncBoth()
        assertNull(phone.videos.video(id))
    }

    @Test
    fun aLinkedVideoArrivesWholeAndPlaysFromTheSameAddress() = runBlocking<Unit> {
        desktop.portuguese()
        phone.service.import(VideoImport(phone.portuguese(), "Aula", VideoKind.LINK, "https://videos.example/aula.m3u8", SubtitleTrack(lines)))
        syncBoth()
        val copy = desktop.videos.observeVideos().first().single().video
        assertEquals(VideoKind.LINK, copy.kind)
        assertEquals("https://videos.example/aula.m3u8", copy.media)
        assertEquals(2, desktop.videos.cues(copy.bookId).size)

        val phoneId = phone.videos.observeVideos().first().single().video.bookId
        phone.videos.setPosition(phoneId, 4000, final = true)
        syncBoth()
        assertEquals(4000, desktop.videos.video(copy.bookId)!!.positionMs)
    }
}
