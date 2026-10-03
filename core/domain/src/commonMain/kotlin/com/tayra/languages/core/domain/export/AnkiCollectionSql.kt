package com.tayra.languages.core.domain.export

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.time.Instant

/**
 * The SQL that fills an Anki collection database (schema version 11, which every Anki reads)
 * with an [AnkiCollection], and the `media` index of the package.
 */
object AnkiCollectionSql {
    const val SCHEMA_VERSION = 11

    /** Statements to run, in order, on an empty SQLite database. */
    fun statements(collection: AnkiCollection, now: Instant): List<String> {
        val millis = now.toEpochMilliseconds()
        val seconds = millis / 1000
        // The note type and decks keep their ids from one export to the next, so Anki updates
        // them on import instead of adding copies; notes are found again by their guid.
        val modelId = stableId(collection.noteType.name)
        val deckNames = collection.notes.map { it.deck }.distinct()
        val deckIds = deckNames.associateWith { stableId(it) }
        val statements = SCHEMA.toMutableList()
        statements += "INSERT INTO col VALUES (1, $seconds, $millis, $millis, $SCHEMA_VERSION, 0, 0, 0, ${sql(conf(modelId))}, ${sql(models(collection.noteType, modelId, deckIds.values.firstOrNull() ?: 1, seconds))}, ${sql(decks(deckIds, seconds))}, ${sql(DECK_CONF)}, '{}')"
        // Note and card ids are the creation time in milliseconds, as Anki makes them; each gets its own.
        var id = millis
        collection.notes.forEachIndexed { index, note ->
            val noteId = id++
            val cardId = id++
            val fields = note.fields.joinToString(FIELD_SEPARATOR)
            val sortField = note.fields.first().replace(Regex("\\{\\{c\\d+::|\\}\\}"), "")
            val checksum = Sha1.hex(sortField.encodeToByteArray()).take(8).toLong(16)
            val tags = note.tags.joinToString(" ", prefix = " ", postfix = " ")
            statements += "INSERT INTO notes VALUES ($noteId, ${sql(note.guid)}, $modelId, $seconds, -1, ${sql(tags)}, ${sql(fields)}, ${sql(sortField)}, $checksum, 0, '')"
            statements += "INSERT INTO cards VALUES ($cardId, $noteId, ${deckIds.getValue(note.deck)}, 0, $seconds, -1, 0, 0, ${index + 1}, 0, 0, 0, 0, 0, 0, 0, 0, '')"
        }
        return statements
    }

    /** The package's `media` file: zip entry names ("0", "1", …) to file names. */
    fun mediaIndex(media: List<AnkiMedia>): String =
        buildJsonObject { media.forEachIndexed { i, file -> put(i.toString(), file.name) } }.toString()

    /** An id drawn from [name], in the range of Anki's millisecond ids. */
    private fun stableId(name: String): Long = 1_600_000_000_000L + Sha1.hex(name.encodeToByteArray()).take(8).toLong(16) % 100_000_000_000L

    private fun sql(text: String): String = "'" + text.replace("\u0000", "").replace("'", "''") + "'"

    private fun conf(modelId: Long): String = buildJsonObject {
        putJsonArray("activeDecks") { add(kotlinx.serialization.json.JsonPrimitive(1)) }
        put("addToCur", true)
        put("collapseTime", 1200)
        put("curDeck", 1)
        put("curModel", modelId.toString())
        put("dueCounts", true)
        put("estTimes", true)
        put("newBury", true)
        put("newSpread", 0)
        put("nextPos", 1)
        put("sortBackwards", false)
        put("sortType", "noteFld")
        put("timeLim", 0)
    }.toString()

