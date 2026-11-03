package com.tayra.languages.core.domain.export

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
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
        val modelId = collection.noteType.id
        val deckNames = collection.notes.map { it.deck }.distinct()
        val deckIds = deckNames.associateWith { stableAnkiId(it) }
        val presetId = collection.preset?.id ?: 1
        val created = collection.days?.dayZero?.epochSeconds ?: seconds
        val statements = SCHEMA.toMutableList()
        statements += "INSERT INTO col VALUES (1, $created, $millis, $millis, $SCHEMA_VERSION, 0, 0, 0, ${sql(conf(modelId, collection.days))}, ${sql(models(collection.noteType, modelId, deckIds.values.firstOrNull() ?: 1, seconds))}, ${sql(decks(deckIds, presetId, seconds))}, ${sql(deckConf(collection.preset, seconds))}, '{}')"
        // Note and card ids are the creation time in milliseconds, as Anki makes them; each gets its own.
        var id = millis
        val reviewIds = mutableSetOf<Long>()
        collection.notes.forEachIndexed { index, note ->
            val noteId = id++
            val cardId = id++
            val fields = note.fields.joinToString(FIELD_SEPARATOR)
            val sortField = note.fields.first().replace(Regex("\\{\\{c\\d+::|\\}\\}"), "")
            val checksum = Sha1.hex(sortField.encodeToByteArray()).take(8).toLong(16)
            val tags = note.tags.joinToString(" ", prefix = " ", postfix = " ")
            statements += "INSERT INTO notes VALUES ($noteId, ${sql(note.guid)}, $modelId, $seconds, -1, ${sql(tags)}, ${sql(fields)}, ${sql(sortField)}, $checksum, 0, '')"
            val deckId = deckIds.getValue(note.deck)
            val card = note.card
            if (card == null) {
                statements += "INSERT INTO cards VALUES ($cardId, $noteId, $deckId, 0, $seconds, -1, 0, 0, ${index + 1}, 0, 0, 0, 0, 0, 0, 0, 0, '')"
                return@forEachIndexed
            }
            statements += "INSERT INTO cards VALUES ($cardId, $noteId, $deckId, 0, $seconds, -1, ${card.type}, ${card.queue}, ${card.due}, ${card.intervalDays}, ${card.factor}, ${card.reps}, ${card.lapses}, ${card.left}, 0, 0, 0, ${sql(card.data)})"
            for (review in card.reviews) {
                // A review's id is when it was given; two at the same millisecond still need their own.
                var reviewId = review.at.toEpochMilliseconds()
                while (!reviewIds.add(reviewId)) reviewId++
                statements += "INSERT INTO revlog VALUES ($reviewId, $cardId, -1, ${review.ease}, ${review.interval}, ${review.lastInterval}, ${review.factor}, ${review.millis}, ${review.type})"
            }
        }
        return statements
    }

    /** The package's `media` file: zip entry names ("0", "1", …) to file names. */
    fun mediaIndex(media: List<AnkiMedia>): String =
        buildJsonObject { media.forEachIndexed { i, file -> put(i.toString(), file.name) } }.toString()

    /** An id drawn from [name], in the range of Anki's millisecond ids. */
    private fun sql(text: String): String = "'" + text.replace("\u0000", "").replace("'", "''") + "'"

    /** The collection's options; with [days], a v2 collection whose days start as they say. */
    private fun conf(modelId: Long, days: AnkiDays?): String = buildJsonObject {
        if (days != null) {
            put("schedVer", 2)
            put("rollover", days.rolloverHour)
            // Anki counts the offset in minutes west of UTC.
            put("creationOffset", -days.utcOffsetMinutes)
        }
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

    private fun decks(deckIds: Map<String, Long>, presetId: Long, seconds: Long): String = buildJsonObject {
        put("1", deck(1, "Default", 1, seconds))
        for ((name, id) in deckIds) put(id.toString(), deck(id, name, presetId, seconds))
    }.toString()

    private fun deckConf(preset: AnkiPreset?, seconds: Long): String {
        if (preset == null) return DECK_CONF
        val default = Json.parseToJsonElement(DECK_CONF).jsonObject
        return JsonObject(default + (preset.id.toString() to preset(preset, seconds))).toString()
    }

    /** The preset as schema 11 keeps it, with the FSRS and 2021 scheduler options Anki adds to it. */
    private fun preset(preset: AnkiPreset, seconds: Long): JsonObject = buildJsonObject {
        put("id", preset.id)
        put("name", preset.name)
        put("mod", seconds)
        put("usn", -1)
        put("maxTaken", 60)
        put("autoplay", preset.autoplay)
        put("timer", if (preset.timer) 1 else 0)
        put("replayq", true)
        put("dyn", false)
        putJsonObject("new") {
            put("bury", false)
            put("delays", numbers(preset.learnSteps))
            put("initialFactor", 2500)
            put("ints", numbers(listOf(1.0, 4.0, 0.0)))
            put("order", if (preset.newSort == NEW_SORT_RANDOM) 0 else 1)
            put("perDay", preset.newPerDay)
        }
        putJsonObject("rev") {
            put("bury", false)
            put("ease4", 1.3)
            put("hardFactor", 1.2)
            put("ivlFct", 1.0)
            put("maxIvl", preset.maximumInterval)
            // Anki has no "no limit"; this many is as good as one.
            put("perDay", if (preset.reviewsPerDay > 0) preset.reviewsPerDay else UNLIMITED)
        }
        putJsonObject("lapse") {
            put("delays", numbers(preset.relearnSteps))
            put("leechAction", if (preset.leechSuspend) 0 else 1)
            put("leechFails", preset.leechThreshold)
            put("minInt", 1)
            put("mult", 0.0)
        }
        put("newMix", preset.newMix)
        put("newGatherPriority", preset.newGather)
        put("newSortOrder", preset.newSort)
        put("desiredRetention", preset.desiredRetention)
        put(if (preset.parameters.size > FSRS5_PARAMETERS) "fsrsParams6" else "fsrsParams5", numbers(preset.parameters))
        put("easyDaysPercentages", numbers(preset.easyDays))
        put("secondsToShowQuestion", preset.secondsToShowQuestion.toDouble())
        put("secondsToShowAnswer", preset.secondsToShowAnswer.toDouble())
        put("answerAction", preset.answerAction)
        put("questionAction", 0)
        put("waitForAudio", true)
        put("stopTimerOnAnswer", false)
    }

    private fun numbers(values: List<Double>): JsonArray = JsonArray(values.map { kotlinx.serialization.json.JsonPrimitive(it) })

    private fun deck(id: Long, name: String, presetId: Long, seconds: Long): JsonObject = buildJsonObject {
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
        put("conf", presetId)
    }

    private const val FIELD_SEPARATOR = "\u001f"

    /** Anki's random order of new cards. */
    const val NEW_SORT_RANDOM = 4

    private const val UNLIMITED = 9999
    private const val FSRS5_PARAMETERS = 19

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
