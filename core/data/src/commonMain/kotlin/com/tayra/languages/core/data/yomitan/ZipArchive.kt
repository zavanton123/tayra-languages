package com.tayra.languages.core.data.yomitan

/** Inflates raw deflate data (a zip entry's) that inflates to [size] bytes. */
internal expect suspend fun inflateRaw(data: ByteArray, size: Int): ByteArray

/**
 * Reads the entries of a zip archive, stored or deflated, from its central directory: Yomitan
 * dictionaries come as zips, and not every platform has a zip reader. Archives over 4 GB (zip64)
 * and encrypted entries are refused.
 */
internal class ZipArchive(private val bytes: ByteArray) {
    class Entry internal constructor(
        val name: String,
        val size: Int,
        internal val method: Int,
        internal val compressedSize: Int,
        internal val localOffset: Int,
    )

    val entries: List<Entry> = readCentralDirectory()

    fun entry(name: String): Entry? = entries.firstOrNull { it.name == name }

    suspend fun read(entry: Entry): ByteArray {
        val offset = entry.localOffset
        if (int(offset) != LOCAL_HEADER) throw ZipException("Damaged zip: no entry at $offset")
        val start = offset + 30 + short(offset + 26) + short(offset + 28)
        if (start + entry.compressedSize > bytes.size) throw ZipException("Damaged zip: ${entry.name} is cut off")
        val data = bytes.copyOfRange(start, start + entry.compressedSize)
        return when (entry.method) {
            STORED -> data
            DEFLATED -> inflateRaw(data, entry.size)
            else -> throw ZipException("${entry.name} is compressed in a way this reader does not know (method ${entry.method})")
        }
    }

    suspend fun readText(name: String): String? = entry(name)?.let { read(it).decodeToString() }

    private fun readCentralDirectory(): List<Entry> {
        val end = (bytes.size - 22 downTo maxOf(0, bytes.size - 22 - 0xFFFF)).firstOrNull { int(it) == END_OF_DIRECTORY }
            ?: throw ZipException("Not a zip file")
        val count = short(end + 10)
        val directory = int(end + 16)
        if (count == 0xFFFF || directory == -1) throw ZipException("Zip archives over 4 GB are not supported")
        val out = ArrayList<Entry>(count)
        var at = directory
        repeat(count) {
            if (int(at) != DIRECTORY_ENTRY) throw ZipException("Damaged zip: bad central directory")
            if (short(at + 8) and 1 != 0) throw ZipException("Encrypted zip archives are not supported")
            val nameLength = short(at + 28)
            val name = bytes.copyOfRange(at + 46, at + 46 + nameLength).decodeToString()
            out += Entry(name, int(at + 24), short(at + 10), int(at + 20), int(at + 42))
            at += 46 + nameLength + short(at + 30) + short(at + 32)
        }
        return out
    }

    private fun short(at: Int): Int = (bytes[at].toInt() and 0xFF) or ((bytes[at + 1].toInt() and 0xFF) shl 8)

    private fun int(at: Int): Int = short(at) or (short(at + 2) shl 16)

    private companion object {
        const val LOCAL_HEADER = 0x04034b50
        const val DIRECTORY_ENTRY = 0x02014b50
        const val END_OF_DIRECTORY = 0x06054b50
        const val STORED = 0
        const val DEFLATED = 8
    }
}

class ZipException(message: String) : Exception(message)
