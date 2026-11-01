package com.tayra.languages.core.data.anki

/**
 * Decompresses Zstandard data (RFC 8878): newer Anki packages compress their collection and media
 * list with it, and not every platform has a zstd reader. Frames may be concatenated and skippable
 * frames are skipped; frames made with a dictionary are refused, and checksums are not checked.
 */
internal object Zstd {
    fun decompress(input: ByteArray): ByteArray = try {
        Decoder(input).run()
    } catch (_: IndexOutOfBoundsException) {
        throw ZstdException("Damaged zstd data")
    }
}

internal class ZstdException(message: String) : Exception(message)

/** An FSE decoding table: for each state, the symbol it decodes and how the next state is found. */
private class Fse(val log: Int, val symbol: IntArray, val bits: IntArray, val base: IntArray)

/** A Huffman decoding table indexed by the next [maxBits] bits: each entry is its symbol shl 8 or its code length. */
private class Huffman(val maxBits: Int, val entries: IntArray)

private class Decoder(private val input: ByteArray) {
    private var pos = 0
    private var out = ByteArray(maxOf(1 shl 16, minOf(input.size, 1 shl 26) * 4))
    private var size = 0
    private val literals = ByteArray(MAX_BLOCK)

    // The block being decoded takes its literals from here: the input itself when they are stored raw.
    private var litSource = literals
    private var litStart = 0
    private var litEnd = 0

    // What a frame's later blocks may repeat from earlier ones.
    private var frameStart = 0
    private var huffman: Huffman? = null
    private var literalLengths: Fse? = null
    private var offsets: Fse? = null
    private var matchLengths: Fse? = null
    private val repeats = IntArray(3)

    fun run(): ByteArray {
        while (pos < input.size) {
            val magic = bytes(pos, 4).toInt()
            pos += 4
            when {
                magic == FRAME_MAGIC -> frame()
                magic and -16 == SKIPPABLE_MAGIC -> {
                    val length = bytes(pos, 4)
                    pos += 4
                    if (length > input.size - pos) throw ZstdException("Truncated zstd data")
                    pos += length.toInt()
                }
                else -> throw ZstdException("Not zstd data")
            }
        }
        return out.copyOf(size)
    }

    private fun frame() {
        val descriptor = byte(pos++)
        if (descriptor and 0x08 != 0) throw ZstdException("Damaged zstd frame header")
        val singleSegment = descriptor and 0x20 != 0
        // The window size only bounds how far back matches reach, and the whole output stays in memory.
        if (!singleSegment) pos++
        val dictionaryBytes = when (descriptor and 3) {
            0 -> 0
            1 -> 1
            2 -> 2
            else -> 4
        }
        if (bytes(pos, dictionaryBytes) != 0L) throw ZstdException("zstd data made with a dictionary is not supported")
        pos += dictionaryBytes
        val sizeBytes = when (descriptor ushr 6) {
            0 -> if (singleSegment) 1 else 0
            1 -> 2
            2 -> 4
            else -> 8
        }
        var contentSize = -1L
        if (sizeBytes > 0) {
            contentSize = bytes(pos, sizeBytes) + if (sizeBytes == 2) 256 else 0
            pos += sizeBytes
            if (contentSize !in 0..(Int.MAX_VALUE - 8L - size)) throw ZstdException("zstd data too large to decompress")
            ensure(minOf(contentSize, MAX_PREALLOCATION).toInt())
        }

        frameStart = size
        huffman = null
        literalLengths = null
        offsets = null
        matchLengths = null
        repeats[0] = 1
        repeats[1] = 4
        repeats[2] = 8
        do {
            val header = bytes(pos, 3).toInt()
            pos += 3
            val blockSize = header ushr 3
            when ((header ushr 1) and 3) {
                RAW_BLOCK -> {
                    if (blockSize > input.size - pos) throw ZstdException("Truncated zstd data")
                    ensure(blockSize)
                    input.copyInto(out, size, pos, pos + blockSize)
                    size += blockSize
                    pos += blockSize
                }
                RLE_BLOCK -> {
                    val value = byte(pos++).toByte()
                    ensure(blockSize)
                    out.fill(value, size, size + blockSize)
                    size += blockSize
                }
                COMPRESSED_BLOCK -> {
                    if (blockSize > MAX_BLOCK) throw ZstdException("Damaged zstd block")
                    if (blockSize > input.size - pos) throw ZstdException("Truncated zstd data")
                    val end = pos + blockSize
                    readLiterals(end)
                    readSequences(end)
                    pos = end
                }
                else -> throw ZstdException("Damaged zstd block")
            }
        } while (header and 1 == 0)

        if (contentSize >= 0 && size - frameStart.toLong() != contentSize) throw ZstdException("Damaged zstd frame: wrong size")
        if (descriptor and 0x04 != 0) {
            if (input.size - pos < 4) throw ZstdException("Truncated zstd data")
            pos += 4
        }
    }

