package com.tayra.languages.core.data.sync

import com.tayra.languages.sync.FlashcardFields
import com.tayra.languages.sync.PracticeProgressFields
import com.tayra.languages.sync.SyncKeys
import com.tayra.languages.sync.SyncKinds
import com.tayra.languages.sync.SyncRecord
import com.tayra.languages.sync.TermFields
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

// The rows sync reads, in the order of their queries' columns, and the records they travel as. The
// same mapping makes the record a device sends and the version it merges a pulled record with, so
// a row that did not change reads as the record it came from.

/** Kinds of record a language removal is sent as: its time, which older records of the language lose to. */
internal const val LANGUAGE_REMOVED = "language_removed"

internal data class TermRow(
    val id: Long, val language: String, val text: String, val textLc: String, val status: Long, val translation: String?,
    val romanization: String?, val tokenCount: Long, val flashMessage: String?, val createdAt: Long, val statusChangedAt: Long,
    val translationLanguage: String?, val sentence: String?, val updatedAt: Long, val parents: String, val level: Boolean,
) {
    fun record(code: String, device: String) = SyncRecord(SyncKinds.TERM, SyncKeys.term(code, textLc), updatedAt, data = buildJsonObject {
        put("text", text)
        put(TermFields.STATUS, status)
        put(TermFields.STATUS_CHANGED_AT, statusChangedAt)
        translation?.let { put("translation", it) }
        romanization?.let { put("romanization", it) }
        put("tokenCount", tokenCount)
        flashMessage?.let { put("flashMessage", it) }
        translationLanguage?.let { put("translationLanguage", it) }
        sentence?.let { put("sentence", it) }
        put("createdAt", createdAt)
        put("parents", buildJsonArray { parents.split(PARENT_SEPARATOR).filter { it.isNotEmpty() }.sorted().forEach { add(JsonPrimitive(it)) } })
        put("level", level)
    }, deviceId = device)

    companion object {
        const val PARENT_SEPARATOR = '\u001f'
    }
}

internal data class FlashcardRow(
    val termId: Long, val state: Long, val due: Long, val stability: Double?, val difficulty: Double?, val intervalDays: Long,
    val remainingSteps: Long, val reps: Long, val lapses: Long, val lastReview: Long?, val suspended: Boolean, val seenStatus: Long,
    val updatedAt: Long, val textLc: String, val language: String,
) {
    fun record(code: String, device: String) = SyncRecord(SyncKinds.FLASHCARD, SyncKeys.ofTerm(code, textLc), updatedAt, data = buildJsonObject {
        put("state", state)
        put("due", due)
        stability?.let { put("stability", it) }
        difficulty?.let { put("difficulty", it) }
        put("intervalDays", intervalDays)
        put("remainingSteps", remainingSteps)
        put("reps", reps)
        put("lapses", lapses)
        lastReview?.let { put(FlashcardFields.LAST_REVIEW, it) }
        put("suspended", suspended)
        put("seenStatus", seenStatus)
    }, deviceId = device)
}

internal data class ReviewRow(
    val id: Long, val termId: Long, val reviewedAt: Long, val rating: Long, val stateBefore: Long, val intervalDays: Long,
    val elapsedDays: Long, val updatedAt: Long, val textLc: String, val language: String,
) {
    fun record(code: String, device: String) = SyncRecord(SyncKinds.REVIEW, SyncKeys.review(code, textLc, reviewedAt), updatedAt, data = buildJsonObject {
        put("rating", rating)
        put("stateBefore", stateBefore)
        put("intervalDays", intervalDays)
        put("elapsedDays", elapsedDays)
    }, deviceId = device)
}

internal data class ProgressRow(
    val termId: Long, val level: Long, val streak: Long, val asked: Long, val rightAnswers: Long, val lastPractised: Long?,
    val updatedAt: Long, val textLc: String, val language: String,
) {
    fun record(code: String, device: String) = SyncRecord(SyncKinds.PRACTICE_PROGRESS, SyncKeys.ofTerm(code, textLc), updatedAt, data = buildJsonObject {
        put("level", level)
        put("streak", streak)
        put("asked", asked)
        put("rightAnswers", rightAnswers)
        lastPractised?.let { put(PracticeProgressFields.LAST_PRACTISED, it) }
    }, deviceId = device)
}

internal data class ExampleRow(
    val id: Long, val termId: Long, val sentence: String, val translation: String?, val form: String, val formStart: Long,
    val source: String, val sourceId: String?, val fetchedAt: Long, val updatedAt: Long, val textLc: String, val language: String,
) {
    fun record(code: String, device: String) = SyncRecord(SyncKinds.PRACTICE_EXAMPLE, SyncKeys.practiceExample(code, textLc, sentence), updatedAt, data = buildJsonObject {
        translation?.let { put("translation", it) }
        put("form", form)
        put("formStart", formStart)
        put("source", source)
        sourceId?.let { put("sourceId", it) }
        put("fetchedAt", fetchedAt)
    }, deviceId = device)
}

internal data class LevelRow(val level: Long, val updatedAt: Long, val language: String) {
    fun record(code: String, device: String) =
        SyncRecord(SyncKinds.VOCABULARY_LEVEL, SyncKeys.language(code), updatedAt, data = buildJsonObject { put("level", level) }, deviceId = device)
}

internal data class LanguageRow(val id: Long, val name: String, val showRomanization: Boolean, val updatedAt: Long, val script: String)

internal data class DictionaryRow(val useFor: String, val dictType: String, val url: String, val active: Boolean, val order: Long)

internal fun languageRecord(row: LanguageRow, dictionaries: List<DictionaryRow>, code: String, device: String) =
    SyncRecord(SyncKinds.LANGUAGE, SyncKeys.language(code), row.updatedAt, data = buildJsonObject {
        put("showRomanization", row.showRomanization)
        if (row.script.isNotEmpty()) put("script", row.script)
        put("dictionaries", buildJsonArray {
            dictionaries.forEach { d ->
                add(buildJsonObject { put("useFor", d.useFor); put("dictType", d.dictType); put("url", d.url); put("active", d.active); put("order", d.order) })
            }
        })
    }, deviceId = device)

// Reading a record's fields back.

internal fun JsonObject.string(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull

internal fun JsonObject.long(name: String): Long? = (this[name] as? JsonPrimitive)?.longOrNull

internal fun JsonObject.double(name: String): Double? = (this[name] as? JsonPrimitive)?.doubleOrNull

internal fun JsonObject.bool(name: String): Boolean? = (this[name] as? JsonPrimitive)?.booleanOrNull

internal fun JsonObject.strings(name: String): List<String> = (this[name] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()

internal fun JsonObject.objects(name: String): List<JsonObject> = (this[name] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
