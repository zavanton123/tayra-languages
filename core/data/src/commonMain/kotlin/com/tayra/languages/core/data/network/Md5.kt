package com.tayra.languages.core.data.network

/** MD5 in plain Kotlin, for request signatures that still demand it (Baidu). Not for anything security-sensitive. */
object Md5 {
    fun hex(message: ByteArray): String = digest(message).joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    fun digest(message: ByteArray): ByteArray {
        val bitLength = message.size.toLong() * 8
        val padLength = ((56 - (message.size + 1) % 64) + 64) % 64
        val padded = ByteArray(message.size + 1 + padLength + 8)
        message.copyInto(padded)
        padded[message.size] = 0x80.toByte()
        for (i in 0 until 8) padded[message.size + 1 + padLength + i] = (bitLength ushr (8 * i)).toByte()

        var a0 = 0x67452301; var b0 = 0xefcdab89.toInt(); var c0 = 0x98badcfe.toInt(); var d0 = 0x10325476
        val m = IntArray(16)
        for (chunk in padded.indices step 64) {
            for (i in 0 until 16) {
                m[i] = (padded[chunk + 4 * i].toInt() and 0xFF) or ((padded[chunk + 4 * i + 1].toInt() and 0xFF) shl 8) or
                    ((padded[chunk + 4 * i + 2].toInt() and 0xFF) shl 16) or ((padded[chunk + 4 * i + 3].toInt() and 0xFF) shl 24)
            }
            var a = a0; var b = b0; var c = c0; var d = d0
            for (i in 0 until 64) {
                val (f, g) = when {
                    i < 16 -> ((b and c) or (b.inv() and d)) to i
                    i < 32 -> ((d and b) or (d.inv() and c)) to (5 * i + 1) % 16
                    i < 48 -> (b xor c xor d) to (3 * i + 5) % 16
                    else -> (c xor (b or d.inv())) to (7 * i) % 16
                }
                val rotated = (a + f + K[i] + m[g]).rotateLeft(S[i])
                a = d; d = c; c = b; b += rotated
            }
            a0 += a; b0 += b; c0 += c; d0 += d
        }
        val out = ByteArray(16)
        intArrayOf(a0, b0, c0, d0).forEachIndexed { i, word -> for (j in 0 until 4) out[4 * i + j] = (word ushr (8 * j)).toByte() }
        return out
    }

    private val S = intArrayOf(
        7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
        5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
        4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
        6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
    )

    private val K = IntArray(64) { i -> (kotlin.math.abs(kotlin.math.sin((i + 1).toDouble())) * 4294967296.0).toLong().toInt() }
}
