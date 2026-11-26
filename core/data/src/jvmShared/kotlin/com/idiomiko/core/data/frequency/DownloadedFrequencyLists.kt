package com.idiomiko.core.data.frequency

import com.idiomiko.core.domain.frequency.FrequencyListDownloads
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import java.io.File
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The frequency lists fetched from [FrequencyListDownloads.URL] the first time a language needs one, then kept in
 * [directory] under the lists' version, so a new version is fetched afresh.
 */
class DownloadedFrequencyLists(private val client: HttpClient, directory: File) : FrequencyListDownloads {
    private val directory = File(directory, "v${FrequencyListDownloads.VERSION}")

    override suspend fun fetch(languageCode: String): ByteArray? {
        val file = File(directory, "$languageCode.tsv")
        if (file.isFile) return withContext(Dispatchers.IO) { file.readBytes() }
        val response = client.get("${FrequencyListDownloads.URL}/$languageCode.tsv.gz")
        // No list for the language: nothing to fetch, now or later.
        if (response.status == HttpStatusCode.NotFound) return null
        check(response.status.isSuccess()) { "Server answered ${response.status.value}" }
        val packed: ByteArray = response.body()
        return withContext(Dispatchers.IO) {
            val bytes = GZIPInputStream(packed.inputStream()).use { it.readBytes() }
            directory.mkdirs()
            val temp = File(directory, "$languageCode.tsv.tmp")
            temp.writeBytes(bytes)
            file.delete()
            check(temp.renameTo(file)) { "Could not move ${temp.path} to ${file.path}" }
            bytes
        }
    }
}
