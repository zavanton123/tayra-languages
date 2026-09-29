package com.tayra.languages.core.data.network

import kotlin.test.Test
import kotlin.test.assertEquals

class AliyunSignerTest {
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    @Test
    fun sha1MatchesKnownVectors() {
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hex(AliyunSigner.sha1(ByteArray(0))))
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", hex(AliyunSigner.sha1("abc".encodeToByteArray())))
        assertEquals("84983e441c3bd26ebaae4aa1f95129e5e54670f1", hex(AliyunSigner.sha1("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray())))
    }

    @Test
    fun hmacSha1MatchesKnownVectors() {
        assertEquals("de7c9b85b8b78aa6bc8a7a36f70a90701c9db4d9", hex(AliyunSigner.hmacSha1("key".encodeToByteArray(), "The quick brown fox jumps over the lazy dog".encodeToByteArray())))
        assertEquals("fbdb1d1b18aa6c08324b7d64b71fb76370690e1d", hex(AliyunSigner.hmacSha1(ByteArray(0), ByteArray(0))))
    }

    @Test
    fun percentEncodesLikeAlibaba() {
        assertEquals("a%20b%2Ac~d-_.", AliyunSigner.percentEncode("a b*c~d-_."))
        assertEquals("%E4%BD%A0%E5%A5%BD", AliyunSigner.percentEncode("\u4f60\u597d"))
    }

    @Test
    fun signatureMatchesTheDocumentedExample() {
        // From the Alibaba Cloud "RPC API signature" documentation: the ECS DescribeRegions sample.
        val params = mapOf(
            "AccessKeyId" to "testid", "Action" to "DescribeRegions", "Format" to "XML", "SignatureMethod" to "HMAC-SHA1",
            "SignatureNonce" to "3ee8c1b8-83d3-44af-a94f-4e0ad82fd6cf", "SignatureVersion" to "1.0",
            "Timestamp" to "2016-02-23T12:46:24Z", "Version" to "2014-05-26",
        )
        assertEquals("OLeaidS1JvxuMvnyHOwuJ+uX5qY=", AliyunSigner.sign("GET", params, "testsecret")["Signature"])
    }
}
