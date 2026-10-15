package com.tayra.languages.core.domain.ocr

import com.tayra.languages.core.domain.model.Language

/** What reading text off a photo in a language takes on this device. */
sealed interface OcrReadiness {
    data object Ready : OcrReadiness

    /**
     * A recognition model has to be downloaded first: [sizeBytes] of it, or null when the system
     * fetches it and does not say (Google Play's models on Android).
     */
    data class NeedsDownload(val sizeBytes: Long?) : OcrReadiness

    /** This device cannot read the language's script. */
    data object Unsupported : OcrReadiness
}

/** A downloaded recognition model, which the learner may remove. */
data class OcrPack(val id: String, val title: String, val sizeBytes: Long)

class OcrException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Reads the text of photos and scans, on the device: Apple's Vision on iOS, Google's ML Kit on
 * Android where it knows the script, Tesseract with a downloaded model for the language elsewhere.
 */
interface TextRecognizer {

    suspend fun readiness(language: Language): OcrReadiness

    /** Fetches what [language] needs, reporting the share done (null while it is not known). */
    suspend fun download(language: Language, onProgress: (Float?) -> Unit)

    /** The text of the photo [image] (JPEG, PNG and the like), one paragraph per line; empty when it shows none. */
    suspend fun recognize(image: ByteArray, language: Language): String

    /** The models downloaded so far. */
    suspend fun installedPacks(): List<OcrPack>

    suspend fun remove(pack: OcrPack)
}

/** Where no text can be read from photos (the browser). */
object NoTextRecognizer : TextRecognizer {
    override suspend fun readiness(language: Language): OcrReadiness = OcrReadiness.Unsupported
    override suspend fun download(language: Language, onProgress: (Float?) -> Unit) = Unit
    override suspend fun recognize(image: ByteArray, language: Language): String = throw OcrException("Reading text from photos is not available here")
    override suspend fun installedPacks(): List<OcrPack> = emptyList()
    override suspend fun remove(pack: OcrPack) = Unit
}
