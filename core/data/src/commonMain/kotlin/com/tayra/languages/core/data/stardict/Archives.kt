package com.tayra.languages.core.data.stardict

import com.tayra.languages.core.data.yomitan.ZipArchive
import com.tayra.languages.core.data.yomitan.inflateRaw

/** A file in an archive, by its name. */
internal class ArchiveFile(val name: String, val bytes: ByteArray)

/**
 * The files of a dictionary however they come: a zip, a tar, either gzip-compressed, or one gzip
 * file. xz and bzip2, which many StarDict archives use, cannot be read here, so they are refused
 * with what to do instead.
 */
internal object Archives {

    suspend fun files(bytes: ByteArray, name: String = ""): List<ArchiveFile> = when {
        isZip(bytes) -> ZipArchive(bytes).let { zip -> zip.entries.filter { !it.name.endsWith('/') }.map { ArchiveFile(it.name, zip.read(it)) } }
        isGzip(bytes) -> {
            val inner = gunzip(bytes)
            if (isTar(inner)) Tar.read(inner) else listOf(ArchiveFile(name.removeSuffix(".gz").removeSuffix(".dz"), inner))
        }
        isTar(bytes) -> Tar.read(bytes)
        isXz(bytes) || isBzip2(bytes) -> throw StardictException("This archive is compressed with ${if (isXz(bytes)) "xz" else "bzip2"}, which cannot be read here; extract it and choose its files")
        else -> listOf(ArchiveFile(name, bytes))
    }

    fun isZip(bytes: ByteArray): Boolean = bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

    fun isGzip(bytes: ByteArray): Boolean = bytes.size >= 18 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()

    fun isTar(bytes: ByteArray): Boolean = bytes.size >= 512 && bytes.copyOfRange(257, 262).decodeToString() == "ustar"

    fun isXz(bytes: ByteArray): Boolean = bytes.size >= 6 && bytes.copyOfRange(0, 6).contentEquals(byteArrayOf(0xFD.toByte(), '7'.code.toByte(), 'z'.code.toByte(), 'X'.code.toByte(), 'Z'.code.toByte(), 0))

    fun isBzip2(bytes: ByteArray): Boolean = bytes.size >= 3 && bytes[0] == 'B'.code.toByte() && bytes[1] == 'Z'.code.toByte() && bytes[2] == 'h'.code.toByte()

    /**
     * A gzip file decompressed: its header skipped (its extra field, name, comment and header
     * checksum when its flags say so) and its deflate data inflated to the size its trailer gives. A
     * dictzip file (.dict.dz) is a gzip file whose extra field indexes its chunks.
     */
    suspend fun gunzip(bytes: ByteArray): ByteArray {
        if (!isGzip(bytes)) throw StardictException("Not a gzip file")
        val flags = bytes[3].toInt()
        var i = 10
        if (flags and 4 != 0) i += 2 + short(bytes, i)
        if (flags and 8 != 0) { while (bytes[i].toInt() != 0) i++; i++ }
        if (flags and 16 != 0) { while (bytes[i].toInt() != 0) i++; i++ }
        if (flags and 2 != 0) i += 2
        // One member, as gzip and dictzip write them: its data runs to the trailer, whose last 4 bytes are its size.
        val end = bytes.size - 8
        return inflateRaw(bytes.copyOfRange(i, end), int(bytes, end + 4))
    }

    private fun short(bytes: ByteArray, at: Int): Int = (bytes[at].toInt() and 0xFF) or ((bytes[at + 1].toInt() and 0xFF) shl 8)

    private fun int(bytes: ByteArray, at: Int): Int = short(bytes, at) or (short(bytes, at + 2) shl 16)
}

/** Tar archives: 512-byte headers, each followed by its file in 512-byte blocks. */
internal object Tar {

    fun read(bytes: ByteArray): List<ArchiveFile> {
        val out = ArrayList<ArchiveFile>()
        var at = 0
        while (at + 512 <= bytes.size) {
            val name = field(bytes, at, 100)
            if (name.isEmpty()) break
            val size = field(bytes, at + 124, 12).trim().toLongOrNull(8)?.toInt() ?: throw StardictException("Damaged tar archive")
            val type = bytes[at + 156].toInt().toChar()
            val prefix = field(bytes, at + 345, 155)
            val start = at + 512
            if (type == '0' || type == '\u0000') {
                if (start + size > bytes.size) throw StardictException("Damaged tar archive: ${prefix + name} is cut off")
                out += ArchiveFile(if (prefix.isEmpty()) name else "$prefix/$name", bytes.copyOfRange(start, start + size))
            }
            at = start + (size + 511) / 512 * 512
        }
        return out
    }

    /** A tar archive of [files], so files chosen together travel as one. */
    fun write(files: List<ArchiveFile>): ByteArray {
        val blocks = ArrayList<ByteArray>()
        for (file in files) {
            val header = ByteArray(512)
            val name = file.name.substringAfterLast('/').encodeToByteArray()
            name.copyInto(header, 0, 0, minOf(100, name.size))
            "0000644".encodeToByteArray().copyInto(header, 100)
            "0000000".encodeToByteArray().copyInto(header, 108)
            "0000000".encodeToByteArray().copyInto(header, 116)
            file.bytes.size.toString(8).padStart(11, '0').encodeToByteArray().copyInto(header, 124)
            "00000000000".encodeToByteArray().copyInto(header, 136)
            header[156] = '0'.code.toByte()
            "ustar".encodeToByteArray().copyInto(header, 257)
            "00".encodeToByteArray().copyInto(header, 263)
            // The checksum counts its own field as spaces.
            for (i in 148 until 156) header[i] = ' '.code.toByte()
            val sum = header.sumOf { it.toInt() and 0xFF }
            sum.toString(8).padStart(6, '0').encodeToByteArray().copyInto(header, 148)
            header[154] = 0
            blocks += header
            blocks += file.bytes.copyOf((file.bytes.size + 511) / 512 * 512)
        }
        blocks += ByteArray(1024)
        val out = ByteArray(blocks.sumOf { it.size })
        var at = 0
        for (block in blocks) { block.copyInto(out, at); at += block.size }
        return out
    }

    private fun field(bytes: ByteArray, at: Int, length: Int): String {
        var end = at
        while (end < at + length && bytes[end].toInt() != 0) end++
        return bytes.copyOfRange(at, end).decodeToString().trim()
    }
}

class StardictException(message: String) : Exception(message)
