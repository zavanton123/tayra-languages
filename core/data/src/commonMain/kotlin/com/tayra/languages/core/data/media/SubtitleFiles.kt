package com.tayra.languages.core.data.media

import com.tayra.languages.core.data.files.TextDecoding
import com.tayra.languages.core.domain.media.Cue
import com.tayra.languages.core.domain.media.SubtitleFormat
import com.tayra.languages.core.domain.media.SubtitleFormats

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

    /** Extensions of the video and audio files the players open, for the file picker. */
    val mediaExtensions = listOf("mp4", "m4v", "mov", "webm", "mkv", "avi", "ogv", "mp3", "m4a", "aac", "wav", "ogg", "opus", "flac")

    val subtitleExtensions = listOf("srt", "vtt", "ass", "ssa", "json3")
}