    private fun readLiterals(end: Int) {
        val first = byte(pos)
        val type = first and 3
        val sizeFormat = (first ushr 2) and 3
        if (type == RAW_LITERALS || type == RLE_LITERALS) {
            val count: Int
            when (sizeFormat) {
                1 -> {
                    count = (first ushr 4) + (byte(pos + 1) shl 4)
                    pos += 2
                }
                3 -> {
                    count = (first ushr 4) + (byte(pos + 1) shl 4) + (byte(pos + 2) shl 12)
                    pos += 3
                }
                else -> {
                    count = first ushr 3
                    pos += 1
                }
            }
            if (count > MAX_BLOCK) throw ZstdException("Damaged zstd literals")
            if (type == RAW_LITERALS) {
                if (count > end - pos) throw ZstdException("Damaged zstd literals")
                litSource = input
                litStart = pos
                pos += count
            } else {
                if (pos >= end) throw ZstdException("Damaged zstd literals")
                literals.fill(input[pos++], 0, count)
                litSource = literals
                litStart = 0
            }
            litEnd = litStart + count
            return
        }

        val headerSize = when (sizeFormat) {
            0, 1 -> 3
            2 -> 4
            else -> 5
        }
        val sizeBits = when (sizeFormat) {
            0, 1 -> 10
            2 -> 14
            else -> 18
        }
        val header = bytes(pos, headerSize)
        val mask = (1L shl sizeBits) - 1
        val count = ((header ushr 4) and mask).toInt()
        val compressedSize = ((header ushr (4 + sizeBits)) and mask).toInt()
        pos += headerSize
        if (count > MAX_BLOCK || compressedSize > end - pos) throw ZstdException("Damaged zstd literals")
        val streamsEnd = pos + compressedSize
        if (type == COMPRESSED_LITERALS) huffman = readHuffman(streamsEnd)
        val table = huffman ?: throw ZstdException("Damaged zstd literals: no Huffman table to repeat")
        if (sizeFormat == 0) {
            decodeLiterals(table, pos, streamsEnd, 0, count)
        } else {
            if (streamsEnd - pos < 6) throw ZstdException("Damaged zstd literals")
            val first = pos + 6
            val second = first + bytes(pos, 2).toInt()
            val third = second + bytes(pos + 2, 2).toInt()
            val fourth = third + bytes(pos + 4, 2).toInt()
            val quarter = (count + 3) / 4
            if (fourth > streamsEnd || quarter * 3 > count) throw ZstdException("Damaged zstd literals")
            decodeLiterals(table, first, second, 0, quarter)
            decodeLiterals(table, second, third, quarter, 2 * quarter)
            decodeLiterals(table, third, fourth, 2 * quarter, 3 * quarter)
            decodeLiterals(table, fourth, streamsEnd, 3 * quarter, count)
        }
        litSource = literals
        litStart = 0
        litEnd = count
        pos = streamsEnd
    }

