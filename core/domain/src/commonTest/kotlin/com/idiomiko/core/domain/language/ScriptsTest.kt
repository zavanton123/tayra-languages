package com.idiomiko.core.domain.language

import kotlin.test.Test
import kotlin.test.assertEquals

class ScriptsTest {
    @Test
    fun bengaliIsWrittenInItsOwnScript() {
        assertEquals(setOf(Script.BENGALI), Scripts.of("bn"))
        assertEquals(Script.BENGALI, Scripts.dominant("আমি বাংলায় কথা বলি।"))
    }
}
