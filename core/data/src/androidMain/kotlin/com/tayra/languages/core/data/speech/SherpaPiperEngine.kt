package com.tayra.languages.core.data.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import java.io.File
import java.util.Locale

/** Piper on Android: the voices sherpa-onnx packages, in their compact copies. */
class SherpaPiperEngine(context: Context) : SherpaSpeechEngine(context, "piper") {

    override val engine: SpeechEngine = SpeechEngine.PIPER
    override val displayName: String = "Piper"
    override val description: String = "Piper is a neural speech engine that runs on this phone with no network. It has voices for most of the languages the app teaches."
    override val packagesDescription: String = "One download per voice, about 20 MB each."

    /** A packaged voice, from its `locale-voice-quality` key. */
    private class Voice(val key: String, val size: Long) {
        val locale: String = key.substringBefore('-')
        val quality: String = key.substringAfterLast('-').replace('_', ' ')
        val name: String = key.substringAfter('-').substringBeforeLast('-').replace('_', ' ').replaceFirstChar { it.uppercase() }
        private val javaLocale = Locale(locale.substringBefore('_'), locale.substringAfter('_', ""))
        val language: String = javaLocale.getDisplayLanguage(Locale.ENGLISH).ifEmpty { locale }
        val country: String = javaLocale.getDisplayCountry(Locale.ENGLISH).ifEmpty { locale.substringAfter('_', "") }

        /** The app's language code: Piper writes Norwegian as "no", the app uses "nb". */
        val appCode: String = locale.substringBefore('_').let { if (it == "no") "nb" else it }
        val title: String get() = "$name \u00b7 $quality \u00b7 $country"
    }

    private val catalog: List<Voice> = SherpaPiperVoices.ALL.map { (key, size) -> Voice(key, size) }
        .sortedWith(compareBy({ it.language }, { it.country }, { it.name }, { it.quality }))

    override suspend fun status(): String {
        val count = catalog.count { isInstalled(it.key) }
        return if (count == 0) "Piper is ready. No voices downloaded yet." else "Piper is ready with $count voice${if (count == 1) "" else "s"} downloaded."
    }

    override suspend fun packages(): List<SpeechPackage> = catalog.map { SpeechPackage(it.key, it.title, it.appCode, it.language, it.size, isInstalled(it.key)) }

    override suspend fun installPackage(id: String) {
        val voice = catalog.firstOrNull { it.key == id } ?: error("unknown voice $id")
        installArchive("$MODELS/vits-piper-${voice.key}-int8.tar.bz2", id, voice.name)
    }

    override suspend fun removePackage(id: String) = remove(id)

    override suspend fun voices(languageCode: String): List<SpeechVoice> =
        catalog.filter { it.appCode == languageCode && isInstalled(it.key) }.map { SpeechVoice(it.key, "${it.name} (${it.country}, ${it.quality})", languageCode) }

    override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? {
        val usable = voices(languageCode)
        val voice = usable.firstOrNull { it.id == voiceId } ?: usable.firstOrNull() ?: return null
        val folder = File(dir, voice.id)
        val model = model(folder) ?: return null
        return speak(voice.id, text, speaker = 0, speed = speed) {
            val vits = OfflineTtsVitsModelConfig(model = model.absolutePath, tokens = File(folder, "tokens.txt").absolutePath, dataDir = File(folder, "espeak-ng-data").absolutePath)
            OfflineTts(config = OfflineTtsConfig(model = OfflineTtsModelConfig(vits = vits, numThreads = 2, debug = false, provider = "cpu")))
        }
    }

    private fun isInstalled(key: String): Boolean = model(File(dir, key)) != null

    private fun model(folder: File): File? = folder.listFiles()?.firstOrNull { it.name.endsWith(".onnx") }?.takeIf { File(folder, "tokens.txt").exists() }
}
