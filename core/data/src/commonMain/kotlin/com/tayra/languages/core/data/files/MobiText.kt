package com.tayra.languages.core.data.files

/**
 * The text of a DRM-free Mobipocket or Kindle book (mobi, azw, azw3): a Palm database whose first
 * record describes the book and whose next records hold its HTML, each compressed on its own with
 * PalmDOC's LZ77 and followed by trailing entries that are no text. A combined mobi/KF8 file is
 * read by its first (Mobipocket) part; a KF8-only azw3 has the same layout. Books compressed with
 * Huffman/CDIC, an old Amazon scheme, and DRM-protected books are refused.
 */
internal object MobiText {

    fun read(bytes: ByteArray): ExtractedText {
        if (bytes.size < 86 || latin1(bytes.copyOfRange(60, 68)).let { it != "BOOKMOBI" && it != "TEXtREAd" }) {
            throw FileImportException("Could not read the book: it is not a valid Mobipocket or Kindle file")
        }
        val recordCount = u16(bytes, 76)
        val offsets = IntArray(recordCount) { u32(bytes, 78 + 8 * it) }
        fun record(index: Int): ByteArray {
            val start = offsets[index]
            val end = if (index + 1 < recordCount) offsets[index + 1] else bytes.size
            if (start < 0 || start > end || end > bytes.size) throw FileImportException("Could not read the book: the file is damaged")
            return bytes.copyOfRange(start, end)
        }
        val header = record(0)
        val compression = u16(header, 0)
        val textLength = u32(header, 4)
        val textRecords = u16(header, 8)
        val encryption = u16(header, 12)
        if (encryption != 0) throw FileImportException("This book is protected by DRM and cannot be imported")
        if (compression == HUFF_CDIC) throw FileImportException("This book uses a compression Tayra cannot read; convert it to EPUB first")
        if (compression != NONE && compression != PALMDOC) throw FileImportException("Could not read the book: unknown compression")

        val mobi = header.size >= 24 && latin1(header.copyOfRange(16, 20)) == "MOBI"
        val mobiHeaderLength = if (mobi) u32(header, 20) else 0
        val encoding = if (mobi && u32(header, 28) == 65001) "utf-8" else "windows-1252"
        val trailingFlags = if (mobi && mobiHeaderLength >= 0xE4 && header.size >= 0xF4) u16(header, 0xF2) else 0

        val text = ArrayList<Byte>(textLength.coerceIn(0, 64 * 1024 * 1024))
        for (index in 1..minOf(textRecords, recordCount - 1)) {
            val data = record(index)
            val size = data.size - trailingSize(data, trailingFlags)
            if (size <= 0) continue
            val content = data.copyOfRange(0, size)
            text.addAll((if (compression == PALMDOC) palmDocInflate(content) else content).asList())
        }
        val html = TextDecoding.decode(text.toByteArray().let { if (textLength in 1 until it.size) it.copyOfRange(0, textLength) else it }, encoding)
        val body = HtmlText.text(html)
        if (body.isEmpty()) throw FileImportException("Could not read the book: it has no text")

        val title = if (mobi && header.size >= 92) {
            val at = u32(header, 84)
            val length = u32(header, 88)
            if (at > 0 && length > 0 && at + length <= header.size) TextDecoding.decode(header.copyOfRange(at, at + length), encoding).trim().takeIf { it.isNotEmpty() } else null
        } else {
            latin1(bytes.copyOfRange(0, 32)).substringBefore('\u0000').replace('_', ' ').trim().takeIf { it.isNotEmpty() }
        }
        return ExtractedText(body, title)
    }

    /**
     * How many bytes at the end of a text record are trailing entries: one per flag bit above the
     * lowest, each ending with its size written backwards in 7-bit groups, and, with the lowest bit,
     * the bytes of a multibyte character that runs into the next record.
     */
    internal fun trailingSize(data: ByteArray, flags: Int): Int {
        var size = 0
        var bits = flags shr 1
        while (bits != 0) {
            if (bits and 1 != 0) size += entrySize(data, data.size - size)
            bits = bits shr 1
        }
        if (flags and 1 != 0 && data.size - size - 1 >= 0) size += (data[data.size - size - 1].toInt() and 0x3) + 1
        return size.coerceAtMost(data.size)
    }

    private fun entrySize(data: ByteArray, end: Int): Int {
        var result = 0
        var shift = 0
        var at = end
        while (at > 0) {
            val v = data[at - 1].toInt() and 0xFF
            result = result or ((v and 0x7F) shl shift)
            shift += 7
            at--
            if (v and 0x80 != 0 || shift >= 28) break
        }
        return result
    }

    /** PalmDOC's LZ77: literals, runs of literals, back-references of 3 to 10 bytes, and space-prefixed letters. */
    internal fun palmDocInflate(data: ByteArray): ByteArray {
        val out = ArrayList<Byte>(data.size * 2)
        var i = 0
        while (i < data.size) {
            val c = data[i++].toInt() and 0xFF
            when {
                c == 0 || c in 0x09..0x7F -> out += c.toByte()
                c in 0x01..0x08 -> repeat(c) { if (i < data.size) out += data[i++] }
                c in 0x80..0xBF -> {
                    if (i >= data.size) break
                    val pair = (c shl 8) or (data[i++].toInt() and 0xFF)
                    val distance = (pair shr 3) and 0x7FF
                    val length = (pair and 0x7) + 3
                    if (distance == 0 || distance > out.size) continue
                    val from = out.size - distance
                    for (k in 0 until length) out += out[from + k]
                }
                else -> {
                    out += ' '.code.toByte()
                    out += (c xor 0x80).toByte()
                }
            }
        }
        return out.toByteArray()
    }

    private fun u16(b: ByteArray, at: Int): Int = ((b[at].toInt() and 0xFF) shl 8) or (b[at + 1].toInt() and 0xFF)

    private fun u32(b: ByteArray, at: Int): Int = (u16(b, at) shl 16) or u16(b, at + 2)

    private const val NONE = 1
    private const val PALMDOC = 2
    private const val HUFF_CDIC = 17480
}
