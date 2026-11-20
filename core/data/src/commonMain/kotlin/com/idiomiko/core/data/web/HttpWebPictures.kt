package com.idiomiko.core.data.web

import co.touchlab.kermit.Logger
import com.idiomiko.core.domain.web.WebPictures
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/** Fetches pictures with a client of no cache of its own, as the screens keep the pictures they show. */
class HttpWebPictures(private val client: HttpClient) : WebPictures {
    override suspend fun bytes(url: String): ByteArray? = try {
        val response = client.get(url)
        val length = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (!response.status.isSuccess() || (length != null && length > MAX_BYTES)) null
        else response.bodyAsBytes().takeIf { it.size <= MAX_BYTES }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.d { "Picture $url: ${e.message}" }
        null
    }

    private companion object {
        /** A news site's picture is well under this; anything bigger is not worth fetching for a card. */
        const val MAX_BYTES = 5_000_000
    }
}
