package com.tayra.languages.sync

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * How two versions of the same record become one, on every device and on the server alike. Devices
 * and the server meet versions in different orders and in different groupings, so merging gives the
 * same record whatever the order and the grouping, and merging a version in twice changes nothing;
 * that is what brings every device to the same library once all have synced.
 *
 * - Most kinds: the later change wins whole; a deletion is a change. Ties go to the greater device id.
 * - A word's status goes by when it was set, apart from its other fields: one device moving a word on
 *   and another later editing its translation both stand. A deletion keeps the status it knew, so a
 *   word brought back by a later change does not lose a status set elsewhere.
 * - A card and practice progress go whole by when they were last used (a deletion by when it was
 *   made), since their fields only make sense together.
 * - A page's first opening and reading keep the earliest time any version had: a page read stays read.
 */
object SyncMerge {

    /**
     * Whether [a] is the later change of the two: by time, then by device, then deletion, then content,
     * leaving out the fields merged on their own, which merging rewrites.
     */
    fun isNewer(a: SyncRecord, b: SyncRecord): Boolean = when {
        a.updatedAt != b.updatedAt -> a.updatedAt > b.updatedAt
        a.deviceId != b.deviceId -> a.deviceId > b.deviceId
        a.deleted != b.deleted -> a.deleted
        else -> content(a) > content(b)
    }

    private fun content(r: SyncRecord): String = r.data?.let { data -> JsonObject(data - registers(r.kind)).toString() }.orEmpty()

    private fun registers(kind: String): Set<String> = when (kind) {
        SyncKinds.TERM -> setOf(TermFields.STATUS, TermFields.STATUS_CHANGED_AT)
        SyncKinds.PAGE -> setOf(PageFields.START_DATE, PageFields.READ_DATE)
        else -> emptySet()
    }

    /** The later of two versions, whole. */
    fun latest(a: SyncRecord, b: SyncRecord): SyncRecord = if (isNewer(a, b)) a else b

    /** The record kept for [a] and [b], two versions of the same kind and key. */
    fun merge(a: SyncRecord, b: SyncRecord): SyncRecord {
        require(a.kind == b.kind && a.key == b.key) { "Only versions of one record merge" }
        return when (a.kind) {
            SyncKinds.TERM -> withRegisters(latest(a, b), statusOf(a, b))
            SyncKinds.PAGE -> withRegisters(latest(a, b), earliestDates(a, b))
            SyncKinds.FLASHCARD -> byUse(a, b, FlashcardFields.LAST_REVIEW)
            SyncKinds.PRACTICE_PROGRESS -> byUse(a, b, PracticeProgressFields.LAST_PRACTISED)
            else -> latest(a, b)
        }
    }

    /**
     * [winner] with [registers] in its fields; a deletion keeps only them, so they live on in the
     * account and come back with the record if a later change brings it back.
     */
    private fun withRegisters(winner: SyncRecord, registers: Map<String, JsonPrimitive>): SyncRecord = when {
        registers.isEmpty() -> if (winner.deleted) winner.copy(data = null) else winner
        winner.deleted -> winner.copy(data = JsonObject(registers))
        else -> winner.copy(data = JsonObject(winner.data.orEmpty() + registers))
    }

    /** A word's status and when it was set, from whichever version set it last; the higher status on a tie. */
    private fun statusOf(a: SyncRecord, b: SyncRecord): Map<String, JsonPrimitive> {
        fun status(r: SyncRecord): Pair<Long, Long>? {
            val at = r.data?.long(TermFields.STATUS_CHANGED_AT) ?: return null
            val status = r.data.long(TermFields.STATUS) ?: return null
            return at to status
        }
        val chosen = listOfNotNull(status(a), status(b)).maxWithOrNull(compareBy<Pair<Long, Long>> { it.first }.thenBy { it.second }) ?: return emptyMap()
        return mapOf(TermFields.STATUS_CHANGED_AT to JsonPrimitive(chosen.first), TermFields.STATUS to JsonPrimitive(chosen.second))
    }

    /** Each of a page's dates at the earliest any version has it. */
    private fun earliestDates(a: SyncRecord, b: SyncRecord): Map<String, JsonPrimitive> =
        listOf(PageFields.START_DATE, PageFields.READ_DATE).mapNotNull { field ->
            listOfNotNull(a.data?.long(field), b.data?.long(field)).minOrNull()?.let { field to JsonPrimitive(it) }
        }.toMap()

    /**
     * The whole version used last, by one order of all versions: when it was last used ([field]), a
     * deletion counting as used when it was made and a version never used as never; then the later
     * change.
     */
    private fun byUse(a: SyncRecord, b: SyncRecord, field: String): SyncRecord {
        fun used(r: SyncRecord): Long = if (r.deleted) r.updatedAt else r.data?.long(field) ?: Long.MIN_VALUE
        val x = used(a)
        val y = used(b)
        return if (x != y) (if (x > y) a else b) else latest(a, b)
    }

    private fun JsonObject?.orEmpty(): Map<String, kotlinx.serialization.json.JsonElement> = this ?: emptyMap()

    private fun JsonObject.long(field: String): Long? = (this[field] as? JsonPrimitive)?.longOrNull
}

/** The fields of a word's record that merging reads. */
object TermFields {
    const val STATUS = "status"
    const val STATUS_CHANGED_AT = "statusChangedAt"
}

object FlashcardFields {
    const val LAST_REVIEW = "lastReview"
}

object PracticeProgressFields {
    const val LAST_PRACTISED = "lastPractised"
}

object PageFields {
    const val START_DATE = "startDate"
    const val READ_DATE = "readDate"
}
