package com.tayra.languages.core.data.speech

import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.domain.language.LanguageCodes
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Kokoro on the desktop: every language of the model, and what the Python worker is asked for. */
class KokoroSpeechEngineTest {

    private val dir: File = Files.createTempDirectory("kokoro").toFile()
    private val python = ManagedPython(File(dir, "python"))
    private val engine = KokoroSpeechEngine(python, TtsWorker(python), File(dir, "model"))

    @Test
    fun everyLanguageOfTheModelHasItsVoicesBestRatedFirst() = runTest {
        val voices = listOf("en", "es", "fr", "hi", "it", "ja", "pt", "zh").associateWith { engine.packageVoices("kokoro-v1.0", it) }
        assertEquals(mapOf("en" to 27, "es" to 2, "fr" to 1, "hi" to 4, "it" to 2, "ja" to 5, "pt" to 2, "zh" to 8), voices.mapValues { it.value.size })
        assertEquals(listOf("jf_alpha", "jf_gongitsune", "jf_tebukuro", "jf_nezumi", "jm_kumo"), voices.getValue("ja").map { it.id })
        assertEquals(listOf("hf_alpha", "hf_beta", "hm_omega", "hm_psi"), voices.getValue("hi").map { it.id })
        assertEquals("zf_xiaobei", voices.getValue("zh").first().id)
        assertEquals("Xiaobei (Mandarin, female)", voices.getValue("zh").first().name)
        assertEquals("Alpha (Japanese, female)", voices.getValue("ja").first().name)
        assertEquals("Omega (Hindi, male)", voices.getValue("hi")[2].name)
        assertTrue(voices.values.flatten().none { it.id.endsWith("_santa") })

        val pkg = engine.packages().single()
        assertEquals("Kokoro model with 51 voices", pkg.title)
        assertEquals(voices.keys, pkg.group.split(", ").map { LanguageCodes.codeFor(it) }.toSet(), "the setup dialog finds the model by these names")
    }

    @Test
    fun chineseAndJapaneseAskTheWorkerForMisakiTheOthersForEspeak() {
        fun ask(code: String, voice: String? = null) = engine.synthesisFields("text", code, voice, 1.1f)?.toMap()
        assertEquals(
            mapOf(
                "model" to File(dir, "model/kokoro.onnx").absolutePath, "voices" to File(dir, "model/voices.bin").absolutePath,
                "voice" to "zf_xiaobei", "lang" to "zh", "text" to "text", "speed" to "1.1",
            ),
            ask("zh"),
        )
        assertEquals("jm_kumo" to "ja", ask("ja", "jm_kumo")!!.let { it["voice"] to it["lang"] })
        assertEquals("hf_alpha" to "hi", ask("hi", "zf_xiaobei")!!.let { it["voice"] to it["lang"] }, "a voice of another language is not used")
        assertEquals("bm_george" to "en-gb", ask("en", "bm_george")!!.let { it["voice"] to it["lang"] })
        assertEquals("pf_dora" to "pt-br", ask("pt")!!.let { it["voice"] to it["lang"] })
        assertNull(ask("ko"))
    }

    @Test
    fun anInstallationWithoutTheReadersIsNotComplete() {
        fun reply(json: String) = Json.parseToJsonElement(json).jsonObject
        assertEquals("Chinese and Japanese readers not installed yet", missingKokoroReaders(reply("""{"version": "0.6.1", "missing": ["zh", "ja"]}""")))
        assertEquals("Japanese reader not installed yet", missingKokoroReaders(reply("""{"missing": ["ja"]}""")))
        assertNull(missingKokoroReaders(reply("""{"version": "0.6.1", "missing": []}""")))
    }

    @Test
    fun aFreshInstallDownloadsPythonKokoroAndTheReaders() = runTest {
        assertFalse(engine.isReady())
        assertEquals(113_000_000 + python.downloadSize, engine.runtimeDownloadSize())
    }
}