    private fun readHuffman(end: Int): Huffman {
        val header = byte(pos++)
        val weights = IntArray(259)
        val count: Int
        if (header < 128) {
            val weightsEnd = pos + header
            if (weightsEnd > end) throw ZstdException("Damaged zstd Huffman table")
            val fse = readFse(255, 6, weightsEnd)
            count = decodeWeights(fse, pos, weightsEnd, weights)
            pos = weightsEnd
        } else {
            count = header - 127
            if ((count + 1) / 2 > end - pos) throw ZstdException("Damaged zstd Huffman table")
            for (i in 0 until count) {
                val value = input[pos + i / 2].toInt()
                weights[i] = if (i % 2 == 0) (value ushr 4) and 15 else value and 15
            }
            pos += (count + 1) / 2
        }

        if (count > 255) throw ZstdException("Damaged zstd Huffman table")
        // The last symbol's weight is implied: whatever brings the total up to a power of two.
        var total = 0
        for (i in 0 until count) {
            val weight = weights[i]
            if (weight > MAX_HUFFMAN_BITS) throw ZstdException("Damaged zstd Huffman table")
            if (weight > 0) total += 1 shl (weight - 1)
        }
        if (total == 0) throw ZstdException("Damaged zstd Huffman table")
        val maxBits = highBit(total) + 1
        val rest = (1 shl maxBits) - total
        if (maxBits > MAX_HUFFMAN_BITS || rest and (rest - 1) != 0) throw ZstdException("Damaged zstd Huffman table")
        weights[count] = highBit(rest) + 1
        val symbols = count + 1

        // Codes go out from the lightest symbols up, so each takes the next run of table entries.
        val entries = IntArray(1 shl maxBits)
        var at = 0
        for (weight in 1..maxBits) {
            for (symbol in 0 until symbols) {
                if (weights[symbol] != weight) continue
                val span = 1 shl (weight - 1)
                entries.fill((symbol shl 8) or (maxBits + 1 - weight), at, at + span)
                at += span
            }
        }
        return Huffman(maxBits, entries)
    }

    /** Decodes Huffman weights from two interleaved FSE states, which run until the bitstream is used up. */
    private fun decodeWeights(fse: Fse, start: Int, end: Int, weights: IntArray): Int {
        val reader = BackwardBits(input, start, end)
        var first = reader.read(fse.log)
        var second = reader.read(fse.log)
        var count = 0
        while (true) {
            if (count > 255) throw ZstdException("Damaged zstd Huffman table")
            weights[count++] = fse.symbol[first]
            reader.reload()
            first = fse.base[first] + reader.read(fse.bits[first])
            if (reader.remaining < 0) {
                weights[count++] = fse.symbol[second]
                return count
            }
            weights[count++] = fse.symbol[second]
            reader.reload()
            second = fse.base[second] + reader.read(fse.bits[second])
            if (reader.remaining < 0) {
                weights[count++] = fse.symbol[first]
                return count
            }
        }
    }

    private fun decodeLiterals(table: Huffman, start: Int, end: Int, from: Int, to: Int) {
        val reader = BackwardBits(input, start, end)
        val entries = table.entries
        val maxBits = table.maxBits
        for (i in from until to) {
            reader.reload()
            val entry = entries[reader.peek(maxBits)]
            literals[i] = (entry ushr 8).toByte()
            reader.skip(entry and 0xFF)
        }
        if (reader.remaining != 0) throw ZstdException("Damaged zstd literals")
    }

