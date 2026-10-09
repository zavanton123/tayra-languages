package com.tayra.languages.core.data.speech

import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.io.encoding.Base64
import kotlin.js.Promise
import kotlin.math.roundToInt

private fun hasWebGpu(): Boolean = js("!!navigator.gpu")

/** Whether the model is loaded on this page, or its file is in the browser cache the library downloads into. */
private fun kokoroInstalled(modelId: String): Promise<JsAny?> = js(
    """(function () {
        if (window.__tayraKokoroReady) return Promise.resolve(true);
        return caches.open('transformers-cache').then(function (cache) { return cache.keys(); })
            .then(function (keys) { return keys.some(function (k) { return k.url.indexOf(modelId + '/resolve/main/onnx/') >= 0; }); });
    })()""",
)

private fun kokoroRemove(modelId: String): Promise<JsAny?> = js(
    """caches.open('transformers-cache').then(function (cache) {
        return cache.keys().then(function (keys) {
            return Promise.all(keys.filter(function (k) { return k.url.indexOf(modelId) >= 0; }).map(function (k) { return cache.delete(k); }));
        });
    }).then(function () { window.__tayraKokoro = null; window.__tayraKokoroReady = false; return null; })""",
)

/** Loads the model once per page, downloading it the first time; [onProgress] follows the model file. */
private fun kokoroLoad(modelId: String, dtype: String, device: String, onProgress: (Double) -> Unit): Promise<JsAny?> = js(
    """(function () {
        if (!window.__tayraKokoro) {
            window.__tayraKokoro = import('kokoro-js').then(function (m) {
                return m.KokoroTTS.from_pretrained(modelId, { dtype: dtype, device: device, progress_callback: function (p) {
                    if (p.status === 'progress' && p.file && p.file.indexOf('.onnx') >= 0) onProgress(p.progress / 100);
                } });
            });
            window.__tayraKokoro.then(function () { window.__tayraKokoroReady = true; }, function () { window.__tayraKokoro = null; });
        }
        return window.__tayraKokoro.then(function () { return null; });
    })()""",
)

/** The eSpeak phonemizer Piper's voices use, loaded once from its CDN build (18 MB of language data). */
private fun loadPhonemizer(): Promise<JsAny?> = js(
    """(function () {
        var base = 'https://cdn.jsdelivr.net/npm/@diffusionstudio/piper-wasm@1.0.0/build/piper_phonemize';
        if (!window.__tayraPhonemizer) {
            window.__tayraPhonemizer = new Promise(function (resolve, reject) {
                if (window.createPiperPhonemize) return resolve();
                var script = document.createElement('script');
                script.src = base + '.js';
                script.onload = resolve;
                script.onerror = function () { reject(new Error('Could not load the phonemizer')); };
                document.head.appendChild(script);
            }).then(function () {
                return function (text, language) {
                    var lines = [], errors = [];
                    return window.createPiperPhonemize({
                        print: function (line) { lines.push(JSON.parse(line)); },
                        printErr: function (message) { errors.push(message); },
                        locateFile: function (url) { return url.endsWith('.wasm') ? base + '.wasm' : url.endsWith('.data') ? base + '.data' : url; }
                    }).then(function (module) {
                        module.callMain(['-l', language, '--input', JSON.stringify([{ text: text.trim() }]), '--espeak_data', '/espeak-ng-data']);
                        if (lines.length === 0) throw new Error('No phonemes: ' + errors.join(' '));
                        return lines.map(function (l) { return l.phonemes.join(''); }).join(' ');
                    });
                };
            });
            window.__tayraPhonemizer.catch(function () { window.__tayraPhonemizer = null; });
        }
        return window.__tayraPhonemizer.then(function () { return null; });
    })()""",
)

/**
 * WAV audio of [text] as base64. English goes through the library's own text handling; the
 * other voices take eSpeak phonemes for [espeakLanguage], as the model was trained on them.
 */
