package com.tayra.languages.core.data.files

class FileImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Reads the text of an epub file, or throws [FileImportException] when unsupported on this platform. */
expect fun extractEpubText(bytes: ByteArray): String

/**
 * Extracts book text from uploaded files: txt, srt, vtt and epub.
 */
object FileTextExtractor {
    val supportedExtensions = listOf("txt", "epub", "srt", "vtt")

    fun extract(fileName: String, bytes: ByteArray): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val content = when (extension) {
            "txt" -> decodeUtf8(fileName, bytes)
            "srt" -> subtitleText(decodeUtf8(fileName, bytes))
            "vtt" -> vttText(decodeUtf8(fileName, bytes))
            "epub" -> extractEpubText(bytes)
            else -> throw FileImportException("Unknown file extension \"$extension\"")
        }.trim()
        if (content.isEmpty()) throw FileImportException("$fileName is empty.")
        return content
    }

    private fun decodeUtf8(fileName: String, bytes: ByteArray): String = try {
        bytes.decodeToString(throwOnInvalidSequence = true).removePrefix("﻿")
    } catch (e: CharacterCodingException) {
        throw FileImportException("$fileName is not utf-8 encoding, please convert it to utf-8 first", e)
    }

    private val timing = Regex("""^\d{1,2}:\d{2}:\d{2}[,.]\d{3}\s*-->""")
    private val vttTiming = Regex("""^(\d{1,2}:)?\d{2}:\d{2}\.\d{3}\s*-->""")
    private val cueTags = Regex("<[^>]+>")

    /** Subtitle lines from SRT: skips indexes and timings, joins each cue. */
    internal fun subtitleText(content: String): String = cues(content.replace("\r\n", "\n"), timing)

    internal fun vttText(content: String): String {
        var lines = content.replace("\r\n", "\n").split("\n")
        if (lines.size > 2 && lines[1].startsWith("Kind:") && lines[2].startsWith("Language:")) {
            lines = lines.take(1) + lines.drop(3)
        }
        val body = lines.dropWhile { it.startsWith("WEBVTT") || it.isBlank() }
            .filterNot { it.startsWith("NOTE") }
        return cues(body.joinToString("\n"), vttTiming)
    }

    private fun cues(content: String, timingPattern: Regex): String {
        val result = mutableListOf<String>()
        for (block in content.split(Regex("\n\\s*\n"))) {
            val lines = block.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            val start = lines.indexOfFirst { timingPattern.containsMatchIn(it) }
            if (start < 0) continue
            val text = lines.drop(start + 1).joinToString(" ") { cueTags.replace(it, "") }.trim()
            if (text.isNotEmpty()) result.add(text)
        }
        return result.joinToString("\n")
    }
}