    private fun readSequences(end: Int) {
        if (pos >= end) throw ZstdException("Damaged zstd sequences")
        val first = byte(pos++)
        val count = when {
            first < 128 -> first
            first < 255 -> ((first - 128) shl 8) + byte(pos++)
            else -> (byte(pos) + (byte(pos + 1) shl 8) + 0x7F00).also { pos += 2 }
        }
        if (count == 0) {
            copyLiterals(litStart, litEnd - litStart)
            return
        }

        val modes = byte(pos++)
        if (modes and 3 != 0) throw ZstdException("Damaged zstd sequences")
        val ll = table(modes ushr 6, literalLengths, LL_MAX_SYMBOL, LL_MAX_LOG, LL_PREDEFINED, end)
        val of = table((modes ushr 4) and 3, offsets, OF_MAX_SYMBOL, OF_MAX_LOG, OF_PREDEFINED, end)
        val ml = table((modes ushr 2) and 3, matchLengths, ML_MAX_SYMBOL, ML_MAX_LOG, ML_PREDEFINED, end)
        literalLengths = ll
        offsets = of
        matchLengths = ml

        val reader = BackwardBits(input, pos, end)
        var llState = reader.read(ll.log)
        var ofState = reader.read(of.log)
        var mlState = reader.read(ml.log)
        var lit = litStart
        for (i in 1..count) {
            reader.reload()
            val ofCode = of.symbol[ofState]
            if (ofCode > 30) throw ZstdException("zstd data too large to decompress")
            var offset = (1 shl ofCode) + reader.read(ofCode)
            reader.reload()
            val mlCode = ml.symbol[mlState]
            val matchLength = ML_BASE[mlCode] + reader.read(ML_BITS[mlCode])
            val llCode = ll.symbol[llState]
            val literalLength = LL_BASE[llCode] + reader.read(LL_BITS[llCode])
            if (i < count) {
                reader.reload()
                llState = ll.base[llState] + reader.read(ll.bits[llState])
                mlState = ml.base[mlState] + reader.read(ml.bits[mlState])
                ofState = of.base[ofState] + reader.read(of.bits[ofState])
            }

            if (offset > 3) {
                offset -= 3
                repeats[2] = repeats[1]
                repeats[1] = repeats[0]
                repeats[0] = offset
            } else {
                // Without literals before the match, the first repeat would be pointless, so the codes shift by one.
                val index = if (literalLength == 0) offset else offset - 1
                if (index == 0) {
                    offset = repeats[0]
                } else {
                    offset = if (index == 3) repeats[0] - 1 else repeats[index]
                    if (index != 1) repeats[2] = repeats[1]
                    repeats[1] = repeats[0]
                    repeats[0] = offset
                }
            }

            if (literalLength > litEnd - lit) throw ZstdException("Damaged zstd sequences")
            copyLiterals(lit, literalLength)
            lit += literalLength
            if (offset <= 0 || offset > size - frameStart) throw ZstdException("Damaged zstd sequences")
            copyMatch(offset, matchLength)
        }
        if (reader.remaining != 0) throw ZstdException("Damaged zstd sequences")
        copyLiterals(lit, litEnd - lit)
    }

    private fun table(mode: Int, previous: Fse?, maxSymbol: Int, maxLog: Int, predefined: Fse, end: Int): Fse = when (mode) {
        PREDEFINED_MODE -> predefined
        RLE_MODE -> {
            if (pos >= end) throw ZstdException("Damaged zstd sequences")
            val symbol = byte(pos++)
            if (symbol > maxSymbol) throw ZstdException("Damaged zstd sequences")
            Fse(0, intArrayOf(symbol), intArrayOf(0), intArrayOf(0))
        }
        FSE_MODE -> readFse(maxSymbol, maxLog, end)
        else -> previous ?: throw ZstdException("Damaged zstd sequences: no table to repeat")
    }

    /** Reads an FSE table's description: its accuracy, then each symbol's share of the states. */
    private fun readFse(maxSymbol: Int, maxLog: Int, end: Int): Fse {
        val start = pos
        var bit = 4
        val log = peekBits(start, 0, 4, end) + 5
        if (log > maxLog) throw ZstdException("Damaged zstd table")
        val counts = IntArray(maxSymbol + 1)
        var remaining = (1 shl log) + 1
        var threshold = 1 shl log
        var nbBits = log + 1
        var symbol = 0
        var previousZero = false
        while (remaining > 1 && symbol <= maxSymbol) {
            if (previousZero) {
                // A zero share is followed by 2-bit counts of further zeros, 3 meaning more follow.
                var repeat: Int
                do {
                    repeat = peekBits(start, bit, 2, end)
                    bit += 2
                    symbol += repeat
                } while (repeat == 3)
                if (symbol > maxSymbol) throw ZstdException("Damaged zstd table")
            }
            val max = 2 * threshold - 1 - remaining
            var count = peekBits(start, bit, nbBits - 1, end)
            if (count < max) {
                bit += nbBits - 1
            } else {
                count = peekBits(start, bit, nbBits, end)
                if (count >= threshold) count -= max
                bit += nbBits
            }
            count--
            remaining -= if (count < 0) -count else count
            if (remaining < 1) throw ZstdException("Damaged zstd table")
            counts[symbol++] = count
            previousZero = count == 0
            while (remaining < threshold) {
                nbBits--
                threshold = threshold ushr 1
            }
        }
        if (remaining != 1) throw ZstdException("Damaged zstd table")
        pos = start + (bit + 7) / 8
        if (pos > end) throw ZstdException("Damaged zstd table")
        return buildFse(counts, symbol, log)
    }

