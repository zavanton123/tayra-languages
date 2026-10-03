package com.tayra.languages.core.data.export

import com.tayra.languages.core.domain.export.AnkiCollection
import com.tayra.languages.core.domain.export.AnkiCollectionSql
import com.tayra.languages.core.domain.export.AnkiPackager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.time.Instant

actual class AnkiPackagerImpl : AnkiPackager {
    override suspend fun pack(collection: AnkiCollection, now: Instant): ByteArray = withContext(Dispatchers.IO) {
        val database = File.createTempFile("tayra-anki", ".anki2")
        try {
            database.delete()
            writeAnkiSqlite(database.absolutePath, AnkiCollectionSql.statements(collection, now))
            val out = ByteArrayOutputStream()
            ZipOutputStream(out).use { zip ->
                fun entry(name: String, bytes: ByteArray) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
                entry("collection.anki2", database.readBytes())
                entry("media", AnkiCollectionSql.mediaIndex(collection.media).encodeToByteArray())
                collection.media.forEachIndexed { i, file -> entry(i.toString(), file.bytes) }
            }
            out.toByteArray()
        } finally {
            database.delete()
        }
    }
}

/** Runs [statements] on a new SQLite database at [path]. */
internal expect fun writeAnkiSqlite(path: String, statements: List<String>)
