package com.tayra.languages.core.data.mdict

/** RIPEMD-128, which MDict uses to make the key that hides the index of its keys. */
internal object Ripemd128 {

    fun digest(message: ByteArray): ByteArray {
        // Padding: a 1 bit, zeros, and the length in bits, little-endian, to a multiple of 64 bytes.
        val length = message.size
        val padded = ByteArray(((length + 8) / 64 + 1) * 64)
        message.copyInto(padded)
        padded[length] = 0x80.toByte()
        val bits = length.toLong() * 8
        for (i in 0 until 8) padded[padded.size - 8 + i] = (bits ushr (8 * i)).toByte()

        var h0 = 0x67452301
        var h1 = 0xEFCDAB89.toInt()
        var h2 = 0x98BADCFE.toInt()
        var h3 = 0x10325476
        val x = IntArray(16)
        for (block in 0 until padded.size / 64) {
            for (i in 0 until 16) {
                val at = block * 64 + i * 4
                x[i] = (padded[at].toInt() and 0xFF) or ((padded[at + 1].toInt() and 0xFF) shl 8) or
                    ((padded[at + 2].toInt() and 0xFF) shl 16) or ((padded[at + 3].toInt() and 0xFF) shl 24)
            }
            var al = h0; var bl = h1; var cl = h2; var dl = h3
            var ar = h0; var br = h1; var cr = h2; var dr = h3
            for (j in 0 until 64) {
                val round = j / 16
                var t = al + f(round, bl, cl, dl) + x[R[j]] + KL[round]
                t = t.rotateLeft(S[j])
                al = dl; dl = cl; cl = bl; bl = t
                t = ar + f(3 - round, br, cr, dr) + x[RP[j]] + KR[round]
                t = t.rotateLeft(SP[j])
                ar = dr; dr = cr; cr = br; br = t
            }
            val t = h1 + cl + dr
            h1 = h2 + dl + ar
            h2 = h3 + al + br
            h3 = h0 + bl + cr
            h0 = t
        }
        val out = ByteArray(16)
        intArrayOf(h0, h1, h2, h3).forEachIndexed { i, h -> for (b in 0 until 4) out[i * 4 + b] = (h ushr (8 * b)).toByte() }
        return out
    }

    private fun f(round: Int, x: Int, y: Int, z: Int): Int = when (round) {
        0 -> x xor y xor z
        1 -> (x and y) or (x.inv() and z)
        2 -> (x or y.inv()) xor z
        else -> (x and z) or (y and z.inv())
    }

    private val KL = intArrayOf(0x00000000, 0x5A827999, 0x6ED9EBA1, 0x8F1BBCDC.toInt())
    private val KR = intArrayOf(0x50A28BE6, 0x5C4DD124, 0x6D703EF3, 0x00000000)

    private val R = intArrayOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        7, 4, 13, 1, 10, 6, 15, 3, 12, 0, 9, 5, 2, 14, 11, 8,
        3, 10, 14, 4, 9, 15, 8, 1, 2, 7, 0, 6, 13, 11, 5, 12,
        1, 9, 11, 10, 0, 8, 12, 4, 13, 3, 7, 15, 14, 5, 6, 2,
    )
    private val RP = intArrayOf(
        5, 14, 7, 0, 9, 2, 11, 4, 13, 6, 15, 8, 1, 10, 3, 12,
        6, 11, 3, 7, 0, 13, 5, 10, 14, 15, 8, 12, 4, 9, 1, 2,
        15, 5, 1, 3, 7, 14, 6, 9, 11, 8, 12, 2, 10, 0, 4, 13,
        8, 6, 4, 1, 3, 11, 15, 0, 5, 12, 2, 13, 9, 7, 10, 14,
    )
    private val S = intArrayOf(
        11, 14, 15, 12, 5, 8, 7, 9, 11, 13, 14, 15, 6, 7, 9, 8,
        7, 6, 8, 13, 11, 9, 7, 15, 7, 12, 15, 9, 11, 7, 13, 12,
        11, 13, 6, 7, 14, 9, 13, 15, 14, 8, 13, 6, 5, 12, 7, 5,
        11, 12, 14, 15, 14, 15, 9, 8, 9, 14, 5, 6, 8, 6, 5, 12,
    )
    private val SP = intArrayOf(
        8, 9, 9, 11, 13, 15, 15, 5, 7, 7, 8, 11, 14, 14, 12, 6,
        9, 13, 15, 7, 12, 8, 9, 11, 7, 7, 12, 7, 6, 15, 13, 11,
        9, 7, 15, 11, 8, 6, 6, 14, 12, 13, 5, 14, 13, 13, 7, 5,
        15, 5, 8, 11, 14, 14, 6, 14, 6, 9, 12, 9, 12, 5, 15, 8,
    )
}
