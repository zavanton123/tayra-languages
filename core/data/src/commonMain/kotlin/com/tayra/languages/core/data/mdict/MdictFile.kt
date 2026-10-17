package com.tayra.languages.core.data.mdict

import com.tayra.languages.core.data.files.TextDecoding
import com.tayra.languages.core.data.yomitan.inflateRaw

/**
 * An MDict dictionary (.mdx): an XML header, then its keys (headwords) and its records (each an
 * HTML entry), each part in compressed blocks, as the format was worked out by readmdict
 * (mdict-analysis). Versions 1 and 2 are read, blocks stored or compressed with zlib or LZO, and
 * the index of keys hidden by the common scheme (Encrypted="2"); dictionaries whose records need a
 * registration key (Encrypted="1") are refused. Text is UTF-8, UTF-16 or one of the single-byte
 * charsets [TextDecoding] knows; GBK and Big5 are refused. A "compact" dictionary's records mark
 * their styles with numbers ("`1`word`2`") that its header's style sheet turns back into tags.
 */
internal class MdictFile(private val bytes: ByteArray) {

    /** The header's attributes: Title, Description, Encoding, GeneratedByEngineVersion... */
    val header: Map<String, String>

    private val version: Double
    private val encoding: String
    private val utf16: Boolean
    private val encrypted: Int
    private val numberWidth: Int

    /** Where the keys start, after the header. */
    private val keysAt: Int

    /** The header's style sheet: a style's number, the tags it opens with and closes with. */
    private val styles: Map<String, Pair<String, String>>

    init {
        if (!looksLike(bytes)) throw MdictException("Not an MDict dictionary (.mdx)")
        val headerSize = uint32(0).toInt()
        val text = utf16le(4, headerSize).trimEnd('\u0000')
        if (text.startsWith("<Library_Data")) throw MdictException("This is an MDict resource file (.mdd); import its .mdx")
        header = ATTRIBUTE.findAll(text).associate { it.groupValues[1] to unescape(it.groupValues[2]) }
        version = header["GeneratedByEngineVersion"]?.trim()?.toDoubleOrNull() ?: 2.0
        encoding = header["Encoding"]?.trim()?.uppercase()?.ifEmpty { null } ?: "UTF-8"
        utf16 = encoding.startsWith("UTF-16")
        if (encoding.startsWith("GB") || encoding.startsWith("BIG5") || encoding == "BIG-5") {
            throw MdictException("This dictionary's text is in $encoding, which cannot be read yet; UTF-8 and UTF-16 dictionaries can")
        }
        encrypted = when (val value = header["Encrypted"]?.trim()) {
            null, "", "No" -> 0
            "Yes" -> 1
            else -> value.toIntOrNull() ?: 0
        }
        if (encrypted and 1 != 0) throw MdictException("This dictionary is locked to a registered user and cannot be imported")
        numberWidth = if (version >= 2.0) 8 else 4
        // The header, its length before it and its checksum after it.
        keysAt = 4 + headerSize + 4
        // Three lines a style: its number, the tags before, the tags after.
        styles = header["StyleSheet"]?.replace("\r", "")?.split('\n')?.let { lines ->
            (0 until lines.size - 2 step 3).associate { i -> lines[i].trim() to (lines[i + 1] to lines[i + 2]) }.filterKeys { it.isNotEmpty() }
        }.orEmpty()
    }