    /** [n] bits (at most 16) from [bit] bits into the data at [start], read least significant first; bytes past [end] read as zeros. */
    private fun peekBits(start: Int, bit: Int, n: Int, end: Int): Int {
        val first = start + (bit ushr 3)
        var value = 0
        for (i in 0 until 3) {
            val at = first + i
            if (at < end) value = value or ((input[at].toInt() and 0xFF) shl (8 * i))
        }
        return (value ushr (bit and 7)) and ((1 shl n) - 1)
    }

    private fun copyLiterals(from: Int, length: Int) {
        ensure(length)
        litSource.copyInto(out, size, from, from + length)
        size += length
    }

    /** Copies [length] bytes from [offset] back; when they overlap what is being written, the copy repeats them. */
    private fun copyMatch(offset: Int, length: Int) {
        ensure(length)
        val from = size - offset
        var left = length
        while (left > 0) {
            val n = minOf(left, size - from)
            out.copyInto(out, size, from, from + n)
            size += n
            left -= n
        }
    }

    private fun ensure(extra: Int) {
        if (extra <= out.size - size) return
        if (extra > Int.MAX_VALUE - 8 - size) throw ZstdException("zstd data too large to decompress")
        val needed = size + extra
        out = out.copyOf(maxOf(needed, minOf(out.size.toLong() * 2, Int.MAX_VALUE - 8L).toInt()))
    }

    private fun byte(at: Int): Int {
        if (at >= input.size) throw ZstdException("Truncated zstd data")
        return input[at].toInt() and 0xFF
    }

    /** [n] bytes at [at] as a little-endian number. */
    private fun bytes(at: Int, n: Int): Long {
        if (n > input.size - at) throw ZstdException("Truncated zstd data")
        var value = 0L
        for (i in n - 1 downTo 0) value = (value shl 8) or (input[at + i].toLong() and 0xFF)
        return value
    }

