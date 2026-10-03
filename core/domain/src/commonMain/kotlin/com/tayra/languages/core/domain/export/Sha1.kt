package com.tayra.languages.core.domain.export

/** SHA-1, as Anki checksums note fields with it; written out since the platforms share no digest. */
object Sha1 {
    fun hex(input: ByteArray): String {
        val bits = input.size.toLong() * 8
        val zeros = (56 - (input.size + 1) % 64 + 64) % 64
        val padded = input + byteArrayOf(0x80.toByte()) + ByteArray(zeros) + ByteArray(8) { i -> (bits ushr (56 - 8 * i)).toByte() }
        var h0 = 0x67452301
        var h1 = 0xEFCDAB89.toInt()
        var h2 = 0x98BADCFE.toInt()
        var h3 = 0x10325476
        var h4 = 0xC3D2E1F0.toInt()
        val w = IntArray(80)
        for (chunk in padded.indices step 64) {
            for (i in 0 until 16) {
                val p = chunk + 4 * i
                w[i] = ((padded[p].toInt() and 0xff) shl 24) or ((padded[p + 1].toInt() and 0xff) shl 16) or ((padded[p + 2].toInt() and 0xff) shl 8) or (padded[p + 3].toInt() and 0xff)
            }
            for (i in 16 until 80) w[i] = (w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]).rotateLeft(1)
            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4
            for (i in 0 until 80) {
                val f: Int
                val k: Int
                when {
                    i < 20 -> { f = (b and c) or (b.inv() and d); k = 0x5A827999 }
                    i < 40 -> { f = b xor c xor d; k = 0x6ED9EBA1 }
                    i < 60 -> { f = (b and c) or (b and d) or (c and d); k = 0x8F1BBCDC.toInt() }
                    else -> { f = b xor c xor d; k = 0xCA62C1D6.toInt() }
                }
                val t = a.rotateLeft(5) + f + e + k + w[i]
                e = d
                d = c
                c = b.rotateLeft(30)
                b = a
                a = t
            }
            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
        }
        return listOf(h0, h1, h2, h3, h4).joinToString("") { it.toUInt().toString(16).padStart(8, '0') }
    }
}
