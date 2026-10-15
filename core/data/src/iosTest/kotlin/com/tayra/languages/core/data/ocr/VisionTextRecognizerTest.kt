package com.tayra.languages.core.data.ocr

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.ocr.OcrReadiness
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What Vision, part of iOS, reads without a download. Its recognition itself returns no text in the
 * simulator on Apple silicon, whose Vision has no text models; it is checked on a device.
 */
class VisionTextRecognizerTest {

    private val recognizer = VisionTextRecognizer()

    @Test
    fun visionsLanguagesAndOtherLatinOnesAreReadyAndOtherScriptsUnsupported() = runTest {
        assertEquals(OcrReadiness.Ready, recognizer.readiness(Language(name = "Russian")))
        assertEquals(OcrReadiness.Ready, recognizer.readiness(Language(name = "Mandarin Chinese")))
        assertEquals(OcrReadiness.Ready, recognizer.readiness(Language(name = "Finnish")))
        assertEquals(OcrReadiness.Unsupported, recognizer.readiness(Language(name = "Georgian")))
    }
}