    private fun models(type: AnkiNoteType, modelId: Long, deckId: Long, seconds: Long): String = buildJsonObject {
        putJsonObject(modelId.toString()) {
            put("id", modelId)
            put("name", type.name)
            put("type", 1)
            put("mod", seconds)
            put("usn", -1)
            put("sortf", 0)
            put("did", deckId)
            putJsonArray("tmpls") {
                add(
                    buildJsonObject {
                        put("name", "Cloze")
                        put("ord", 0)
                        put("qfmt", type.front)
                        put("afmt", type.back)
                        put("bqfmt", "")
                        put("bafmt", "")
                        put("did", JsonNull)
                        put("bfont", "")
                        put("bsize", 0)
                    },
                )
            }
            putJsonArray("flds") {
                type.fields.forEachIndexed { i, name ->
                    add(
                        buildJsonObject {
                            put("name", name)
                            put("ord", i)
                            put("sticky", false)
                            put("rtl", false)
                            put("font", "Arial")
                            put("size", 20)
                            put("media", JsonArray(emptyList()))
                        },
                    )
                }
            }
            put("css", type.css)
            put("latexPre", LATEX_PRE)
            put("latexPost", "\\end{document}")
            put("latexsvg", false)
            put("req", buildJsonArray { add(buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(0)); add(kotlinx.serialization.json.JsonPrimitive("all")); add(buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(0)) }) }) })
            put("tags", JsonArray(emptyList()))
            put("vers", JsonArray(emptyList()))
        }
    }.toString()

    private fun decks(deckIds: Map<String, Long>, seconds: Long): String = buildJsonObject {
        put("1", deck(1, "Default", seconds))
        for ((name, id) in deckIds) put(id.toString(), deck(id, name, seconds))
    }.toString()

    private fun deck(id: Long, name: String, seconds: Long): JsonObject = buildJsonObject {
        put("id", id)
        put("name", name)
        put("desc", "")
        put("mod", seconds)
        put("usn", -1)
        put("collapsed", false)
        put("browserCollapsed", false)
        putJsonArray("newToday") { add(kotlinx.serialization.json.JsonPrimitive(0)); add(kotlinx.serialization.json.JsonPrimitive(0)) }
        putJsonArray("revToday") { add(kotlinx.serialization.json.JsonPrimitive(0)); add(kotlinx.serialization.json.JsonPrimitive(0)) }
        putJsonArray("lrnToday") { add(kotlinx.serialization.json.JsonPrimitive(0)); add(kotlinx.serialization.json.JsonPrimitive(0)) }
        putJsonArray("timeToday") { add(kotlinx.serialization.json.JsonPrimitive(0)); add(kotlinx.serialization.json.JsonPrimitive(0)) }
        put("dyn", 0)
        put("extendNew", 0)
        put("extendRev", 0)
        put("conf", 1)
    }

    private const val FIELD_SEPARATOR = "\u001f"

    private val SCHEMA = listOf(
        "CREATE TABLE col (id integer primary key, crt integer not null, mod integer not null, scm integer not null, ver integer not null, dty integer not null, usn integer not null, ls integer not null, conf text not null, models text not null, decks text not null, dconf text not null, tags text not null)",
        "CREATE TABLE notes (id integer primary key, guid text not null, mid integer not null, mod integer not null, usn integer not null, tags text not null, flds text not null, sfld integer not null, csum integer not null, flags integer not null, data text not null)",
        "CREATE TABLE cards (id integer primary key, nid integer not null, did integer not null, ord integer not null, mod integer not null, usn integer not null, type integer not null, queue integer not null, due integer not null, ivl integer not null, factor integer not null, reps integer not null, lapses integer not null, left integer not null, odue integer not null, odid integer not null, flags integer not null, data text not null)",
        "CREATE TABLE revlog (id integer primary key, cid integer not null, usn integer not null, ease integer not null, ivl integer not null, lastIvl integer not null, factor integer not null, time integer not null, type integer not null)",
        "CREATE TABLE graves (usn integer not null, oid integer not null, type integer not null)",
        "CREATE INDEX ix_notes_usn ON notes (usn)",
        "CREATE INDEX ix_cards_usn ON cards (usn)",
        "CREATE INDEX ix_revlog_usn ON revlog (usn)",
        "CREATE INDEX ix_cards_nid ON cards (nid)",
        "CREATE INDEX ix_cards_sched ON cards (did, queue, due)",
        "CREATE INDEX ix_revlog_cid ON revlog (cid)",
        "CREATE INDEX ix_notes_csum ON notes (csum)",
    )

    private const val DECK_CONF = """{"1":{"id":1,"name":"Default","new":{"delays":[1,10],"ints":[1,4,7],"initialFactor":2500,"separate":true,"order":1,"perDay":20,"bury":true},"lapse":{"delays":[10],"mult":0,"minInt":1,"leechFails":8,"leechAction":0},"rev":{"perDay":200,"ease4":1.3,"fuzz":0.05,"minSpace":1,"ivlFct":1,"maxIvl":36500,"bury":true,"hardFactor":1.2},"maxTaken":60,"timer":0,"autoplay":true,"replayq":true,"mod":0,"usn":0}}"""

    private const val LATEX_PRE = "\\documentclass[12pt]{article}\n\\special{papersize=3in,5in}\n\\usepackage[utf8]{inputenc}\n\\usepackage{amssymb,amsmath}\n\\pagestyle{empty}\n\\setlength{\\parindent}{0in}\n\\begin{document}\n"
}
