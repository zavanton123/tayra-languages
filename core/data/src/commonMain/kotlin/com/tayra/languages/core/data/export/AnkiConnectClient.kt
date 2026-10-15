package com.tayra.languages.core.data.export

import com.tayra.languages.core.domain.export.AnkiAddResult
import com.tayra.languages.core.domain.export.AnkiCollection
import com.tayra.languages.core.domain.export.AnkiConnection
import com.tayra.languages.core.domain.export.AnkiNote
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64

/**
 * Adds notes to a running Anki through the AnkiConnect add-on (version 6 of its API), as Yomitan's
 * AnkiConnect client does: the note type and decks are created when missing, media files stored,
 * and notes Anki already has (the same first field in the deck) are left out after asking
 * canAddNotesWithErrorDetail.
 */
class AnkiConnectClient(
    private val client: HttpClient,
    private val url: String = DEFAULT_URL,
    /** The API key set in AnkiConnect's configuration, when one is. */
    private val apiKey: () -> String? = { null },
) : AnkiConnection {

    override suspend fun version(): Int? = try {
        call("version").jsonPrimitive.intOrNull
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    override suspend fun add(collection: AnkiCollection): AnkiAddResult {
        val type = collection.noteType
        val models = call("modelNames").jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
        if (type.name !in models) {
            call("createModel", buildJsonObject {
                put("modelName", type.name)
                putJsonArray("inOrderFields") { type.fields.forEach { add(JsonPrimitive(it)) } }
                put("css", type.css)
                put("isCloze", true)
                putJsonArray("cardTemplates") {
                    add(buildJsonObject {
                        put("Name", "Cloze")
                        put("Front", type.front)
                        put("Back", type.back)
                    })
                }
            })
        }
        for (deck in collection.notes.map { it.deck }.distinct()) call("createDeck", buildJsonObject { put("deck", deck) })

        val notes = collection.notes.map { noteJson(it, type.name, type.fields) }
        val checks = canAdd(notes)
        val duplicates = HashSet<Int>()
        val failed = HashMap<Int, String>()
        val addable = ArrayList<Int>()
        checks.forEachIndexed { index, (canAdd, error) ->
            when {
                canAdd -> addable += index
                error == null || "duplicate" in error -> duplicates += index
                else -> failed[index] = error
            }
        }
        // Only the media the added notes play.
        val needed = addable.flatMap { collection.notes[it].fields }.joinToString(" ")
        for (media in collection.media.filter { "[sound:${it.name}]" in needed }) {
            call("storeMediaFile", buildJsonObject {
                put("filename", media.name)
                put("data", Base64.encode(media.bytes))
            })
        }
        val added = HashSet<Int>()
        if (addable.isNotEmpty()) {
            val ids = call("addNotes", buildJsonObject { put("notes", JsonArray(addable.map { notes[it] })) }).jsonArray
            ids.forEachIndexed { i, id -> if (id !is JsonNull) added += addable[i] else failed[addable[i]] = "Anki did not add the note" }
        }
        return AnkiAddResult(added, duplicates, failed)
    }

    /** Whether each note can be added, and why not; an AnkiConnect older than canAddNotesWithErrorDetail only says whether. */
    private suspend fun canAdd(notes: List<JsonObject>): List<Pair<Boolean, String?>> {
        val params = buildJsonObject { put("notes", JsonArray(notes)) }
        return try {
            call("canAddNotesWithErrorDetail", params).jsonArray.map { result ->
                val detail = result.jsonObject
                (detail["canAdd"]?.jsonPrimitive?.booleanOrNull == true) to detail["error"]?.jsonPrimitive?.contentOrNull
            }
        } catch (e: AnkiConnectException) {
            if ("unsupported action" !in e.message.orEmpty()) throw e
            call("canAddNotes", params).jsonArray.map { (it.jsonPrimitive.booleanOrNull == true) to null }
        }
    }

    private fun noteJson(note: AnkiNote, model: String, fieldNames: List<String>): JsonObject = buildJsonObject {
        put("deckName", note.deck)
        put("modelName", model)
        putJsonObject("fields") { fieldNames.forEachIndexed { i, name -> put(name, note.fields.getOrElse(i) { "" }) } }
        putJsonArray("tags") { note.tags.forEach { add(JsonPrimitive(it)) } }
        putJsonObject("options") {
            put("allowDuplicate", false)
            put("duplicateScope", "deck")
        }
    }

    private suspend fun call(action: String, params: JsonObject? = null): JsonElement {
        val request = buildJsonObject {
            put("action", action)
            put("version", API_VERSION)
            params?.let { put("params", it) }
            apiKey()?.takeIf { it.isNotBlank() }?.let { put("key", it) }
        }
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(request.toString())
        }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        (body["error"] as? JsonPrimitive)?.contentOrNull?.let { throw AnkiConnectException(it) }
        return body["result"] ?: JsonNull
    }

    companion object {
        /** Where AnkiConnect listens unless configured otherwise. */
        const val DEFAULT_URL = "http://127.0.0.1:8765"
        private const val API_VERSION = 6
        private val json = Json { ignoreUnknownKeys = true }
    }
}

/** An error AnkiConnect answered with. */
class AnkiConnectException(message: String) : Exception(message)
