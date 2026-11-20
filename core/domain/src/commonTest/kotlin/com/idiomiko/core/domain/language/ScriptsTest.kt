package com.idiomiko.core.domain.language

import kotlin.test.Test
import kotlin.test.assertEquals

class ScriptsTest {
    @Test
    fun bengaliIsWrittenInItsOwnScript() {
        assertEquals(setOf(Script.BENGALI), Scripts.of("bn"))
        assertEquals(Script.BENGALI, Scripts.dominant("আমি বাংলায় কথা বলি।"))
    }

    @Test
    fun tamilIsWrittenInItsOwnScript() {
        assertEquals(setOf(Script.TAMIL), Scripts.of("ta"))
        assertEquals(Script.TAMIL, Scripts.dominant("நான் தமிழ் பேசுகிறேன்."))
    }

    @Test
    fun punjabiIsWrittenInGurmukhi() {
        assertEquals(setOf(Script.GURMUKHI), Scripts.of("pa"))
        assertEquals(Script.GURMUKHI, Scripts.dominant("ਮੈਂ ਪੰਜਾਬੀ ਬੋਲਦਾ ਹਾਂ।"))
    }

    @Test
    fun gujaratiIsWrittenInItsOwnScript() {
        assertEquals(setOf(Script.GUJARATI), Scripts.of("gu"))
        assertEquals(Script.GUJARATI, Scripts.dominant("હું ગુજરાતી બોલું છું."))
    }

    @Test
    fun sinhalaIsWrittenInItsOwnScript() {
        assertEquals(setOf(Script.SINHALA), Scripts.of("si"))
        assertEquals(Script.SINHALA, Scripts.dominant("මම සිංහල කතා කරනවා."))
    }
}
