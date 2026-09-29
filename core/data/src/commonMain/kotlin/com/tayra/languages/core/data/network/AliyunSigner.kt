package com.tayra.languages.core.data.network

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * The Alibaba Cloud RPC request signature: parameters are sorted and percent-encoded into a
 * canonical query string, wrapped as `METHOD&%2F&<query>` and signed with HMAC-SHA1 using the
 * AccessKey Secret plus a trailing `&`. SHA-1 is implemented here so no platform crypto is needed.
 */
object AliyunSigner {

    /** Returns [params] plus the `Signature` entry. */
    fun sign(method: String, params: Map<String, String>, accessKeySecret: String): Map<String, String> {
        val canonical = params.entries.sortedBy { it.key }.joinToString("&") { (k, v) -> "${percentEncode(k)}=${percentEncode(v)}" }
        val stringToSign = "$method&${percentEncode("/")}&${percentEncode(canonical)}"
        val signature = base64(hmacSha1(("$accessKeySecret&").encodeToByteArray(), stringToSign.encodeToByteArray()))
        return params + ("Signature" to signature)
    }

    /** RFC 3986 encoding as Alibaba expects it: `~` stays, space is `%20`, `*` is `%2A`. */
    fun percentEncode(value: String): String {
        val sb = StringBuilder()
        for (byte in value.encodeToByteArray()) {
            val c = byte.toInt() and 0xFF
            val ch = c.toChar()
            if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch == '-' || ch == '_' || ch == '.' || ch == '~') sb.append(ch)
            else sb.append('%').append(HEX[c shr 4]).append(HEX[c and 0xF])
        }
        return sb.toString()
    }

    fun hmacSha1(key: ByteArray, message: ByteArray): ByteArray {
        val k = if (key.size > 64) sha1(key) else key
        val padded = ByteArray(64).also { k.copyInto(it) }
        val inner = ByteArray(64) { (padded[it].toInt() xor 0x36).toByte() }
        val outer = ByteArray(64) { (padded[it].toInt() xor 0x5c).toByte() }
        return sha1(outer + sha1(inner + message))
    }

    fun sha1(message: ByteArray): ByteArray {
        val bitLength = message.size.toLong() * 8
        val padLength = ((56 - (message.size + 1) % 64) + 64) % 64
        val padded = ByteArray(message.size + 1 + padLength + 8)
        message.copyInto(padded)
        padded[message.size] = 0x80.toByte()
        for (i in 0 until 8) padded[padded.size - 1 - i] = (bitLength ushr (8 * i)).toByte()

        var h0 = 0x67452301; var h1 = 0xEFCDAB89.toInt(); var h2 = 0x98BADCFE.toInt(); var h3 = 0x10325476; var h4 = 0xC3D2E1F0.toInt()
        val w = IntArray(80)
        for (chunk in padded.indices step 64) {
            for (i in 0 until 16) {
                w[i] = ((padded[chunk + 4 * i].toInt() and 0xFF) shl 24) or ((padded[chunk + 4 * i + 1].toInt() and 0xFF) shl 16) or
                    ((padded[chunk + 4 * i + 2].toInt() and 0xFF) shl 8) or (padded[chunk + 4 * i + 3].toInt() and 0xFF)
            }
            for (i in 16 until 80) w[i] = (w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]).rotateLeft(1)
            var a = h0; var b = h1; var c = h2; var d = h3; var e = h4
            for (i in 0 until 80) {
                val (f, k) = when {
                    i < 20 -> ((b and c) or (b.inv() and d)) to 0x5A827999
                    i < 40 -> (b xor c xor d) to 0x6ED9EBA1
                    i < 60 -> ((b and c) or (b and d) or (c and d)) to 0x8F1BBCDC.toInt()
                    else -> (b xor c xor d) to 0xCA62C1D6.toInt()
                }
                val temp = a.rotateLeft(5) + f + e + k + w[i]
                e = d; d = c; c = b.rotateLeft(30); b = a; a = temp
            }
            h0 += a; h1 += b; h2 += c; h3 += d; h4 += e
        }
        return byteArrayOf(h0, h1, h2, h3, h4)
    }

    private fun byteArrayOf(vararg words: Int): ByteArray {
        val out = ByteArray(words.size * 4)
        words.forEachIndexed { i, word -> for (j in 0 until 4) out[4 * i + j] = (word ushr (24 - 8 * j)).toByte() }
        return out
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun base64(bytes: ByteArray): String = Base64.encode(bytes)

    private const val HEX = "0123456789ABCDEF"
}
