package com.tayra.languages.core.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.ocr.OcrException
import com.tayra.languages.core.domain.ocr.OcrPack
import com.tayra.languages.core.domain.ocr.OcrReadiness
import com.tayra.languages.core.domain.ocr.TextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Reads photos on Android with ML Kit where it knows the script (Latin, Chinese, Japanese, Korean,
 * Devanagari): Google Play services fetches its model, and reads camera photos well. Other scripts,
 * phones without Google Play, and phones whose Google Play cannot fetch the model, are read by
 * [tesseract] with a downloaded model.
 */
class AndroidTextRecognizer(private val context: Context, private val tesseract: TesseractTextRecognizer) : TextRecognizer {

    private val playServices: Boolean by lazy {
        runCatching { GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS }.getOrDefault(false)
    }

    /** Set once Google Play failed to fetch an ML Kit model: Tesseract reads from then on. */
    @Volatile
    private var mlKitFailed = false

    /** ML Kit's recognizer for [language]'s script, or null when Tesseract reads it. */
    private fun mlKit(language: Language): TextRecognizerOptionsInterface? {
        if (!playServices || mlKitFailed) return null
        return when (OcrLanguages.of(language).script) {
            OcrScript.LATIN -> TextRecognizerOptions.DEFAULT_OPTIONS
            OcrScript.HAN_SIMPLIFIED, OcrScript.HAN_TRADITIONAL -> ChineseTextRecognizerOptions.Builder().build()
            OcrScript.JAPANESE -> JapaneseTextRecognizerOptions.Builder().build()
            OcrScript.KOREAN -> KoreanTextRecognizerOptions.Builder().build()
            OcrScript.DEVANAGARI -> DevanagariTextRecognizerOptions.Builder().build()
            else -> null
        }
    }

    /** Whether Google Play has ML Kit's model for [options] on the phone. */
    private suspend fun mlKitInstalled(options: TextRecognizerOptionsInterface): Boolean {
        val client = TextRecognition.getClient(options)
        return try {
            ModuleInstall.getClient(context).areModulesAvailable(client).await().areModulesAvailable()
        } catch (e: Exception) {
            false
        } finally {
            client.close()
        }
    }

    override suspend fun readiness(language: Language): OcrReadiness {
        val options = mlKit(language) ?: return tesseract.readiness(language)
        if (mlKitInstalled(options)) return OcrReadiness.Ready
        // A Tesseract model downloaded once when Google Play could not fetch ML Kit's reads as well.
        if (tesseract.readiness(language) == OcrReadiness.Ready) return OcrReadiness.Ready
        return OcrReadiness.NeedsDownload(null)
    }

    override suspend fun download(language: Language, onProgress: (Float?) -> Unit) {
        val options = mlKit(language) ?: return tesseract.download(language, onProgress)
        val client = TextRecognition.getClient(options)
        try {
            installModule(client, onProgress)
        } catch (e: OcrException) {
            // Some phones have Google Play services that cannot fetch modules: Tesseract's model comes from GitHub instead.
            if (!tesseract.available) throw e
            mlKitFailed = true
            if (tesseract.readiness(language) is OcrReadiness.NeedsDownload) tesseract.download(language, onProgress)
        } finally {
            client.close()
        }
    }

    /** Asks Google Play services for the recognizer's model and waits until it is in. */
    private suspend fun installModule(client: com.google.mlkit.vision.text.TextRecognizer, onProgress: (Float?) -> Unit) {
        val installer = ModuleInstall.getClient(context)
        suspendCancellableCoroutine { continuation ->
            lateinit var listener: InstallStatusListener
            listener = InstallStatusListener { update: ModuleInstallStatusUpdate ->
                update.progressInfo?.let { info -> if (info.totalBytesToDownload > 0) onProgress(info.bytesDownloaded.toFloat() / info.totalBytesToDownload) }
                when (update.installState) {
                    ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED -> {
                        installer.unregisterListener(listener)
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    ModuleInstallStatusUpdate.InstallState.STATE_FAILED, ModuleInstallStatusUpdate.InstallState.STATE_CANCELED -> {
                        installer.unregisterListener(listener)
                        if (continuation.isActive) continuation.resumeWithException(OcrException("Google Play could not download the text recognition model; check the internet connection"))
                    }
                }
            }
            val request = ModuleInstallRequest.newBuilder().addApi(client).setListener(listener).build()
            installer.installModules(request)
                .addOnSuccessListener { response ->
                    if (response.areModulesAlreadyInstalled()) {
                        installer.unregisterListener(listener)
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
                .addOnFailureListener { e ->
                    installer.unregisterListener(listener)
                    if (continuation.isActive) continuation.resumeWithException(OcrException("Google Play could not download the text recognition model; check the internet connection", e))
                }
            continuation.invokeOnCancellation { installer.unregisterListener(listener) }
        }
    }

    override suspend fun recognize(image: ByteArray, language: Language): String {
        val options = mlKit(language)?.takeIf { mlKitInstalled(it) } ?: return tesseract.recognize(readableByLeptonica(image), language)
        val ocr = OcrLanguages.of(language)
        val bitmap = withContext(Dispatchers.Default) { decode(image) }
        val client = TextRecognition.getClient(options)
        try {
            val result = client.process(InputImage.fromBitmap(bitmap, 90 * ExifOrientation.quarterTurns(image))).await()
            return OcrText.clean(blocks(result), ocr)
        } finally {
            client.close()
            bitmap.recycle()
        }
    }

    /** ML Kit's blocks are paragraphs, their lines the lines of the page. */
    private fun blocks(text: Text): String = text.textBlocks.joinToString("\n\n") { block -> block.lines.joinToString("\n") { it.text } }

    /** The photo, made at most [MAX_SIDE] pixels on its long side: a 12-megapixel photo reads no better at full size. */
    private fun decode(bytes: ByteArray): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw OcrException("The photo could not be read; use a JPEG, PNG, WebP or HEIC image")
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw OcrException("The photo could not be read; use a JPEG, PNG, WebP or HEIC image")
    }

    /**
     * The photo as Tesseract's image library reads it: JPEG and PNG as they are, anything else
     * Android decodes (HEIC, WebP) made a JPEG first.
     */
    private suspend fun readableByLeptonica(image: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        val jpeg = image.size > 3 && image[0] == 0xFF.toByte() && image[1] == 0xD8.toByte()
        val png = image.size > 8 && image[0] == 0x89.toByte() && image[1] == 'P'.code.toByte()
        if (jpeg || png) return@withContext image
        val bitmap = decode(image)
        try {
            java.io.ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }.toByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    override suspend fun installedPacks(): List<OcrPack> = tesseract.installedPacks()

    override suspend fun remove(pack: OcrPack) = tesseract.remove(pack)

    private companion object {
        const val MAX_SIDE = 2400
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
