package com.tayra.languages.core.data.network

import kotlin.test.Test
import kotlin.test.assertEquals

class Md5Test {
    @Test
    fun matchesKnownVectors() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Md5.hex(ByteArray(0)))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Md5.hex("abc".encodeToByteArray()))
        assertEquals("9e107d9d372bb6826bd81d3542a419d6", Md5.hex("The quick brown fox jumps over the lazy dog".encodeToByteArray()))
        assertEquals("57edf4a22be3c955ac49da2e2107b67a", Md5.hex("12345678901234567890123456789012345678901234567890123456789012345678901234567890".encodeToByteArray()))
        // Baidu's documented sample: appid 2015063000000001, q "apple", salt 1435660288, key 12345678.
        assertEquals("f89f9594663708c1605f3d736d01d2d4", Md5.hex("2015063000000001apple143566028812345678".encodeToByteArray()))
    }
}
