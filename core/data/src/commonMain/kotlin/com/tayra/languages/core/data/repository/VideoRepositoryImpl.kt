package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.TayraDatabase
import com.tayra.languages.core.data.db.Video_cues
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.media.Cue
import com.tayra.languages.core.domain.media.NewVideo
import com.tayra.languages.core.domain.media.StoredCue
import com.tayra.languages.core.domain.media.SubtitleKind
import com.tayra.languages.core.domain.media.TermVideoSource
import com.tayra.languages.core.domain.media.TranslationSource
import com.tayra.languages.core.domain.media.Video
import com.tayra.languages.core.domain.media.VideoKind
import com.tayra.languages.core.domain.media.VideoListItem
import com.tayra.languages.core.domain.media.VideoRepository
import com.tayra.languages.core.domain.media.VideoTranscript
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class VideoRepositoryImpl(private val provider: DatabaseProvider) : VideoRepository {

    private suspend fun db(): TayraDatabase = provider.database()

    override fun observeVideos(): Flow<List<VideoListItem>> = flow {
        db().videosQueries.listVideos().asFlow().mapToList(databaseDispatcher).collect { rows ->
            emit(
                rows.map { row ->
                    VideoListItem(
                        video = Video(
                            bookId = row.book_id,
                            languageId = row.language_id,
                            title = row.title,
                            kind = kindOf(row.kind),
                            media = row.media,
                            channel = row.channel,
                            thumbnail = row.thumbnail,
                            durationMs = row.duration_ms,
                            positionMs = row.position_ms,
                            subtitleKind = subtitleKindOf(row.subtitle_kind),
                            createdAt = row.created_at.toInstant(),
                        ),
                        languageName = row.language_name,
                        lastOpened = row.last_opened?.toInstant(),
                        wordCount = row.word_count.toInt(),
                        distinctTerms = row.distinct_terms?.toInt(),
                        distinctUnknowns = row.distinct_unknowns?.toInt(),
                        unknownPercent = row.unknown_percent?.toInt(),
                    )
                },
            )
        }
    }

    override suspend fun video(bookId: Long): Video? = withContext(databaseDispatcher) {
        db().videosQueries.selectVideo(bookId).awaitAsOneOrNull()?.let { row ->
            Video(
                bookId = row.book_id,
                languageId = row.language_id,
                title = row.title,
                kind = kindOf(row.kind),
                media = row.media,
                channel = row.channel,
                thumbnail = row.thumbnail,
                durationMs = row.duration_ms,
                positionMs = row.position_ms,
                subtitleKind = subtitleKindOf(row.subtitle_kind),
                createdAt = row.created_at.toInstant(),
            )
        }
    }

    override suspend fun isVideo(bookId: Long): Boolean = withContext(databaseDispatcher) {
        db().videosQueries.isVideo(bookId).awaitAsOne()
    }

    override suspend fun findByMedia(kind: VideoKind, media: String, languageId: Long): Long? = withContext(databaseDispatcher) {
        db().videosQueries.videoOfMedia(kind.name, media, languageId).awaitAsOneOrNull()
    }

    override suspend fun cues(bookId: Long): List<StoredCue> = withContext(databaseDispatcher) {
        db().videosQueries.cues(bookId).awaitAsList().map { it.toStored() }
    }

    override suspend fun insert(video: NewVideo, transcript: VideoTranscript): Long = withContext(databaseDispatcher) {
        val database = db()
        val books = database.booksQueries
        database.transactionWithResult {
            books.insert(
                languageId = video.languageId,
                title = video.title,
                sourceUri = video.sourceUri(),
                currentPageId = null,
                archived = false,
                audioFilename = null,
                audioCurrentPos = null,
                audioBookmarks = null,
            )
            val id = books.lastInsertId().awaitAsOne()
            transcript.pages.forEachIndexed { index, page ->
                books.insertPage(bookId = id, pageOrder = (index + 1).toLong(), text = page.text, wordCount = page.wordCount.toLong())
            }
            database.videosQueries.insertVideo(
                bookId = id,
                kind = video.kind.name,
                media = video.media,
                channel = video.channel,
                thumbnail = video.thumbnail,
                durationMs = video.durationMs,
                subtitleKind = video.subtitleKind.name,
                createdAt = video.createdAt.toEpochMillis(),
            )
            insertCues(database, id, transcript)
            id
        }
    }

    override suspend fun replaceTranscript(bookId: Long, transcript: VideoTranscript, subtitleKind: SubtitleKind) = withContext(databaseDispatcher) {
        val database = db()
        val books = database.booksQueries
        database.transaction {
            books.deleteSentencesOfBook(bookId)
            books.deleteBookmarksOfBook(bookId)
            database.wordsReadQueries.detachPagesOfBook(bookId)
            books.deletePagesOfBook(bookId)
            transcript.pages.forEachIndexed { index, page ->
                books.insertPage(bookId = bookId, pageOrder = (index + 1).toLong(), text = page.text, wordCount = page.wordCount.toLong())
            }
            books.setCurrentPage(pageId = null, id = bookId)
            books.deleteStats(bookId)
            database.videosQueries.deleteCues(bookId)
            insertCues(database, bookId, transcript)
            database.videosQueries.setSubtitleKind(subtitleKind.name, bookId)
            database.videosQueries.touch(bookId)
        }
    }

    override suspend fun setTranslations(bookId: Long, translations: Map<Int, String?>, source: TranslationSource?) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            for ((order, translation) in translations) {
                database.videosQueries.setCueTranslation(
                    translation = translation,
                    translationSource = source?.name?.takeIf { translation != null },
                    bookId = bookId,
                    cueOrder = order.toLong(),
                )
            }
            database.videosQueries.touch(bookId)
        }
    }

    override suspend fun shiftCues(bookId: Long, deltaMs: Long) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            database.videosQueries.shiftCues(deltaMs, bookId)
            database.videosQueries.touch(bookId)
        }
    }

    override suspend fun setPosition(bookId: Long, positionMs: Long, final: Boolean) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            database.videosQueries.setPosition(positionMs.coerceAtLeast(0), bookId)
            if (final) database.videosQueries.touch(bookId)
        }
    }

    override suspend fun setDuration(bookId: Long, durationMs: Long) {
        withContext(databaseDispatcher) { db().videosQueries.setDuration(durationMs, bookId) }
    }

    override suspend fun setMedia(bookId: Long, media: String) {
        withContext(databaseDispatcher) { db().videosQueries.setMedia(media, bookId) }
    }

    override suspend fun keepTermSource(termId: Long, bookId: Long, beginMs: Long, endMs: Long) {
        withContext(databaseDispatcher) { db().videosQueries.keepTermSource(termId, bookId, beginMs, endMs) }
    }

    override suspend fun termSource(termId: Long): TermVideoSource? = withContext(databaseDispatcher) {
        db().videosQueries.termSource(termId).awaitAsOneOrNull()?.let {
            TermVideoSource(it.book_id, it.title, kindOf(it.kind), it.media, it.begin_ms, it.end_ms)
        }
    }

    private suspend fun insertCues(database: TayraDatabase, bookId: Long, transcript: VideoTranscript) {
        for (stored in transcript.cues) {
            database.videosQueries.insertCue(
                bookId = bookId,
                cueOrder = stored.order.toLong(),
                pageOrder = stored.pageOrder.toLong(),
                lineIndex = stored.lineIndex.toLong(),
                beginMs = stored.cue.beginMs,
                endMs = stored.cue.endMs,
                text = stored.cue.text,
                translation = stored.cue.translation,
                translationSource = stored.translationSource?.name?.takeIf { stored.cue.translation != null },
            )
        }
    }

    private fun Video_cues.toStored() = StoredCue(
        order = cue_order.toInt(),
        pageOrder = page_order.toInt(),
        lineIndex = line_index.toInt(),
        cue = Cue(begin_ms, end_ms, text, translation),
        translationSource = translation_source?.let { name -> TranslationSource.entries.firstOrNull { it.name == name } },
    )

    companion object {
        fun kindOf(name: String): VideoKind = VideoKind.entries.firstOrNull { it.name == name } ?: VideoKind.FILE
        fun subtitleKindOf(name: String): SubtitleKind = SubtitleKind.entries.firstOrNull { it.name == name } ?: SubtitleKind.FILE

        /** YouTube videos are known by their id; a file has no address that means anything on another device. */
        private fun NewVideo.sourceUri(): String? = if (kind == VideoKind.YOUTUBE) "youtube:$media" else null
    }
}
