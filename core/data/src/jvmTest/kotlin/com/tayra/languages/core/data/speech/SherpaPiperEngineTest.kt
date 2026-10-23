package com.tayra.languages.core.data.speech

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Piper on phones: each voice downloaded as the full model; a compact copy from before still speaks until then, and can be removed. */
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
    fun eachVoiceIsOfferedAsTheFullModel() = runTest {
        val runtime = FakeRuntime()
        val engine = SherpaPiperEngine(runtime)
        val pkg = engine.packages().single { it.id.startsWith(voice) }
        assertEquals(voice, pkg.id, "the package is the voice, as the setup dialog saves it")
        assertEquals("Faber · medium · Brazil", pkg.title)

        engine.installPackage(pkg.id)
        val models = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"
        assertEquals(listOf("$models/vits-piper-$voice.tar.bz2" to "/tts/piper/$voice-full"), runtime.installed)
    }

    @Test
    fun aCompactCopyFromBeforeIsListedToRemove() = runTest {
        val runtime = FakeRuntime()
        val engine = SherpaPiperEngine(runtime)
        runtime.folders["/tts/piper/$voice"] = listOf("$voice.int8.onnx", "tokens.txt", "espeak-ng-data")
        val (full, compact) = engine.packages().filter { it.id.startsWith(voice) }
        assertEquals("Faber · medium · Brazil · compact model", compact.title)
        assertTrue(compact.installed && !full.installed)
        assertEquals(compact.sizeBytes * 3, full.sizeBytes)
        assertEquals("Piper is ready with 1 voice downloaded.", engine.status())

        engine.removePackage(compact.id)
        assertEquals(null, runtime.folders["/tts/piper/$voice"])
        assertEquals(listOf(voice), engine.packages().filter { it.id.startsWith(voice) }.map { it.id })
    }

    @Test
    fun theFullModelSpeaksWhenItIsOnTheDevice() = runTest {
        val runtime = FakeRuntime()
        val engine = SherpaPiperEngine(runtime)
        runtime.folders["/tts/piper/$voice"] = listOf("$voice.int8.onnx", "tokens.txt", "espeak-ng-data")
        assertEquals(listOf("Faber (Brazil, medium, compact model)"), engine.voices("pt").map { it.name })
        engine.synthesize("Olá.", "pt", voice, 1f)

        runtime.folders["/tts/piper/$voice-full"] = listOf("$voice.onnx", "tokens.txt", "espeak-ng-data")
        // Still one voice to choose, now speaking with the full model.
        assertEquals(listOf(voice), engine.voices("pt").map { it.id })
        assertEquals("Faber (Brazil, medium)", engine.voices("pt").single().name)
        engine.synthesize("Olá.", "pt", voice, 1f)

        engine.removePackage(voice)
        engine.synthesize("Olá.", "pt", voice, 1f)
        assertEquals(listOf("/tts/piper/$voice/$voice.int8.onnx", "/tts/piper/$voice-full/$voice.onnx", "/tts/piper/$voice/$voice.int8.onnx"), runtime.requests.map { it.model })
    }
}
