package com.tayra.languages.core.data.speech

import com.tayra.languages.core.domain.language.LanguageCodes
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The multilingual Kokoro model on phones: its speakers, the languages sherpa-onnx can read, and the requests. */
class KokoroVoicesTest {

    /** The `speaker_names` metadata of sherpa-onnx's kokoro-multi-lang-v1_0 model. */
    private val modelSpeakers = (
        "af_alloy,af_aoede,af_bella,af_heart,af_jessica,af_kore,af_nicole,af_nova,af_river,af_sarah,af_sky,am_adam,am_echo,am_eric," +
            "am_fenrir,am_liam,am_michael,am_onyx,am_puck,am_santa,bf_alice,bf_emma,bf_isabella,bf_lily,bm_daniel,bm_fable,bm_george," +
            "bm_lewis,ef_dora,em_alex,ff_siwis,hf_alpha,hf_beta,hm_omega,hm_psi,if_sara,im_nicola,jf_alpha,jf_gongitsune,jf_nezumi," +
            "jf_tebukuro,jm_kumo,pf_dora,pm_alex,pm_santa,zf_xiaobei,zf_xiaoni,zf_xiaoxiao,zf_xiaoyi,zm_yunjian,zm_yunxi,zm_yunxia," +
            "zm_yunyang,em_santa"
        ).split(',')

    private class FakeRuntime(files: List<String>, val folders: MutableMap<String, List<String>> = mutableMapOf("/tts/kokoro/kokoro-v1.0" to files)) : SherpaRuntime {
        val installed = mutableListOf<Pair<String, String>>()
        val requests = mutableListOf<SherpaRequest>()
        override fun folder(engine: String) = "/tts/$engine"
        override fun list(path: String) = folders[path]
        override suspend fun installArchive(url: String, destination: String, onDownload: (Long, Long) -> Unit, onUnpack: () -> Unit) {
            installed += url to destination
        }
        override suspend fun delete(path: String) = Unit
        override suspend fun synthesize(request: SherpaRequest): ByteArray = ByteArray(0).also { requests += request }
    }

    private val modelFiles = listOf(
        "model.int8.onnx", "voices.bin", "tokens.txt", "espeak-ng-data", "lexicon-us-en.txt", "lexicon-gb-en.txt", "lexicon-zh.txt",
        "date-zh.fst", "phone-zh.fst", "number-zh.fst",
    )

    @Test
    fun everyVoiceButTheSantasPlaysItsSpeakerInTheModel() {
        assertEquals(modelSpeakers.filterNot { it.endsWith("_santa") }.toSet(), KokoroVoices.ALL.map { it.id }.toSet())
        for (voice in KokoroVoices.ALL) assertEquals(modelSpeakers.indexOf(voice.id), voice.speaker, voice.id)
    }

    @Test
    fun eachLanguageLeadsWithItsBestRatedVoice() {
        val first = KokoroVoices.ALL.groupBy { it.language }.mapValues { it.value.first().id }
        assertEquals(
            mapOf("en" to "af_heart", "es" to "ef_dora", "fr" to "ff_siwis", "hi" to "hf_alpha", "it" to "if_sara", "ja" to "jf_alpha", "pt" to "pf_dora", "zh" to "zf_xiaobei"),
            first,
        )
        val mandarin = KokoroVoices.ALL.filter { it.language == "zh" }
        assertEquals(listOf("Xiaobei (Mandarin, female)", "Xiaoni (Mandarin, female)", "Xiaoxiao (Mandarin, female)", "Xiaoyi (Mandarin, female)"), mandarin.take(4).map { it.label })
        assertEquals("Alpha (Hindi, female)", KokoroVoices.ALL.first { it.language == "hi" }.label)
        assertEquals("Alpha (Japanese, female)", KokoroVoices.ALL.first { it.language == "ja" }.label)
    }

