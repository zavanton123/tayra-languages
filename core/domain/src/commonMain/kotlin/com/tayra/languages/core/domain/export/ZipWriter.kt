package com.tayra.languages.core.domain.export

import kotlinx.datetime.LocalDateTime

/**
 * Writes a zip archive with its entries stored as they are, which every reader accepts; it is
 * here because not every platform offers a zip writer. Entry names are UTF-8.
 */
object ZipWriter {
    fun write(entries: List<Pair<String, ByteArray>>, modified: LocalDateTime): ByteArray {
        val time = (modified.hour shl 11) or (modified.minute shl 5) or (modified.second / 2)
        val date = ((modified.year - 1980).coerceAtLeast(0) shl 9) or ((modified.month.ordinal + 1) shl 5) or modified.day
        val out = Bytes()
        val central = Bytes()
        for ((name, data) in entries) {
            val nameBytes = name.encodeToByteArray()
            val crc = Crc32.of(data)
            val offset = out.size
            out.int(0x04034b50).short(20).short(0x0800).short(0).short(time).short(date).int(crc).int(data.size).int(data.size).short(nameBytes.size).short(0)
            out.bytes(nameBytes).bytes(data)
            central.int(0x02014b50).short(20).short(20).short(0x0800).short(0).short(time).short(date).int(crc).int(data.size).int(data.size)
            central.short(nameBytes.size).short(0).short(0).short(0).short(0).int(0).int(offset).bytes(nameBytes)
        }
        val centralOffset = out.size
        out.bytes(central.toByteArray())
        out.int(0x06054b50).short(0).short(0).short(entries.size).short(entries.size).int(central.size).int(centralOffset).short(0)
        return out.toByteArray()
    }

    /** A growing little-endian byte buffer. */
    private class Bytes {
        private var buffer = ByteArray(1024)
        var size = 0
            private set

        fun bytes(data: ByteArray): Bytes {
            if (size + data.size > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, size + data.size))
            data.copyInto(buffer, size)
            size += data.size
            return this
        }

        fun short(value: Int): Bytes = bytes(byteArrayOf(value.toByte(), (value shr 8).toByte()))

        fun int(value: Int): Bytes = bytes(byteArrayOf(value.toByte(), (value shr 8).toByte(), (value shr 16).toByte(), (value shr 24).toByte()))

        fun toByteArray(): ByteArray = buffer.copyOf(size)
    }
}

/** CRC-32 as zip files use it. */
object Crc32 {
    private val table = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) 0xEDB88320.toInt() xor (c ushr 1) else c ushr 1 }
        c
    }

    fun of(data: ByteArray): Int {
        var crc = -1
        for (b in data) crc = table[(crc xor b.toInt()) and 0xff] xor (crc ushr 8)
        return crc.inv()
    }
}
