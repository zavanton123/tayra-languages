package com.idiomiko.core.domain.fonts

import kotlinx.coroutines.flow.Flow

/** A font file the reader added to read in, by the name it is listed under. */
data class OwnFont(val id: Long, val name: String)

/** The reader's own font files, kept on this device and not synced. */
interface OwnFontRepository {
    val fonts: Flow<List<OwnFont>>

    /** Keeps the font in [bytes], named after [fileName]; null when they are not a font file the app can use. */
    suspend fun add(fileName: String, bytes: ByteArray): OwnFont?

    suspend fun remove(id: Long)

    /** The font file's contents, or null once it has been removed. */
    suspend fun data(id: Long): ByteArray?
}

/** What makes a file a font the reader can add, and the name it is listed under. */
object FontFiles {
    val pickerExtensions: List<String> = listOf("ttf", "otf")

    const val MAX_BYTES: Int = 30 * 1024 * 1024

    /** TrueType and OpenType fonts begin with one of these; collections of several fonts are not taken. */
    private val signatures = setOf(0x00010000, 0x4F54544F /* OTTO */, 0x74727565 /* true */)

    fun isFont(bytes: ByteArray): Boolean = bytes.size in 12..MAX_BYTES && signature(bytes) in signatures

    /** "MyFont-Regular.ttf" is listed as "MyFont". */
    fun nameOf(fileName: String): String {
        val words = fileName.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
            .replace('_', ' ').replace('-', ' ').split(' ').filter { it.isNotBlank() }
        return words.dropLastWhile { it.equals("Regular", ignoreCase = true) }.joinToString(" ").ifEmpty { fileName }
    }

    private fun signature(bytes: ByteArray): Int =
        (0 until 4).fold(0) { acc, i -> (acc shl 8) or (bytes[i].toInt() and 0xFF) }
}