private fun kokoroSpeak(voice: String, speed: Double, text: String, espeakLanguage: String?): Promise<JsAny?> = js(
    """window.__tayraKokoro.then(function (tts) {
        if (!espeakLanguage) return tts.generate(text, { voice: voice, speed: speed });
        return window.__tayraPhonemizer.then(function (phonemize) { return phonemize(text, espeakLanguage); }).then(function (phonemes) {
            var ids = tts.tokenizer(phonemes.replace(/ʲ/g, 'j'), { truncation: true }).input_ids;
            return tts.generate_from_ids(ids, { voice: voice, speed: speed });
        });
    }).then(function (audio) {
        var samples = audio.audio, rate = audio.sampling_rate, n = samples.length;
        var buffer = new ArrayBuffer(44 + n * 2), v = new DataView(buffer);
        function tag(offset, s) { for (var i = 0; i < s.length; i++) v.setUint8(offset + i, s.charCodeAt(i)); }
        tag(0, 'RIFF'); v.setUint32(4, 36 + n * 2, true); tag(8, 'WAVE'); tag(12, 'fmt ');
        v.setUint32(16, 16, true); v.setUint16(20, 1, true); v.setUint16(22, 1, true); v.setUint32(24, rate, true);
        v.setUint32(28, rate * 2, true); v.setUint16(32, 2, true); v.setUint16(34, 16, true); tag(36, 'data'); v.setUint32(40, n * 2, true);
        for (var i = 0; i < n; i++) { var s = Math.max(-1, Math.min(1, samples[i])); v.setInt16(44 + i * 2, s < 0 ? s * 32768 : s * 32767, true); }
        var bytes = new Uint8Array(buffer), binary = '';
        for (var j = 0; j < bytes.length; j += 8192) binary += String.fromCharCode.apply(null, bytes.subarray(j, j + 8192));
        return btoa(binary);
    })""",
)

/**
 * Kokoro in the browser, through Transformers.js: on the graphics card where the browser offers
 * WebGPU, at full precision, and otherwise a compressed model on the processor, which is slow.
 */
class WebKokoroEngine : LocalSpeechEngine {
    override val engine: SpeechEngine = SpeechEngine.KOKORO
    override val displayName: String = "Kokoro"
    override val description: String = "Kokoro is a high-quality neural voice that runs in the browser. It speaks English, Spanish, French, Italian and Portuguese; other languages fall back to the system voice. " +
        if (hasWebGpu()) "This browser lets it use the graphics card, so it answers quickly." else "This browser gives it no graphics card access, so it answers slowly; Chrome or Edge are faster."
    override val packagesDescription: String = "One download holds the model and all its voices."
    override val hasRuntimeSetup: Boolean = false
    override val progress = MutableStateFlow<String?>(null)

    private val webGpu = hasWebGpu()
    private val dtype = if (webGpu) "fp32" else "q8"
    private val device = if (webGpu) "webgpu" else "wasm"
    private val sizeBytes = if (webGpu) 326_000_000L else 92_000_000L

    private suspend fun installed(): Boolean = (kokoroInstalled(MODEL).await<JsAny?>() as? JsBoolean)?.toBoolean() == true

    override suspend fun status(): String = if (installed()) "Kokoro is ready. The model is downloaded." else "Kokoro is ready. The model is not downloaded yet."

    override suspend fun isReady(): Boolean = true

    override suspend fun setUp(): String = status()

    override suspend fun packages(): List<SpeechPackage> = listOf(
        SpeechPackage(PACKAGE, "Kokoro model with ${KokoroVoices.ALL.size} voices", languageCode = null, group = "English, Spanish, French, Italian, Portuguese", sizeBytes = sizeBytes, installed = installed()),
    )

    override suspend fun installPackage(id: String) {
        progress.value = "Downloading the Kokoro model..."
        try {
            load { fraction -> progress.value = "Downloading the Kokoro model: ${(fraction * 100).roundToInt()}%" }
        } finally {
            progress.value = null
        }
    }

    private suspend fun load(onProgress: (Double) -> Unit = {}) {
        kokoroLoad(MODEL, dtype, device, onProgress).await<JsAny?>()
    }

    override suspend fun removePackage(id: String) {
        kokoroRemove(MODEL).await<JsAny?>()
    }

    override fun packageVoices(id: String, languageCode: String): List<SpeechVoice> =
        KokoroVoices.ALL.filter { it.language == languageCode }.map { SpeechVoice(it.id, it.label, languageCode, KOKORO_VOICE_BYTES) }

    override suspend fun voices(languageCode: String): List<SpeechVoice> =
        if (!installed()) emptyList() else KokoroVoices.ALL.filter { it.language == languageCode }.map { SpeechVoice(it.id, it.label, languageCode) }

    override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? {
        if (!installed()) return null
        val usable = KokoroVoices.ALL.filter { it.language == languageCode }
        val voice = usable.firstOrNull { it.id == voiceId } ?: usable.firstOrNull() ?: return null
        load()
        val espeak = if (voice.language == "en") null else voice.accent
        if (espeak != null) loadPhonemizer().await<JsAny?>()
        val base64 = kokoroSpeak(voice.id, speed.toDouble(), text, espeak).await<JsAny?>()?.toString() ?: error("Kokoro gave no audio")
        return Base64.decode(base64)
    }

    private companion object {
        const val MODEL = "onnx-community/Kokoro-82M-v1.0-ONNX"
        const val PACKAGE = "kokoro"
    }
}
