package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.service.WordRecording
import com.tayra.languages.core.domain.service.WordRecordingSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

/**
 * Recordings from Wikimedia Commons, searched as Yomitan's audio sources search them: Lingua Libre's
 * (named "LL-Q<id> (<ISO 639-3>)-<speaker>-<word>.wav") and Wiktionary's ("<code>(-<region>)-<word>.ogg").
 * Each plays as the MP3 Commons transcodes it to.
 */
class CommonsRecordingSource(
    private val client: HttpClient,
    private val apiUrl: String = "https://commons.wikimedia.org/w/api.php",
) : WordRecordingSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun recordings(word: String, languageCode: String): List<WordRecording> {
        if (!word.all { it.isLetter() || it in ALLOWED }) return emptyList()
        val escaped = Regex.escape(word)
        val linguaLibre = linguaLibreCode(languageCode)?.let { iso3 ->
            val names = Regex("""^File:LL-Q\d+\s+\(${Regex.escape(iso3)}\)-(.+)-$escaped\.wav$""", RegexOption.IGNORE_CASE)
            search("intitle:/-$word\\.wav/i incategory:\"Lingua_Libre_pronunciation-$iso3\"").mapNotNull { file ->
                val speaker = names.find(file.title)?.groupValues?.get(1) ?: return@mapNotNull null
                WordRecording(word, file.mp3, speaker, "Lingua Libre", file.page)
            }
        }.orEmpty()
        val names = Regex("""^File:${Regex.escape(languageCode)}(-\w\w)?-$escaped\d*\.ogg$""", RegexOption.IGNORE_CASE)
        val wiktionary = search("intitle:/$languageCode(-[a-zA-Z]{2})?-$word[0123456789]*\\.ogg/i")
            .filter { names.matches(it.title) }
            .map { WordRecording(word, it.mp3, it.user, "Wiktionary", it.page) }
        return (linguaLibre + wiktionary).distinctBy { it.url }.take(MAX_RECORDINGS)
    }

    private class File(val title: String, val mp3: String, val user: String, val page: String)

    private suspend fun search(query: String): List<File> {
        val body = try {
            val response = client.get(apiUrl) {
                parameter("action", "query")
                parameter("format", "json")
                parameter("formatversion", "2")
                parameter("origin", "*")
                parameter("generator", "search")
                parameter("gsrsearch", query)
                parameter("gsrnamespace", "6")
                parameter("gsrlimit", "10")
                parameter("prop", "videoinfo")
                parameter("viprop", "user|url|derivatives")
            }
            if (response.status != HttpStatusCode.OK) return emptyList()
            response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Commons recording search failed" }
            return emptyList()
        }
        return runCatching { parse(body) }.getOrElse { emptyList() }
    }

    private fun parse(body: String): List<File> {
        val pages = json.parseToJsonElement(body).jsonObject["query"]?.jsonObject?.get("pages")?.jsonArray ?: return emptyList()
        return pages.mapNotNull { element ->
            val page = element.jsonObject
            val title = page.string("title") ?: return@mapNotNull null
            val info = page["videoinfo"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@mapNotNull null
            val mp3 = info["derivatives"]?.jsonArray?.map { it.jsonObject }
                ?.firstOrNull { it.string("transcodekey") == "mp3" }?.string("src") ?: return@mapNotNull null
            File(title, mp3, info.string("user").orEmpty(), info.string("descriptionurl").orEmpty())
        }
    }

    private fun JsonObject.string(key: String): String? = runCatching { this[key]?.jsonPrimitive?.content }.getOrNull()

    private companion object {
        const val MAX_RECORDINGS = 6
        /** Besides letters, what a word or short phrase may have; anything else would be read as part of the search regex. */
        const val ALLOWED = " '-’"
        /** Lingua Libre's categories, where they differ from Tatoeba's ISO 639-3 codes. */
        val LINGUA_LIBRE_CODES = mapOf("fa" to "fas", "lv" to "lav", "sw" to "swa", "tok" to "tok")

        fun linguaLibreCode(code: String): String? = LINGUA_LIBRE_CODES[code] ?: LanguageCodes.tatoebaCode(code)
    }
}
