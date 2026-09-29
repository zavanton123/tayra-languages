package com.tayra.languages.core.data.speech

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Plain HTTP downloads that work the same on the desktop JVM and on Android. */
object FileDownloads {
    /** Streams [url] into [target] through a `.part` file, reporting bytes done and total. Blocks. */
    fun download(url: String, target: File, onProgress: (Long, Long) -> Unit = { _, _ -> }) {
        var location = url
        var connection: HttpURLConnection
        var hops = 0
        while (true) {
            connection = (URL(location).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 30_000
                readTimeout = 60_000
                setRequestProperty("User-Agent", "TayraLanguages")
            }
            val code = connection.responseCode
            if (code in 300..399) {
                val next = connection.getHeaderField("Location") ?: error("redirect without a location")
                connection.disconnect()
                location = URL(URL(location), next).toString()
                if (++hops > 8) error("too many redirects")
                continue
            }
            if (code != 200) { connection.disconnect(); error("download failed with HTTP $code") }
            break
        }
        val total = connection.contentLengthLong
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, target.name + ".part")
        var done = 0L
        try {
            connection.inputStream.use { input ->
                partial.outputStream().use { out ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        val n = input.read(buffer); if (n < 0) break
                        out.write(buffer, 0, n); done += n
                        onProgress(done, if (total > 0) total else done)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        target.delete()
        if (!partial.renameTo(target)) error("could not save ${target.name}")
    }
}
