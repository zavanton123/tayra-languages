package com.tayra.languages.sync

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncMergeTest {
    private fun record(kind: String, at: Long, device: String = "a", deleted: Boolean = false, data: JsonObject? = JsonObject(emptyMap())) =
        SyncRecord(kind, "pt/casa", at, deleted, if (deleted) null else data, device)

    private fun term(at: Long, status: Int, statusAt: Long, translation: String, device: String = "a") = record(SyncKinds.TERM, at, device, data = buildJsonObject {
        put(TermFields.STATUS, status)
        put(TermFields.STATUS_CHANGED_AT, statusAt)
        put("translation", translation)
    })

    @Test
    fun theLaterChangeWinsAndTiesGoTheSameWayEverywhere() {
        val early = record(SyncKinds.LANGUAGE, 100, "a")
        val late = record(SyncKinds.LANGUAGE, 200, "a")
        assertEquals(late, SyncMerge.merge(early, late))
        assertEquals(late, SyncMerge.merge(late, early))
        val fromA = record(SyncKinds.LANGUAGE, 100, "a")
        val fromB = record(SyncKinds.LANGUAGE, 100, "b")
        assertEquals(fromB, SyncMerge.merge(fromA, fromB), "same millisecond: the greater device id")
        assertEquals(fromB, SyncMerge.merge(fromB, fromA))
    }

    /** Deleted on one device and changed on another: whichever came later stands. */
    @Test
    fun aDeletionAndAChangeGoByTime() {
        val changed = term(100, 1, 100, "house")
        val deleted = record(SyncKinds.TERM, 200, deleted = true)
        assertTrue(SyncMerge.merge(changed, deleted).deleted)
        val changedAfter = term(300, 2, 300, "home")
        assertEquals(changedAfter, SyncMerge.merge(deleted, changedAfter), "a change after the deletion brings the word back")
    }

    /** One device moves the word on, another later edits its translation: both survive. */
    @Test
    fun aWordKeepsTheLatestStatusAndTheLatestOtherEdits() {
        val learnt = term(at = 100, status = 3, statusAt = 100, translation = "house", device = "phone")
        val translated = term(at = 200, status = 1, statusAt = 10, translation = "home", device = "desktop")
        val merged = SyncMerge.merge(learnt, translated)
        assertEquals(200, merged.updatedAt)
        assertEquals(JsonPrimitive(3), merged.data!![TermFields.STATUS], "the status set last")
        assertEquals(JsonPrimitive(100L), merged.data!![TermFields.STATUS_CHANGED_AT])
        assertEquals(JsonPrimitive("home"), merged.data!!["translation"], "the translation edited last")
        assertEquals(merged, SyncMerge.merge(translated, learnt), "the same whichever way round")
    }

    @Test
    fun aCardGoesByItsLastReview() {
        fun card(at: Long, lastReview: Long?, device: String) = record(SyncKinds.FLASHCARD, at, device, data = buildJsonObject { lastReview?.let { put(FlashcardFields.LAST_REVIEW, it) } })
        val reviewed = card(at = 100, lastReview = 90, device = "phone")
        val touchedLater = card(at = 200, lastReview = 50, device = "desktop")
        assertEquals(reviewed, SyncMerge.merge(reviewed, touchedLater), "the card reviewed last, though the other changed later")
        assertEquals(reviewed, SyncMerge.merge(touchedLater, reviewed))
        val neverReviewed = card(at = 300, lastReview = null, device = "tablet")
        assertEquals(reviewed, SyncMerge.merge(neverReviewed, reviewed), "a review beats none")
    }

    @Test
    fun aPageReadStaysRead() {
        fun page(at: Long, read: Long?, started: Long?) = record(SyncKinds.PAGE, at, data = buildJsonObject {
            read?.let { put(PageFields.READ_DATE, it) }
            started?.let { put(PageFields.START_DATE, it) }
            put("text", "t$at")
        })
        val readHere = page(at = 100, read = 100, started = 90)
        val editedThere = page(at = 200, read = null, started = 150)
        val merged = SyncMerge.merge(readHere, editedThere)
        assertEquals(JsonPrimitive(100L), merged.data!![PageFields.READ_DATE])
        assertEquals(JsonPrimitive(90L), merged.data!![PageFields.START_DATE], "first opened when it first was")
        assertEquals(JsonPrimitive("t200"), merged.data!!["text"], "the text edited last")
    }

    /** Devices and the server meet versions in different orders; they must all end with the same record. */
    @Test
    fun theOrderVersionsMeetInDoesNotMatter() {
        val versions = listOf(
            term(100, 3, 100, "house", "phone"),
            term(200, 1, 10, "home", "desktop"),
            term(150, 4, 150, "dwelling", "tablet"),
            record(SyncKinds.TERM, 120, "web", deleted = true),
        )
        val results = permutations(versions).map { order -> order.reduce(SyncMerge::merge) }.toSet()
        assertEquals(1, results.size, results.toString())
        val result = results.single()
        assertFalse(result.deleted)
        assertEquals(JsonPrimitive(4), result.data!![TermFields.STATUS])
        assertEquals(JsonPrimitive("home"), result.data!!["translation"])
        // Merging what is already merged changes nothing.
        assertEquals(result, SyncMerge.merge(result, versions[1]))
    }

    /**
     * For random versions of each kind (close times so they tie, few devices, some deletions), every
     * order and grouping of merges gives one record, and merging in any version again changes nothing.
     */
    @Test
    fun mergingConvergesWhateverTheOrderAndGrouping() {
        val random = Random(7)
        fun version(kind: String): SyncRecord {
            val deleted = random.nextInt(4) == 0
            val data = buildJsonObject {
                when (kind) {
                    SyncKinds.TERM -> { put(TermFields.STATUS, random.nextInt(1, 6)); put(TermFields.STATUS_CHANGED_AT, random.nextLong(0, 6)) }
                    SyncKinds.PAGE -> { if (random.nextBoolean()) put(PageFields.READ_DATE, random.nextLong(0, 6)); if (random.nextBoolean()) put(PageFields.START_DATE, random.nextLong(0, 6)) }
                    SyncKinds.FLASHCARD -> if (random.nextBoolean()) put(FlashcardFields.LAST_REVIEW, random.nextLong(0, 6))
                }
                put("note", "n${random.nextInt(3)}")
            }
            return SyncRecord(kind, "k", random.nextLong(0, 6), deleted, if (deleted) null else data, listOf("a", "b", "c")[random.nextInt(3)])
        }
        for (kind in listOf(SyncKinds.TERM, SyncKinds.PAGE, SyncKinds.FLASHCARD, SyncKinds.PRACTICE_PROGRESS, SyncKinds.LANGUAGE)) {
            repeat(60) {
                val versions = List(5) { version(kind) }
                val folds = permutations(versions).map { it.reduce(SyncMerge::merge) }.toSet()
                assertEquals(1, folds.size, "$kind: $versions gave $folds")
                val result = folds.single()
                val grouped = SyncMerge.merge(SyncMerge.merge(versions[0], versions[1]), SyncMerge.merge(versions[2], SyncMerge.merge(versions[3], versions[4])))
                assertEquals(result, grouped, "$kind grouped: $versions")
                versions.forEach { assertEquals(result, SyncMerge.merge(result, it), "$kind again: $it") }
            }
        }
    }

    @Test
    fun keysNameLanguagesByCodeAndWordsByText() {
        assertEquals("pt/a/b", SyncKeys.term("pt", "a/b"))
        assertEquals("pt" to "a/b", SyncKeys.termParts("pt/a/b"))
        assertNull(SyncKeys.termParts("/x"))
        assertEquals("1700000000000/pt/casa", SyncKeys.review("pt", "casa", 1700000000000))
    }

    @Test
    fun recordsFromANewerAppAreRead() {
        val json = """{"kind":"term","key":"pt/casa","updatedAt":5,"data":{"status":1},"deviceId":"x","schema":3}"""
        val record = SyncJson.decodeFromString(SyncRecord.serializer(), json)
        assertEquals("pt/casa", record.key)
        assertEquals(JsonPrimitive(1), record.data!!["status"])
    }

    private fun <T> permutations(items: List<T>): List<List<T>> =
        if (items.size <= 1) listOf(items) else items.indices.flatMap { i -> permutations(items - items[i]).map { listOf(items[i]) + it } }
}