    private companion object {
        const val FRAME_MAGIC = 0xFD2FB528.toInt()
        const val SKIPPABLE_MAGIC = 0x184D2A50
        const val MAX_BLOCK = 1 shl 17
        const val MAX_PREALLOCATION = 1L shl 28
        const val MAX_HUFFMAN_BITS = 11

        const val RAW_BLOCK = 0
        const val RLE_BLOCK = 1
        const val COMPRESSED_BLOCK = 2
        const val RAW_LITERALS = 0
        const val RLE_LITERALS = 1
        const val COMPRESSED_LITERALS = 2
        const val PREDEFINED_MODE = 0
        const val RLE_MODE = 1
        const val FSE_MODE = 2

        const val LL_MAX_SYMBOL = 35
        const val ML_MAX_SYMBOL = 52
        const val OF_MAX_SYMBOL = 31
        const val LL_MAX_LOG = 9
        const val ML_MAX_LOG = 9
        const val OF_MAX_LOG = 8

        val LL_BASE = IntArray(16) { it } + intArrayOf(
            16, 18, 20, 22, 24, 28, 32, 40, 48, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536,
        )
        val LL_BITS = IntArray(16) + intArrayOf(1, 1, 1, 1, 2, 2, 3, 3, 4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val ML_BASE = IntArray(32) { it + 3 } + intArrayOf(
            35, 37, 39, 41, 43, 47, 51, 59, 67, 83, 99, 131, 259, 515, 1027, 2051, 4099, 8195, 16387, 32771, 65539,
        )
        val ML_BITS = IntArray(32) + intArrayOf(1, 1, 1, 1, 2, 2, 3, 3, 4, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)

        val LL_PREDEFINED = buildFse(
            intArrayOf(
                4, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 2, 1, 1, 1, 1, 1,
                -1, -1, -1, -1,
            ),
            36,
            6,
        )
        val ML_PREDEFINED = buildFse(
            intArrayOf(
                1, 4, 3, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, -1, -1, -1, -1, -1,
            ),
            53,
            6,
        )
        val OF_PREDEFINED = buildFse(
            intArrayOf(1, 1, 1, 1, 1, 1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, -1, -1, -1),
            29,
            5,
        )
    }
}

/**
 * Builds the decoding table for [symbols] symbols with the given shares of 2^[log] states: symbols
 * with a share of -1 ("less than one") take one state each from the top, the rest are spread out.
 */
private fun buildFse(counts: IntArray, symbols: Int, log: Int): Fse {
    val tableSize = 1 shl log
    val symbol = IntArray(tableSize)
    val bits = IntArray(tableSize)
    val base = IntArray(tableSize)
    val next = IntArray(symbols)
    var high = tableSize - 1
    for (s in 0 until symbols) {
        if (counts[s] == -1) {
            symbol[high--] = s
            next[s] = 1
        } else {
            next[s] = counts[s]
        }
    }
    val step = (tableSize ushr 1) + (tableSize ushr 3) + 3
    var at = 0
    for (s in 0 until symbols) {
        repeat(counts[s]) {
            symbol[at] = s
            do at = (at + step) and (tableSize - 1) while (at > high)
        }
    }
    if (at != 0) throw ZstdException("Damaged zstd table")
    for (state in 0 until tableSize) {
        val n = next[symbol[state]]++
        val nb = log - highBit(n)
        bits[state] = nb
        base[state] = (n shl nb) - tableSize
    }
    return Fse(log, symbol, bits, base)
}

private fun highBit(value: Int): Int = 31 - value.countLeadingZeroBits()

/**
 * Reads a bitstream from its end backwards, the way zstd writes its entropy-coded streams: the last
 * byte's highest set bit marks where the stream starts, and bits read past its beginning are zeros.
 */
private class BackwardBits(private val data: ByteArray, private val start: Int, end: Int) {
    private var ptr: Int
    private var container: Long
    private var consumed: Int

    init {
        if (end <= start || data[end - 1].toInt() == 0) throw ZstdException("Damaged zstd bitstream")
        val padding = (data[end - 1].toInt() and 0xFF).countLeadingZeroBits() - 23
        if (end - start >= 8) {
            ptr = end - 8
            container = long(ptr)
            consumed = padding
        } else {
            ptr = start
            container = 0L
            for (i in start until end) container = container or ((data[i].toLong() and 0xFF) shl (8 * (i - start)))
            consumed = (8 - (end - start)) * 8 + padding
        }
    }

    /** Bits not read yet; negative once reading has gone past the stream's beginning. */
    val remaining: Int get() = (ptr - start) * 8 + 64 - consumed

    fun peek(n: Int): Int = if (consumed >= 64) 0 else ((container shl consumed) ushr (64 - n)).toInt()

    fun skip(n: Int) {
        consumed += n
    }

    fun read(n: Int): Int {
        if (n == 0) return 0
        val value = peek(n)
        consumed += n
        return value
    }

    /** Refills the container, after which at least 56 bits can be read unless the stream is nearly done. */
    fun reload() {
        if (consumed < 8 || ptr == start) return
        val back = minOf(consumed ushr 3, ptr - start)
        ptr -= back
        consumed -= back * 8
        container = long(ptr)
    }

    private fun long(at: Int): Long {
        var value = 0L
        for (i in 7 downTo 0) value = (value shl 8) or (data[at + i].toLong() and 0xFF)
        return value
    }
}
