package com.tayra.languages.core.data.export

import com.tayra.languages.core.domain.export.AnkiCollection
import com.tayra.languages.core.domain.export.AnkiCollectionSql
import com.tayra.languages.core.domain.export.AnkiPackager
import com.tayra.languages.core.domain.export.ZipWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Zips the collection database, written by the platform's SQLite, with the media. */
class AnkiPackagerImpl : AnkiPackager {
    override suspend fun pack(collection: AnkiCollection, now: Instant): ByteArray = withContext(Dispatchers.Default) {
        val database = ankiSqliteBytes(AnkiCollectionSql.statements(collection, now))
        val entries = listOf("collection.anki2" to database, "media" to AnkiCollectionSql.mediaIndex(collection.media).encodeToByteArray()) +
            collection.media.mapIndexed { i, file -> i.toString() to file.bytes }
        ZipWriter.write(entries, now.toLocalDateTime(TimeZone.currentSystemDefault()))
    }
}

/** A new SQLite database with [statements] run on it, as a file's bytes. */
internal expect suspend fun ankiSqliteBytes(statements: List<String>): ByteArray
