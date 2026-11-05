package com.tayra.languages.core.data.media

import com.tayra.languages.core.data.files.TextDecoding
import com.tayra.languages.core.data.files.pickerFiltersByExtension
import com.tayra.languages.core.domain.media.Cue
import com.tayra.languages.core.domain.media.SubtitleFormat
import com.tayra.languages.core.domain.media.SubtitleFormats
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/** Subtitle files as people have them: any of the common formats, in UTF-8 or an older code page. */
object SubtitleFiles {

    /** The cues of the file [name] with [bytes]; empty when it is no subtitle file. */
    fun read(name: String, bytes: ByteArray): List<Cue> {
        val text = decode(bytes)
        val format = SubtitleFormat.ofExtension(name.substringAfterLast('.', "")) ?: SubtitleFormat.sniff(text)
        return SubtitleFormats.parse(text, format)
    }

    /**
     * The text of a subtitle file: UTF-8 or UTF-16 when it is that, else windows-1251 when most of its
     * letters are Cyrillic there (Russian subtitles still come so), else windows-1252.
     */
    fun decode(bytes: ByteArray): String {
        if (TextDecoding.isUtf(bytes)) return TextDecoding.utf(bytes)
        val high = bytes.count { (it.toInt() and 0xFF) >= 0xC0 }
        val letters = bytes.count { val b = it.toInt() and 0xFF; b in 0x41..0x5A || b in 0x61..0x7A || b >= 0xC0 }
        return TextDecoding.decode(bytes, if (letters > 0 && high * 2 > letters) "windows-1251" else "windows-1252")
    }

    /** Whether [name] looks like a subtitle file. */
    fun isSubtitleFile(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in SubtitleFormat.allExtensions

    /**
     * The extensions to give the file picker, or null for every file: Android's picker filters by MIME type,
     * and Android knows none for .vtt or .ass, so it greys those out.
     */
    val pickerExtensions: Set<String>? get() = if (pickerFiltersByExtension) setOf("srt", "vtt", "ass", "ssa", "json3") else null
}

/** Fetches a subtitle file from the internet, for a video whose subtitles are online too. */
class SubtitleDownloader(private val client: HttpClient) {
    /** The file's name and its bytes, or null when the address gives nothing. */
    suspend fun download(url: String): Pair<String, ByteArray>? = try {
        val response = client.get(url)
        if (!response.status.isSuccess()) null
        else url.substringBefore('?').substringBefore('#').substringAfterLast('/').ifBlank { "subtitles" } to response.readRawBytes()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
