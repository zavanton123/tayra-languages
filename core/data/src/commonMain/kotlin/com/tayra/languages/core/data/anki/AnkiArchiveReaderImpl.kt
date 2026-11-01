package com.tayra.languages.core.data.anki

import com.tayra.languages.core.data.db.SqliteQuery
import com.tayra.languages.core.data.db.readSqliteFile
import com.tayra.languages.core.data.yomitan.ZipArchive
import com.tayra.languages.core.domain.flashcards.AnkiArchive
import com.tayra.languages.core.domain.flashcards.AnkiArchiveCard
import com.tayra.languages.core.domain.flashcards.AnkiArchiveNote
import com.tayra.languages.core.domain.flashcards.AnkiArchiveReader
import com.tayra.languages.core.domain.flashcards.AnkiArchiveReview
import com.tayra.languages.core.domain.flashcards.AnkiImportException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

/** Whether Anki packages are read on this platform; tried on the desktop and Android. */
internal expect val ankiImportAvailable: Boolean

/**
 * Reads an Anki package: a zip holding the collection, compressed with zstd by Anki 2.1.50 and
 * later (collection.anki21b), or plain from older versions and from "Support older Anki versions"
 * (collection.anki21, collection.anki2). Newer collections keep note types and decks in tables of
 * their own, older ones as JSON in the col table; the media is not read.
 */
class AnkiArchiveReaderImpl : AnkiArchiveReader {
    override val available: Boolean get() = ankiImportAvailable

    override suspend fun read(bytes: ByteArray): AnkiArchive = try {
        readCollection(withoutUnicase(collectionOf(bytes)))
    } catch (e: CancellationException) {
        throw e
    } catch (e: AnkiImportException) {
        throw e
    } catch (e: Exception) {
        throw AnkiImportException(NOT_A_PACKAGE, e)
    }

    private suspend fun collectionOf(bytes: ByteArray): ByteArray {
        val zip = ZipArchive(bytes)
        zip.entry("collection.anki21b")?.let { return Zstd.decompress(zip.read(it)) }
        val plain = zip.entry("collection.anki21") ?: zip.entry("collection.anki2") ?: throw AnkiImportException(NOT_A_PACKAGE)
        return zip.read(plain)
    }

    private suspend fun readCollection(file: ByteArray): AnkiArchive {
        val tables = readSqliteFile(file, listOf(SqliteQuery("SELECT name FROM sqlite_master WHERE type = 'table'", 1))).single().map { it[0] }
        if ("notes" !in tables || "cards" !in tables) throw AnkiImportException(NOT_A_PACKAGE)
        val separate = "notetypes" in tables && "fields" in tables && "decks" in tables
        val shape = if (separate) {
            listOf(SqliteQuery("SELECT ntid, ord, name FROM fields ORDER BY ntid, ord", 3), SqliteQuery("SELECT id, name FROM decks", 2))
        } else {
            listOf(SqliteQuery("SELECT models, decks FROM col", 2))
        }
        val results = readSqliteFile(
            file,
            listOf(
                SqliteQuery("SELECT crt FROM col", 1),
                SqliteQuery("SELECT id, mid, flds, tags FROM notes", 4),
                SqliteQuery("SELECT nid, did, odid, type, queue, due, odue, ivl, reps, lapses, left, data, id FROM cards", 13),
                SqliteQuery("SELECT cid, id, ease, ivl, type FROM revlog", 5),
            ) + shape,
        )
        val dayZero = Instant.fromEpochSeconds(results[0].single()[0]!!.toLong())
        val fieldNames: Map<Long, List<String>>
        val deckNames: Map<Long, String>
        if (separate) {
            fieldNames = results[4].groupBy({ it[0]!!.toLong() }, { it[2]!! })
            deckNames = results[5].associate { it[0]!!.toLong() to it[1]!!.replace(DECK_SEPARATOR, "::") }
        } else {
            val (models, decks) = results[4].single().map { Json.parseToJsonElement(it!!).jsonObject }
            fieldNames = models.entries.associate { (id, model) ->
                id.toLong() to model.jsonObject.getValue("flds").jsonArray.map { it.jsonObject }.sortedBy { it.getValue("ord").jsonPrimitive.int }.map { it.getValue("name").jsonPrimitive.content }
            }
            deckNames = decks.entries.associate { (id, deck) -> id.toLong() to deck.jsonObject.getValue("name").jsonPrimitive.content }
        }
        val reviews = results[3].groupBy({ it[0]!!.toLong() }) { row ->
            AnkiArchiveReview(Instant.fromEpochMilliseconds(row[1]!!.toLong()), row[2]!!.toInt(), row[3]!!.toInt(), row[4]!!.toInt())
        }
        val cards = results[2].groupBy({ it[0]!!.toLong() }) { row ->
            val memory = row[11]?.takeIf { it.startsWith("{") }?.let { Json.parseToJsonElement(it).jsonObject }
            val filtered = row[2]!!.toLong() != 0L
            AnkiArchiveCard(
                deck = deckNames[if (filtered) row[2]!!.toLong() else row[1]!!.toLong()].orEmpty(),
                type = row[3]!!.toInt(),
                queue = row[4]!!.toInt(),
                due = row[5]!!.toLong(),
                originalDue = row[6]!!.toLong().takeIf { filtered && it != 0L },
                intervalDays = row[7]!!.toInt(),
                reps = row[8]!!.toInt(),
                lapses = row[9]!!.toInt(),
                left = row[10]!!.toInt(),
                stability = memory?.number("s"),
                difficulty = memory?.number("d"),
                lastReview = memory?.get("lrt")?.jsonPrimitive?.longOrNull?.let { Instant.fromEpochSeconds(it) },
                reviews = reviews[row[12]!!.toLong()].orEmpty(),
            )
        }
        val notes = results[1].map { row ->
            val id = row[0]!!.toLong()
            val names = fieldNames[row[1]!!.toLong()].orEmpty()
            val values = row[2]!!.split(FIELD_SEPARATOR)
            AnkiArchiveNote(
                id = id,
                fields = names.zip(values).toMap(),
                tags = row[3]!!.split(' ').filter { it.isNotBlank() },
                cards = cards[id].orEmpty(),
            )
        }
        return AnkiArchive(dayZero, notes)
    }

    /**
     * Anki sorts the names in its newer tables with a collation of its own, "unicase", which SQLite
     * here lacks and will not read those tables without; SQLite's own "nocase", as long, takes its
     * place in the schema's text. Only names are compared with it, and only for Anki's indexes.
     */
    private fun withoutUnicase(file: ByteArray): ByteArray {
        val from = "COLLATE unicase".encodeToByteArray()
        val to = "COLLATE nocase ".encodeToByteArray()
        var at = 0
        while (at <= file.size - from.size) {
            if (file[at] == from[0] && (1 until from.size).all { file[at + it] == from[it] }) {
                to.copyInto(file, at)
                at += from.size
            } else at++
        }
        return file
    }

    private fun JsonObject.number(key: String): Double? = get(key)?.jsonPrimitive?.let { it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull() }

    private companion object {
        const val NOT_A_PACKAGE = "This file is not an Anki package"
        const val FIELD_SEPARATOR = '\u001f'
        const val DECK_SEPARATOR = "\u001f"
    }
}
