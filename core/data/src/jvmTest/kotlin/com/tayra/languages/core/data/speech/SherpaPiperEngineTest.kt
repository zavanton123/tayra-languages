package com.tayra.languages.core.data.speech

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Piper on phones: each voice in a compact copy or the full model, the full one speaking when both are there. */
class SherpaPiperEngineTest {
    private class FakeRuntime : SherpaRuntime {
        val folders = mutableMapOf<String, List<String>>()
        val installed = mutableListOf<Pair<String, String>>()
        val requests = mutableListOf<SherpaRequest>()
        override fun folder(engine: String) = "/tts/$engine"
        override fun list(path: String) = folders[path]
        override suspend fun installArchive(url: String, destination: String, onDownload: (Long, Long) -> Unit, onUnpack: () -> Unit) {
            installed += url to destination
        }
        override suspend fun delete(path: String) {
            folders.remove(path)
        }
        override suspend fun synthesize(request: SherpaRequest): ByteArray = ByteArray(0).also { requests += request }
    }

    private val voice = "pt_BR-faber-medium"

    @Test
    fun eachVoiceComesCompactOrAsTheFullModel() = runTest {
        val runtime = FakeRuntime()
        val engine = SherpaPiperEngine(runtime)
        val (compact, full) = engine.packages().filter { it.id.startsWith(voice) }
        assertEquals(voice, compact.id)
        assertEquals("$voice-full", full.id)
        assertEquals("Faber · medium · Brazil · full model", full.title)
        assertEquals(compact.sizeBytes * 3, full.sizeBytes)

        engine.installPackage(compact.id)
        engine.installPackage(full.id)
        val models = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"
        assertEquals(listOf("$models/vits-piper-$voice-int8.tar.bz2" to "/tts/piper/$voice", "$models/vits-piper-$voice.tar.bz2" to "/tts/piper/$voice-full"), runtime.installed)
    }

    @Test
    fun theFullModelSpeaksWhenItIsOnTheDevice() = runTest {
        val runtime = FakeRuntime()
        val engine = SherpaPiperEngine(runtime)
        runtime.folders["/tts/piper/$voice"] = listOf("$voice.int8.onnx", "tokens.txt", "espeak-ng-data")
        assertEquals(listOf("Faber (Brazil, medium)"), engine.voices("pt").map { it.name })
        engine.synthesize("Olá.", "pt", voice, 1f)

        runtime.folders["/tts/piper/$voice-full"] = listOf("$voice.onnx", "tokens.txt", "espeak-ng-data")
        // Still one voice to choose, now speaking with the full model.
        assertEquals(listOf(voice), engine.voices("pt").map { it.id })
        assertEquals("Faber (Brazil, medium, full model)", engine.voices("pt").single().name)
        engine.synthesize("Olá.", "pt", voice, 1f)

        engine.removePackage("$voice-full")
        engine.synthesize("Olá.", "pt", voice, 1f)
        assertEquals(listOf("/tts/piper/$voice/$voice.int8.onnx", "/tts/piper/$voice-full/$voice.onnx", "/tts/piper/$voice/$voice.int8.onnx"), runtime.requests.map { it.model })
    }
}
