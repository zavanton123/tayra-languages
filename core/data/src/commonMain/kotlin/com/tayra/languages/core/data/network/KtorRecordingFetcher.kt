package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.service.RecordingFetcher
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException

/** Downloads example recordings; a refused one (Tatoeba answers 403 for some) gives null. */
class KtorRecordingFetcher(private val client: HttpClient) : RecordingFetcher {
    override suspend fun fetch(url: String): ByteArray? = try {
        val response = client.get(url)
        if (response.status == HttpStatusCode.OK) response.bodyAsBytes() else null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w(e) { "Could not download recording $url" }
        null
    }
}
