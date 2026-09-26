package com.tayra.languages.core.data.dictionary

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.http.isSuccess

/** Fetches a pack file into memory; packs are a few megabytes, so no streaming to disk is needed. */
class DictionaryDownloader(private val client: HttpClient) {
    suspend fun download(url: String, onProgress: (Float?) -> Unit): ByteArray {
        val response = client.get(url) {
            onDownload { sent, total -> onProgress(total?.takeIf { it > 0 }?.let { sent.toFloat() / it }) }
        }
        check(response.status.isSuccess()) { "Server answered ${response.status.value}" }
        return response.body()
    }
}
