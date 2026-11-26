package com.idiomiko.core.data

import com.idiomiko.core.data.frequency.DownloadedFrequencyLists
import com.idiomiko.core.domain.frequency.FrequencyListDownloads
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** The frequency lists an Android build fetches: downloaded once from their release, then read from the device. */
class DownloadedFrequencyListsTest {
    private val list = "# source\nde\tdo\nque\nnão\n".encodeToByteArray()
    private val directory = Files.createTempDirectory("idiomiko-frequency").toFile()
    private val asked = mutableListOf<String>()

    private fun lists(status: HttpStatusCode = HttpStatusCode.OK) = DownloadedFrequencyLists(
        HttpClient(MockEngine { request ->
            asked += request.url.toString()
            if (status == HttpStatusCode.OK) respond(gzip(list)) else respondError(status)
        }),
        directory,
    )

    private fun gzip(bytes: ByteArray) = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(bytes) } }.toByteArray()

    @Test
    fun aListIsFetchedOnceAndKept() = runTest {
        val lists = lists()
        assertEquals(list.decodeToString(), lists.fetch("pt")?.decodeToString())
        assertEquals(listOf("${FrequencyListDownloads.URL}/pt.tsv.gz"), asked)
        assertEquals(list.decodeToString(), lists.fetch("pt")?.decodeToString())
        assertEquals(1, asked.size, "the second time it is read from the device")
        assertEquals(list.decodeToString(), directory.resolve("v${FrequencyListDownloads.VERSION}/pt.tsv").readText())
    }

    @Test
    fun aLanguageWithNoListHasNone() = runTest {
        assertNull(lists(HttpStatusCode.NotFound).fetch("xx"))
    }

    @Test
    fun aFailedDownloadKeepsNothing() = runTest {
        assertFailsWith<IllegalStateException> { lists(HttpStatusCode.ServiceUnavailable).fetch("pt") }
        assertEquals(false, directory.resolve("v${FrequencyListDownloads.VERSION}/pt.tsv").exists())
    }
}
