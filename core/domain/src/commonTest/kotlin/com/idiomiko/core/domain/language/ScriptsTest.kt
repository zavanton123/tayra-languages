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
}