    val title: String? get() = header["Title"]?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("Title (No HTML") }

    val description: String? get() = header["Description"]?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("Description (No HTML") }

    /** A key, a headword, and where its record starts among all the records decompressed in turn. */
    class Key(val offset: Long, val text: String)

    /** Every key and its record's text, in the dictionary's order, as [onRecord] calls; [onProgress] goes from 0 to 1. */
    suspend fun read(onProgress: (Float) -> Unit = {}, onRecord: suspend (key: String, record: String) -> Unit) {
        val (keys, recordsAt) = keys()
        var pos = recordsAt
        val blocks = number(pos).toInt()
        pos += 2 * numberWidth
        val infoSize = number(pos).toInt()
        pos += 2 * numberWidth
        val sizes = (0 until blocks).map { i -> number(pos + i * 2 * numberWidth) to number(pos + i * 2 * numberWidth + numberWidth) }
        pos += infoSize
        var offset = 0L
        var next = 0
        sizes.forEachIndexed { b, (compressed, decompressed) ->
            val block = decompress(pos, compressed.toInt(), decompressed.toInt())
            pos += compressed.toInt()
            val end = offset + decompressed
            while (next < keys.size && keys[next].offset < end) {
                val start = (keys[next].offset - offset).toInt()
                val stop = ((keys.getOrNull(next + 1)?.offset ?: end).coerceAtMost(end) - offset).toInt()
                if (start in 0..stop) onRecord(keys[next].text, styled(text(block, start, stop)))
                next++
            }
            offset = end
            onProgress((b + 1).toFloat() / sizes.size)
        }
    }

    /** The keys, in order, and where the records start. */
    private suspend fun keys(): Pair<List<Key>, Int> {
        var pos = keysAt
        val blockCount = number(pos).toInt()
        pos += numberWidth
        pos += numberWidth // the number of entries
        val infoDecompressedSize = if (version >= 2.0) number(pos).toInt().also { pos += numberWidth } else 0
        val infoSize = number(pos).toInt()
        pos += numberWidth
        pos += numberWidth // the size of the key blocks
        if (version >= 2.0) pos += 4 // the checksum of the numbers above

        var info = bytes.copyOfRange(pos, pos + infoSize)
        pos += infoSize
        if (version >= 2.0) {
            if (encrypted and 2 != 0) info = decryptKeyInfo(info)
            info = inflateZlib(info, 8, info.size, infoDecompressedSize)
        }
        val blockSizes = keyBlockSizes(info, blockCount)

        val keys = ArrayList<Key>()
        for ((compressed, decompressed) in blockSizes) {
            val block = decompress(pos, compressed, decompressed)
            pos += compressed
            var i = 0
            while (i < block.size) {
                val offset = number(block, i)
                i += numberWidth
                val end = terminator(block, i)
                keys += Key(offset, decode(block, i, end).trim())
                i = end + if (utf16) 2 else 1
            }
        }
        return keys to pos
    }

    /** The compressed and decompressed size of each block of keys, from the index of keys. */
    private fun keyBlockSizes(info: ByteArray, count: Int): List<Pair<Int, Int>> {
        val sizeWidth = if (version >= 2.0) 2 else 1
        val terminator = if (version >= 2.0) 1 else 0
        val charWidth = if (utf16) 2 else 1
        var i = 0
        return List(count) {
            i += numberWidth // the block's number of entries
            repeat(2) { // its first and last keys
                val length = if (sizeWidth == 2) ((info[i].toInt() and 0xFF) shl 8) or (info[i + 1].toInt() and 0xFF) else info[i].toInt() and 0xFF
                i += sizeWidth + (length + terminator) * charWidth
            }
            val compressed = number(info, i).toInt()
            val decompressed = number(info, i + numberWidth).toInt()
            i += 2 * numberWidth
            compressed to decompressed
        }
    }

    /** A block at [at]: its compression (4 bytes), a checksum (4), and its data. */
    private suspend fun decompress(at: Int, compressedSize: Int, size: Int): ByteArray {
        if (at + compressedSize > bytes.size) throw MdictException("Damaged dictionary: a block is cut off")
        return when (val type = uint32le(at)) {
            0L -> bytes.copyOfRange(at + 8, at + compressedSize)
            1L -> Lzo.decompress(bytes.copyOfRange(at + 8, at + compressedSize), size)
            2L -> inflateZlib(bytes, at + 8, at + compressedSize, size)
            else -> throw MdictException("Damaged dictionary: unknown compression $type")
        }
    }

    /** zlib data between [from] and [to] of [data], inflated to [size] bytes: its two-byte header skipped, its checksum left unread. */
    private suspend fun inflateZlib(data: ByteArray, from: Int, to: Int, size: Int): ByteArray =
        inflateRaw(data.copyOfRange(from + 2, to), size)

    /** The index of keys revealed: each byte's halves swapped and mixed with the one before and a key the index's own checksum makes. */
    private fun decryptKeyInfo(info: ByteArray): ByteArray {
        val key = Ripemd128.digest(info.copyOfRange(4, 8) + byteArrayOf(0x95.toByte(), 0x36, 0, 0))
        val out = info.copyOf()
        var previous = 0x36
        for (i in 8 until info.size) {
            val b = info[i].toInt() and 0xFF
            val swapped = ((b ushr 4) or (b shl 4)) and 0xFF
            out[i] = (swapped xor previous xor ((i - 8) and 0xFF) xor (key[(i - 8) % key.size].toInt() and 0xFF)).toByte()
            previous = b
        }
        return out
    }

    /** A compact record with its style numbers turned back into the tags they stand for, as readmdict does. */
    private fun styled(record: String): String {
        if (styles.isEmpty() || '`' !in record) return record
        val out = StringBuilder()
        var last = 0
        var style: Pair<String, String>? = null
        for (mark in STYLE.findAll(record)) {
            val part = record.substring(last, mark.range.first)
            out.append(style?.let { it.first + part + it.second } ?: part)
            style = styles[mark.groupValues[1]]
            last = mark.range.last + 1
        }
        val part = record.substring(last)
        out.append(style?.let { it.first + part + it.second } ?: part)
        return out.toString()
    }

    /** Where the text starting at [from] ends: its terminating zero. */
    private fun terminator(block: ByteArray, from: Int): Int {
        var i = from
        if (utf16) {
            while (i + 1 < block.size && (block[i].toInt() != 0 || block[i + 1].toInt() != 0)) i += 2
        } else {
            while (i < block.size && block[i].toInt() != 0) i++
        }
        return i
    }

    /** A record's text, without the zero that ends it. */
    private fun text(block: ByteArray, from: Int, to: Int): String = decode(block, from, to).trimEnd('\u0000')

    private fun decode(data: ByteArray, from: Int, to: Int): String =
        if (utf16) utf16le(data, from, to - from) else TextDecoding.decode(data.copyOfRange(from, to), encoding)

    private fun utf16le(at: Int, length: Int): String = utf16le(bytes, at, length)

    private fun utf16le(data: ByteArray, at: Int, length: Int): String {
        val chars = CharArray(length / 2)
        for (i in chars.indices) chars[i] = ((data[at + 2 * i].toInt() and 0xFF) or ((data[at + 2 * i + 1].toInt() and 0xFF) shl 8)).toChar()
        return chars.concatToString()
    }

    private fun number(at: Int): Long = number(bytes, at)

    /** A big-endian number of [numberWidth] bytes. */
    private fun number(data: ByteArray, at: Int): Long {
        var n = 0L
        for (i in 0 until numberWidth) n = (n shl 8) or (data[at + i].toLong() and 0xFF)
        return n
    }

    private fun uint32(at: Int): Long = (0 until 4).fold(0L) { n, i -> (n shl 8) or (bytes[at + i].toLong() and 0xFF) }

    private fun uint32le(at: Int): Long = (3 downTo 0).fold(0L) { n, i -> (n shl 8) or (bytes[at + i].toLong() and 0xFF) }

    companion object {
        private val ATTRIBUTE = Regex("""(\w+)="([^"]*)"""")
        private val STYLE = Regex("""`(\d+)`""")

        /** Whether [bytes] start as an MDict file: a header's length, then "<Dictionary" or "<Library_Data" in UTF-16. */
        fun looksLike(bytes: ByteArray): Boolean {
            if (bytes.size < 32) return false
            val size = (0 until 4).fold(0L) { n, i -> (n shl 8) or (bytes[i].toLong() and 0xFF) }
            if (size <= 0 || size + 8 > bytes.size) return false
            val start = CharArray(5) { i -> ((bytes[4 + 2 * i].toInt() and 0xFF) or ((bytes[5 + 2 * i].toInt() and 0xFF) shl 8)).toChar() }.concatToString()
            return start == "<Dict" || start == "<Libr"
        }

        private fun unescape(text: String): String = text.replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">").replace("&#39;", "'").replace("&amp;", "&")
    }
}

class MdictException(message: String) : Exception(message)
