package com.tayra.languages.core.data.ocr

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.ocr.OcrException
import com.tayra.languages.core.domain.ocr.OcrPack
import com.tayra.languages.core.domain.ocr.OcrReadiness
import com.tayra.languages.core.domain.ocr.TextRecognizer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.ImageIO.CGImagePropertyOrientation
import platform.ImageIO.kCGImagePropertyOrientationDown
import platform.ImageIO.kCGImagePropertyOrientationDownMirrored
import platform.ImageIO.kCGImagePropertyOrientationLeft
import platform.ImageIO.kCGImagePropertyOrientationLeftMirrored
import platform.ImageIO.kCGImagePropertyOrientationRight
import platform.ImageIO.kCGImagePropertyOrientationRightMirrored
import platform.ImageIO.kCGImagePropertyOrientationUp
import platform.ImageIO.kCGImagePropertyOrientationUpMirrored
import platform.UIKit.UIImage
import platform.UIKit.UIImageOrientation
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

/**
 * Reads photos with Apple's Vision, part of iOS: nothing to download. A language Vision reads is
 * read as itself; another one written in the Latin alphabet is read with all the Latin languages
 * Vision knows and no language correction, so its letters come through as written. Other scripts
 * Vision does not know are not read.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class VisionTextRecognizer : TextRecognizer {

    /** Vision's languages ("en-US", "zh-Hans", "ru-RU"), for its accurate recognition. */
    private val supported: List<String> by lazy {
        val request = VNRecognizeTextRequest()
        request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
        request.supportedRecognitionLanguagesAndReturnError(null)?.filterIsInstance<String>().orEmpty()
    }

    /** Vision's languages to read [language] with, and whether to correct by them; null when Vision cannot read it. */
    private fun visionLanguages(language: Language): Pair<List<String>, Boolean>? {
        val ocr = OcrLanguages.of(language)
        val wanted: List<String> = when (ocr.script) {
            OcrScript.HAN_SIMPLIFIED -> supported.filter { it == (if (ocr.code == "yue") "yue-Hans" else "zh-Hans") }
            OcrScript.HAN_TRADITIONAL -> supported.filter { it == (if (ocr.code == "yue") "yue-Hant" else "zh-Hant") }
            else -> {
                val codes = if (ocr.code == "no") setOf("no", "nb", "nn") else setOfNotNull(ocr.code)
                supported.filter { it.substringBefore('-') in codes }
            }
        }
        if (wanted.isNotEmpty()) return wanted to true
        if (ocr.script == OcrScript.LATIN) {
            val latin = supported.filter { it.substringBefore('-') in LATIN_LANGUAGES }
            if (latin.isNotEmpty()) return latin to false
        }
        return null
    }

    override suspend fun readiness(language: Language): OcrReadiness =
        if (visionLanguages(language) != null) OcrReadiness.Ready else OcrReadiness.Unsupported

    override suspend fun download(language: Language, onProgress: (Float?) -> Unit) = onProgress(1f)

    override suspend fun recognize(image: ByteArray, language: Language): String = withContext(Dispatchers.Default) {
        val (languages, corrected) = visionLanguages(language) ?: throw OcrException("Reading this language from photos is not available on this device")
        val data = image.usePinned { NSData.create(bytes = it.addressOf(0), length = image.size.toULong()) }
        val picture = UIImage(data = data)
        val cgImage = picture.CGImage ?: throw OcrException("The photo could not be read; use a JPEG, PNG or HEIC image")
        val request = VNRecognizeTextRequest()
        request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
        request.recognitionLanguages = languages
        request.usesLanguageCorrection = corrected
        val handler = VNImageRequestHandler(cGImage = cgImage, orientation = orientation(picture.imageOrientation), options = emptyMap<Any?, Any?>())
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            if (!handler.performRequests(listOf(request), error.ptr)) {
                throw OcrException("Could not read the photo")
            }
        }
        val lines = request.results.orEmpty().filterIsInstance<VNRecognizedTextObservation>().mapNotNull { observation ->
            val text = (observation.topCandidates(1uL).firstOrNull() as? VNRecognizedText)?.string ?: return@mapNotNull null
            // Vision's boxes are fractions of the picture, measured from its bottom left.
            observation.boundingBox.useContents { Line(text, top = 1 - (origin.y + size.height), bottom = 1 - origin.y, left = origin.x) }
        }
        OcrText.clean(paragraphs(lines), OcrLanguages.of(language))
    }

    private class Line(val text: String, val top: Double, val bottom: Double, val left: Double)

    /** The lines top to bottom, an empty line between two that a gap of most of a line's height parts. */
    private fun paragraphs(lines: List<Line>): String {
        if (lines.isEmpty()) return ""
        val sorted = lines.sortedWith(compareBy({ it.top }, { it.left }))
        val heights = sorted.map { it.bottom - it.top }.sorted()
        val height = heights[heights.size / 2]
        val out = StringBuilder(sorted.first().text)
        for (i in 1 until sorted.size) {
            out.append(if (sorted[i].top - sorted[i - 1].bottom > height * 0.7) "\n\n" else "\n")
            out.append(sorted[i].text)
        }
        return out.toString()
    }

    /** How UIKit says a photo is turned, as Vision wants it said. */
    private fun orientation(value: UIImageOrientation): CGImagePropertyOrientation = when (value) {
        UIImageOrientation.UIImageOrientationDown -> kCGImagePropertyOrientationDown
        UIImageOrientation.UIImageOrientationLeft -> kCGImagePropertyOrientationLeft
        UIImageOrientation.UIImageOrientationRight -> kCGImagePropertyOrientationRight
        UIImageOrientation.UIImageOrientationUpMirrored -> kCGImagePropertyOrientationUpMirrored
        UIImageOrientation.UIImageOrientationDownMirrored -> kCGImagePropertyOrientationDownMirrored
        UIImageOrientation.UIImageOrientationLeftMirrored -> kCGImagePropertyOrientationLeftMirrored
        UIImageOrientation.UIImageOrientationRightMirrored -> kCGImagePropertyOrientationRightMirrored
        else -> kCGImagePropertyOrientationUp
    }

    override suspend fun installedPacks(): List<OcrPack> = emptyList()

    override suspend fun remove(pack: OcrPack) = Unit

    private companion object {
        /** The languages of Vision's that are written in the Latin alphabet. */
        val LATIN_LANGUAGES = setOf("en", "fr", "it", "de", "es", "pt", "cs", "da", "nl", "no", "nb", "nn", "ms", "pl", "ro", "sv", "tr", "id", "vi")
    }
}
