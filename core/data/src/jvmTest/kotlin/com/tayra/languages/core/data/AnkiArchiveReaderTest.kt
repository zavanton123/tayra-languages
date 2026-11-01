package com.tayra.languages.core.data

import com.tayra.languages.core.data.anki.AnkiArchiveReaderImpl
import com.tayra.languages.core.data.export.AnkiPackagerImpl
import com.tayra.languages.core.domain.export.AnkiCollection
import com.tayra.languages.core.domain.export.AnkiExportService
import com.tayra.languages.core.domain.export.AnkiNote
import com.tayra.languages.core.domain.flashcards.AnkiImportException
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.sql.DriverManager
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

/** Anki packages are read whichever way Anki wrote them: plain as older versions and Tayra's export write them, or compressed as newer Anki does. */
class AnkiArchiveReaderTest {

    private val reader = AnkiArchiveReaderImpl()

    @Test
    fun tayrasOwnExportIsReadBack() = runBlocking<Unit> {
        val noteType = AnkiExportService.noteType()
        val fields = noteType.fields.map { name -> if (name == "Word") "lobo" else if (name == "Language") "pt" else "" }
        val bytes = AnkiPackagerImpl().pack(AnkiCollection(noteType, listOf(AnkiNote("abc", "Tayra Languages::Portuguese", fields, listOf("Tayra_Languages::lang::pt"))), emptyList()), Instant.parse("2026-11-01T12:00:00Z"))

        val archive = reader.read(bytes)
        val note = archive.notes.single()
        assertEquals("lobo", note.fields["Word"])
        assertEquals("pt", note.fields["Language"])
        assertEquals(listOf("Tayra_Languages::lang::pt"), note.tags)
        val card = note.cards.single()
        assertEquals(0, card.type)
        assertEquals("Tayra Languages::Portuguese", card.deck)
    }

    @Test
    fun aNewerAnkisCompressedPackageIsRead() = runBlocking<Unit> {
        val zstd = listOf("/opt/homebrew/bin/zstd", "/usr/local/bin/zstd", "/usr/bin/zstd").map(::File).firstOrNull { it.canExecute() } ?: return@runBlocking
        val database = File.createTempFile("anki-collection", ".db").also { it.delete() }
        DriverManager.getConnection("jdbc:sqlite:${database.absolutePath}").use { c ->
            listOf(
                "CREATE TABLE col (id integer primary key, crt integer not null)",
                "INSERT INTO col VALUES (1, 1771027200)",
                // As Anki makes them, with its own collation put in below, which SQLite here does not have.
                "CREATE TABLE notetypes (id integer primary key, name text not null COLLATE nocase )",
                "INSERT INTO notetypes VALUES (7, 'Cloze')",
                "CREATE TABLE fields (ntid integer not null, ord integer not null, name text not null COLLATE nocase , PRIMARY KEY (ntid, ord)) without rowid",
                "CREATE UNIQUE INDEX idx_fields_name_ntid ON fields (name, ntid)",
                "INSERT INTO fields VALUES (7, 1, 'Back Extra'), (7, 0, 'Text')",
                "CREATE TABLE decks (id integer primary key, name text not null COLLATE nocase )",
                "INSERT INTO decks VALUES (3, 'Tayra Languages' || char(31) || 'Portuguese'), (4, 'Filtered')",
                "CREATE TABLE notes (id integer primary key, guid text, mid integer, mod integer, usn integer, tags text, flds text, sfld text, csum integer, flags integer, data text)",
                "INSERT INTO notes VALUES (1790000000000, 'g', 7, 0, 0, ' a::b c ', 'É à {{c1::direita}}.' || char(31) || 'extra', '', 0, 0, '')",
                "CREATE TABLE cards (id integer primary key, nid integer, did integer, ord integer, mod integer, usn integer, type integer, queue integer, due integer, ivl integer, factor integer, reps integer, lapses integer, left integer, odue integer, odid integer, flags integer, data text)",
                """INSERT INTO cards VALUES (55, 1790000000000, 4, 0, 0, 0, 2, 2, 999, 12, 2500, 9, 2, 0, 262, 3, 0, '{"s":21.5,"d":6.25,"dr":0.9,"lrt":1793456993}')""",
                "CREATE TABLE revlog (id integer primary key, cid integer, usn integer, ease integer, ivl integer, lastIvl integer, factor integer, time integer, type integer)",
                "INSERT INTO revlog VALUES (1793456993000, 55, 0, 3, 12, 5, 0, 4000, 1), (1793000000000, 99, 0, 1, -600, 0, 0, 1000, 0)",
            ).forEach { c.createStatement().execute(it) }
        }
        database.writeBytes(String(database.readBytes(), Charsets.ISO_8859_1).replace("COLLATE nocase ", "COLLATE unicase").toByteArray(Charsets.ISO_8859_1))
        val compressed = File.createTempFile("anki-collection", ".zst").also { it.delete() }
        assertEquals(0, ProcessBuilder(zstd.path, "-q", "-o", compressed.path, database.path).start().waitFor())
        val bytes = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                listOf("collection.anki2" to "placeholder".encodeToByteArray(), "collection.anki21b" to compressed.readBytes(), "meta" to byteArrayOf(8, 3)).forEach { (name, data) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(data)
                    zip.closeEntry()
                }
            }
        }.toByteArray()

        val archive = reader.read(bytes)
        assertEquals(Instant.fromEpochSeconds(1771027200), archive.dayZero)
        val note = archive.notes.single()
        assertEquals(mapOf("Text" to "É à {{c1::direita}}.", "Back Extra" to "extra"), note.fields)
        assertEquals(listOf("a::b", "c"), note.tags)
        val card = note.cards.single()
        assertEquals("Tayra Languages::Portuguese", card.deck, "the deck it came from, not the filtered one it is in")
        assertEquals(262L, card.originalDue)
        assertEquals(listOf(2, 2, 12, 9, 2), listOf(card.type, card.queue, card.intervalDays, card.reps, card.lapses))
        assertEquals(21.5, card.stability)
        assertEquals(6.25, card.difficulty)
        assertEquals(Instant.fromEpochSeconds(1793456993), card.lastReview)
        assertEquals(listOf(3), card.reviews.map { it.ease }, "only its own answers")
    }

    @Test
    fun otherFilesAreRefused() = runBlocking<Unit> {
        assertFailsWith<AnkiImportException> { reader.read("not a zip at all".encodeToByteArray()) }
        val zip = ByteArrayOutputStream().also { out -> ZipOutputStream(out).use { it.putNextEntry(ZipEntry("book.txt")); it.write(1); it.closeEntry() } }.toByteArray()
        assertFailsWith<AnkiImportException> { reader.read(zip) }
    }
}
