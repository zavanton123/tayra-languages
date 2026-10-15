package com.tayra.languages.core.data.ocr

import com.tayra.languages.core.data.speech.FileDownloads
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.ocr.OcrException
import com.tayra.languages.core.domain.ocr.OcrPack
import com.tayra.languages.core.domain.ocr.OcrReadiness
import com.tayra.languages.core.domain.ocr.TextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.javacpp.Loader
import org.bytedeco.leptonica.PIX
import org.bytedeco.leptonica.global.leptonica
import org.bytedeco.tesseract.TessBaseAPI
import org.bytedeco.tesseract.global.tesseract
import java.io.File

/**
 * Reads photos with Tesseract (Apache 2.0, through bytedeco's builds) and a model per language,
 * downloaded into [tessdata] when first needed. A phone photo is turned as its EXIF says, made
 * grey and its uneven lighting evened out before Tesseract reads it.
 */
class TesseractTextRecognizer(private val tessdata: File) : TextRecognizer {

    /** Whether Tesseract's native libraries load here: bytedeco builds no 32-bit ARM ones for Android. */
    val available: Boolean by lazy {
        runCatching { Loader.load(tesseract::class.java) }.isSuccess
    }

    private fun file(model: String) = File(tessdata, "$model.traineddata")

    private fun missing(language: Language): List<String> = OcrLanguages.of(language).tesseractModels.filter { !file(it).isFile }

    override suspend fun readiness(language: Language): OcrReadiness = withContext(Dispatchers.IO) {
        val models = OcrLanguages.of(language).tesseractModels
        when {
            !available || models.any { it !in TesseractModels.SIZES } -> OcrReadiness.Unsupported
            else -> missing(language).let { gone -> if (gone.isEmpty()) OcrReadiness.Ready else OcrReadiness.NeedsDownload(gone.sumOf { TesseractModels.SIZES.getValue(it) }) }
        }
    }

    override suspend fun download(language: Language, onProgress: (Float?) -> Unit) = withContext(Dispatchers.IO) {
        val models = missing(language)
        val total = models.sumOf { TesseractModels.SIZES[it] ?: 0L }.coerceAtLeast(1L)
        var before = 0L
        for (model in models) {
            val target = file(model)
            target.parentFile?.mkdirs()
            try {
                FileDownloads.download(TesseractModels.url(model), target) { done, _ -> onProgress(((before + done).toFloat() / total).coerceIn(0f, 1f)) }
            } catch (e: Exception) {
                throw OcrException("Could not download the text recognition model; check the internet connection", e)
            }
            before += TesseractModels.SIZES[model] ?: target.length()
        }
        onProgress(1f)
    }

    override suspend fun recognize(image: ByteArray, language: Language): String = withContext(Dispatchers.Default) {
        val ocr = OcrLanguages.of(language)
        OcrText.clean(read(image, ocr.tesseractModels), ocr)
    }

    /** Tesseract's text of [image] in [models], its paragraphs parted by empty lines. */
    internal fun read(image: ByteArray, models: List<String>): String {
        if (!available) throw OcrException("Text recognition is not available on this device")
        val source = leptonica.pixReadMem(image, image.size.toLong())
            ?: throw OcrException("The photo could not be read; use a JPEG, PNG, TIFF, BMP or WebP image")
        val pixes = mutableListOf(source)
        try {
            val turns = ExifOrientation.quarterTurns(image)
            val upright = if (turns == 0) source else leptonica.pixRotateOrth(source, turns).also { pixes += it }
            val grey = leptonica.pixConvertTo8(upright, 0).also { pixes += it }
            // Letters Tesseract reads best are some 30 pixels tall; a small picture is enlarged.
            val width = leptonica.pixGetWidth(grey)
            val scaled = if (width in 1 until 1600) leptonica.pixScale(grey, 2f, 2f).also { pixes += it } else grey
            val even = leptonica.pixBackgroundNormSimple(scaled, null, null)?.also { pixes += it } ?: scaled
            return ocr(even, models)
        } finally {
            pixes.forEach { leptonica.pixDestroy(it) }
        }
    }

    private fun ocr(pix: PIX, models: List<String>): String {
        val api = TessBaseAPI()
        try {
            if (api.Init(tessdata.absolutePath, models.joinToString("+"), tesseract.OEM_LSTM_ONLY) != 0) {
                throw OcrException("Could not start text recognition; download its model again")
            }
            // Photos carry no resolution; without one Tesseract guesses and warns.
            api.SetVariable("user_defined_dpi", "300")
            api.SetPageSegMode(tesseract.PSM_AUTO)
            api.SetImage(pix)
            val text = api.GetUTF8Text()
            return try {
                text?.getString(Charsets.UTF_8).orEmpty()
            } finally {
                text?.deallocate()
            }
        } finally {
            api.End()
            api.close()
        }
    }

    override suspend fun installedPacks(): List<OcrPack> = withContext(Dispatchers.IO) {
        TesseractModels.SIZES.keys.filter { file(it).isFile }.map { OcrPack(it, OcrLanguages.title(it), file(it).length()) }.sortedBy { it.title }
    }

    override suspend fun remove(pack: OcrPack) {
        withContext(Dispatchers.IO) { file(pack.id).delete() }
    }
}
