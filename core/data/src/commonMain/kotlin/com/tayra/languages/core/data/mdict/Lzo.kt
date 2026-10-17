package com.tayra.languages.core.data.mdict

/** LZO1X decompression, which older MDict files compress their blocks with; the reference decoder's steps, bounds-checked. */
internal object Lzo {

    /** [data] decompressed to [size] bytes. */
    fun decompress(data: ByteArray, size: Int): ByteArray {
        val out = ByteArray(size)
        var ip = 0
        var op = 0

        fun byte(at: Int): Int {
            if (at >= data.size) throw MdictException("Damaged dictionary: an LZO block ends early")
            return data[at].toInt() and 0xFF
        }

        fun literals(count: Int) {
            if (op + count > size || ip + count > data.size) throw MdictException("Damaged dictionary: an LZO block overruns")
            data.copyInto(out, op, ip, ip + count)
            ip += count
            op += count
        }

        fun copy(from: Int, count: Int) {
            if (from < 0 || op + count > size) throw MdictException("Damaged dictionary: an LZO match overruns")
            // Byte by byte: a match may overlap the bytes it is copying.
            for (i in 0 until count) out[op + i] = out[from + i]
            op += count
        }

        /** A run length past its field's own bits: each zero byte adds 255, the next byte ends it. */
        fun longRun(base: Int): Int {
            var t = 0
            while (byte(ip) == 0) { t += 255; ip++ }
            return t + base + byte(ip++)
        }

        var t = 0
        // The states of the reference decoder: a literal run, the match after a first run, a match, more literals.
        var state: Int
        if (byte(ip) > 17) {
            t = byte(ip++) - 17
            if (t < 4) {
                literals(t)
                t = byte(ip++)
                state = MATCH
            } else {
                literals(t)
                state = FIRST_LITERAL_RUN
            }
        } else {
            state = LOOP
        }
        while (true) {
            when (state) {
                LOOP -> {
                    t = byte(ip++)
                    if (t >= 16) { state = MATCH; continue }
                    if (t == 0) t = longRun(15)
                    literals(t + 3)
                    state = FIRST_LITERAL_RUN
                }
                FIRST_LITERAL_RUN -> {
                    t = byte(ip++)
                    if (t >= 16) { state = MATCH; continue }
                    val from = op - (1 + 0x0800) - (t shr 2) - (byte(ip++) shl 2)
                    copy(from, 3)
                    state = MATCH_DONE
                }
                MATCH -> {
                    when {
                        t >= 64 -> {
                            val from = op - 1 - ((t shr 2) and 7) - (byte(ip++) shl 3)
                            copy(from, (t shr 5) - 1 + 2)
                        }
                        t >= 32 -> {
                            t = t and 31
                            if (t == 0) t = longRun(31)
                            val from = op - 1 - ((byte(ip) shr 2) + (byte(ip + 1) shl 6))
                            ip += 2
                            copy(from, t + 2)
                        }
                        t >= 16 -> {
                            var from = op - ((t and 8) shl 11)
                            t = t and 7
                            if (t == 0) t = longRun(7)
                            from -= (byte(ip) shr 2) + (byte(ip + 1) shl 6)
                            ip += 2
                            if (from == op) {
                                if (op != size) throw MdictException("Damaged dictionary: an LZO block is shorter than it says")
                                return out
                            }
                            copy(from - 0x4000, t + 2)
                        }
                        else -> {
                            val from = op - 1 - (t shr 2) - (byte(ip++) shl 2)
                            copy(from, 2)
                        }
                    }
                    state = MATCH_DONE
                }
                MATCH_DONE -> {
                    t = byte(ip - 2) and 3
                    if (t == 0) { state = LOOP; continue }
                    literals(t)
                    t = byte(ip++)
                    state = MATCH
                }
            }
        }
    }

    private const val LOOP = 0
    private const val FIRST_LITERAL_RUN = 1
    private const val MATCH = 2
    private const val MATCH_DONE = 3
}
