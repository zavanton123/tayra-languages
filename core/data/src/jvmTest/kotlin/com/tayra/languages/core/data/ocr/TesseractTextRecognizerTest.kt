package com.tayra.languages.core.data.ocr

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.domain.ocr.OcrReadiness
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tesseract reads text drawn into a picture once its model is downloaded. The models are kept in
 * the build folder, so they download once.
 */
class TesseractTextRecognizerTest {

    private val tessdata = File("build/ocr-test-tessdata")
    private val recognizer = TesseractTextRecognizer(tessdata)

    private fun picture(lines: List<String>, font: Font, rotate: Boolean = false, format: String = "png"): ByteArray {
        val width = 1800
        val leading = (font.size * 1.4).toInt()
        val height = 140 + lines.size * leading
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        // Lit unevenly, as a page under a lamp.
        g.paint = java.awt.GradientPaint(0f, 0f, Color(250, 248, 240), width.toFloat(), height.toFloat(), Color(190, 185, 170))
        g.fillRect(0, 0, width, height)
        g.color = Color(30, 30, 30)
        g.font = font
        lines.forEachIndexed { i, line -> g.drawString(line, 60, 100 + i * leading) }
        g.dispose()
        val out = ByteArrayOutputStream()
        ImageIO.write(if (rotate) quarterTurnLeft(image) else image, format, out)
        return out.toByteArray()
    }

    private fun quarterTurnLeft(image: BufferedImage): BufferedImage {
        val turned = BufferedImage(image.height, image.width, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until image.width) for (y in 0 until image.height) turned.setRGB(y, image.width - 1 - x, image.getRGB(x, y))
        return turned
    }

    private fun ready(language: Language) = runBlocking {
        if (recognizer.readiness(language) is OcrReadiness.NeedsDownload) recognizer.download(language) {}
        assertEquals(OcrReadiness.Ready, recognizer.readiness(language))
    }

    @Test
    fun russianTextFromAPictureInParagraphs() {
        val russian = Language(name = "Russian")
        ready(russian)
        val image = picture(
            listOf("Мой друг живёт в большом городе и", "работает в библиотеке каждый день.", "", "Вечером мы гуляем в парке."),
            Font("SansSerif", Font.PLAIN, 44),
        )
        val text = runBlocking { recognizer.recognize(image, russian) }
        assertEquals("Мой друг живёт в большом городе и работает в библиотеке каждый день.\nВечером мы гуляем в парке.", text)
        assertTrue(recognizer.let { runBlocking { it.installedPacks() } }.any { it.id == "rus" && it.title == "Russian" })
    }

    @Test
    fun chineseCharactersWithoutTheSpacesTesseractPutsBetweenThem() {
        val chinese = Language(name = "Mandarin Chinese", chineseScript = ChineseScript.SIMPLIFIED)
        ready(chinese)
        val font = listOf("PingFang SC", "Heiti SC", "STHeiti", "Noto Sans CJK SC").map { Font(it, Font.PLAIN, 56) }.firstOrNull { it.canDisplay('我') } ?: return
        val text = runBlocking { recognizer.recognize(picture(listOf("我们是学生。他们也是学生。"), font), chinese) }
        assertEquals("我们是学生。他们也是学生。", text)
    }

    /** A phone stores a photo as the sensor saw it and names the turn in EXIF; the text is read upright. */
    @Test
    fun aSidewaysPhotoIsTurnedAsItsExifSays() {
        val russian = Language(name = "Russian")
        ready(russian)
        val jpeg = picture(listOf("Вечером мы гуляем в парке."), Font("SansSerif", Font.PLAIN, 44), rotate = true, format = "jpg")
        assertEquals(0, ExifOrientation.quarterTurns(jpeg))
        val withExif = withOrientation(jpeg, 6)
        assertEquals(1, ExifOrientation.quarterTurns(withExif))
        assertEquals("Вечером мы гуляем в парке.", runBlocking { recognizer.recognize(withExif, russian) })
    }

    /** [jpeg] with an EXIF segment naming [orientation], right after its start marker. */
    private fun withOrientation(jpeg: ByteArray, orientation: Int): ByteArray {
        val tiff = byteArrayOf(0x4D, 0x4D, 0x00, 0x2A, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, orientation.toByte(), 0, 0, 0, 0, 0, 0)
        val body = "Exif\u0000\u0000".toByteArray(Charsets.ISO_8859_1) + tiff
        val length = body.size + 2
        val segment = byteArrayOf(0xFF.toByte(), 0xE1.toByte(), (length shr 8).toByte(), length.toByte()) + body
        return jpeg.copyOfRange(0, 2) + segment + jpeg.copyOfRange(2, jpeg.size)
    }

    /** An engine's empty line between two lines of one sentence is no paragraph's end; one after a sentence is. */
    @Test
    fun emptyLinesInsideASentenceDoNotPartIt() {
        val russian = OcrLanguages.of(Language(name = "Russian"))
        assertEquals("Утром он идёт на работу.\nВечером отдыхает.", OcrText.clean("Утром он идёт\n\nна работу.\n\nВечером отдыхает.", russian))
        assertEquals("我们是学生。", OcrText.clean("我 们 是 学 生 。", OcrLanguages.of(Language(name = "Mandarin Chinese"))))
    }

    /** A language needs its model, of the size the catalog gives; one Tesseract has none for is read by its script's. */
    @Test
    fun eachLanguageNeedsItsModelOrItsScripts() {
        val readiness = runBlocking { recognizer.readiness(Language(name = "Portuguese")) }
        assertTrue(readiness is OcrReadiness.Ready || readiness == OcrReadiness.NeedsDownload(TesseractModels.SIZES["por"]), readiness.toString())
        assertEquals(listOf("script/Latin"), OcrLanguages.of(Language(name = "Manx")).tesseractModels)
        assertEquals(listOf("srp", "srp_latn"), OcrLanguages.of(Language(name = "Serbian")).tesseractModels)
        assertEquals(listOf("chi_tra"), OcrLanguages.of(Language(name = "Cantonese")).tesseractModels)
        assertEquals("Latin script", OcrLanguages.title("script/Latin"))
    }
}