    @Test
    fun accentsAreTheEspeakLanguagesOfTheVoices() {
        val accents = KokoroVoices.ALL.groupBy { it.id.take(1) }.mapValues { (_, voices) -> voices.map { it.accent }.toSet() }
        assertEquals(
            mapOf("a" to setOf("en-us"), "b" to setOf("en-gb"), "e" to setOf("es"), "f" to setOf("fr-fr"), "h" to setOf("hi"), "i" to setOf("it"), "j" to setOf("ja"), "p" to setOf("pt-br"), "z" to setOf("cmn")),
            accents,
        )
    }

    @Test
    fun phonesOfferEveryLanguageButJapanese() = runTest {
        val engine = SherpaKokoroEngine(FakeRuntime(modelFiles))
        val counts = listOf("en", "es", "fr", "hi", "it", "ja", "pt", "zh").associateWith { engine.voices(it).size }
        assertEquals(mapOf("en" to 27, "es" to 2, "fr" to 1, "hi" to 4, "it" to 2, "ja" to 0, "pt" to 2, "zh" to 8), counts)
        // The compact model downloaded before is listed after the full one, to remove.
        val (pkg, compact) = engine.packages()
        assertEquals("Kokoro model with 46 voices", pkg.title)
        assertEquals("Kokoro compact model with 46 voices", compact.title)
        assertTrue(!pkg.installed && compact.installed)
        assertEquals(setOf("en", "es", "fr", "it", "pt", "hi", "zh"), pkg.group.split(", ").map { LanguageCodes.codeFor(it) }.toSet())
        assertEquals(listOf("hf_alpha", "hf_beta", "hm_omega", "hm_psi"), engine.packageVoices(pkg.id, "hi").map { it.id })
    }

    @Test
    fun chineseIsReadWithTheLexiconAndNumberRules() = runTest {
        val runtime = FakeRuntime(modelFiles)
        val engine = SherpaKokoroEngine(runtime)
        engine.synthesize("我有3本书。", "zh", "zm_yunxi", 1f)
        engine.synthesize("नमस्ते", "hi", null, 1f)
        val (chinese, hindi) = runtime.requests
        val folder = "/tts/kokoro/kokoro-v1.0"
        assertEquals(50, chinese.speaker)
        assertEquals("cmn", chinese.lang)
        assertEquals("$folder/lexicon-us-en.txt,$folder/lexicon-zh.txt", chinese.lexicon)
        assertEquals("$folder/date-zh.fst,$folder/phone-zh.fst,$folder/number-zh.fst", chinese.ruleFsts)
        assertEquals(31, hindi.speaker)
        assertEquals("hi", hindi.lang)
        assertEquals("", hindi.ruleFsts, "the Chinese number rules would turn other languages' numbers into characters")
        assertTrue(chinese.key != hindi.key, "each reader configuration loads its own engine")
    }

    @Test
    fun onlyTheFullModelIsOfferedAndItSpeaksBeforeACompactOne() = runTest {
        assertEquals(listOf("kokoro-v1.0-full"), SherpaKokoroEngine(FakeRuntime(emptyList(), mutableMapOf())).packages().map { it.id })
        val runtime = FakeRuntime(modelFiles)
        val engine = SherpaKokoroEngine(runtime)
        val full = engine.packages().first()
        engine.installPackage(full.id)
        assertEquals("https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2" to "/tts/kokoro/kokoro-v1.0-full", runtime.installed.single())

        engine.synthesize("Hello.", "en", null, 1f)
        runtime.folders["/tts/kokoro/kokoro-v1.0-full"] = modelFiles.map { if (it == "model.int8.onnx") "model.onnx" else it }
        engine.synthesize("Hello.", "en", null, 1f)
        val (compact, fullModel) = runtime.requests
        assertEquals("/tts/kokoro/kokoro-v1.0/model.int8.onnx", compact.model)
        assertEquals("/tts/kokoro/kokoro-v1.0-full/model.onnx", fullModel.model)
        assertTrue(compact.key != fullModel.key, "switching models loads the other one")
    }
}
