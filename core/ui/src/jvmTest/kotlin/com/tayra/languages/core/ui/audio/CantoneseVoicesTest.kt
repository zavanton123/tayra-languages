package com.tayra.languages.core.ui.audio

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Cantonese is read by a Cantonese voice under whichever tag the system lists it, and never by a Mandarin one. */
class CantoneseVoicesTest {
    @Test
    fun theTagsOfCantoneseVoicesAreKnownAndMandarinOnesAreNot() {
        for (tag in listOf("yue", "yue-HK", "yue_HK", "YUE-hk", "zh-HK", "zh_HK", "zh-hk", "zh-Hant-HK", "zh-MO", "zh-yue", "yue-Hant-HK")) {
            assertTrue(CantoneseVoices.matches(tag), tag)
        }
        for (tag in listOf("zh", "zh-CN", "zh_CN", "zh-TW", "zh-Hans", "zh-Hant", "zh-Hant-TW", "cmn-CN", "en-HK", "yuc", "zh-HKX", "")) {
            assertFalse(CantoneseVoices.matches(tag), tag)
        }
        assertTrue(CantoneseVoices.isCantonese("yue"))
        assertFalse(CantoneseVoices.isCantonese("zh"))
        assertFalse(CantoneseVoices.isCantonese(null))
    }

    private val macVoices = listOf(
        "Albert              en_US    # Hello! My name is Albert.",
        "Eddy (Chinese (China mainland)) zh_CN    # 你好！我叫Eddy。",
        "Eddy (Chinese (Taiwan)) zh_TW    # 你好，我叫Eddy。",
        "Meijia              zh_TW    # 你好，我叫美佳。",
        "Sinji               zh_HK    # 你好！我叫善怡。",
        "Tingting            zh_CN    # 你好！我叫婷婷。",
    )

    @Test
    fun macOsReadsWithItsHongKongVoice() {
        assertEquals("Sinji", macCantoneseVoice(macVoices))
        assertEquals("Sinji (Enhanced)", macCantoneseVoice(listOf("Sinji (Enhanced) zh_HK    # 你好！")))
        assertNull(macCantoneseVoice(macVoices.filterNot { "zh_HK" in it }))
        assertNull(macCantoneseVoice(emptyList()))
    }

    @Test
    fun linuxTakesTheLanguageSpeechDispatcherLists() {
        val voices = listOf(
            "                     NAME                 LANGUAGE                  VARIANT",
            "                 Mandarin                       zh                     none",
            "       Chinese (Cantonese)                      yue                     none",
        )
        assertEquals("yue", linuxCantoneseLanguage(voices))
        assertEquals("zh-HK", linuxCantoneseLanguage(listOf("Tracy zh-HK none")))
        assertNull(linuxCantoneseLanguage(voices.filterNot { "Cantonese" in it }))
    }

    @Test
    fun windowsSpeaksCantoneseOnlyWithACantoneseVoice() {
        val script = windowsSpeechCommand("佢's", "yue").last()
        assertTrue("Culture.Name -match '${CantoneseVoices.LOCALES}'" in script, script)
        assertTrue(script.endsWith("if (\$voice) { \$s.SelectVoice(\$voice.VoiceInfo.Name); \$s.Speak('佢''s') }"), script)
    }

    @Test
    fun windowsAsksForTheCantoneseVoiceItWouldUse() {
        val script = windowsCantoneseVoiceCommand().last()
        assertTrue("Culture.Name -match '${CantoneseVoices.LOCALES}'" in script, script)
        assertTrue(script.endsWith("if (\$voice) { \$voice.VoiceInfo.Name }"), script)
    }

    @Test
    fun onlyCantoneseHasToHaveAVoice() = runBlocking {
        for (code in listOf("zh", "de", null)) assertTrue(SpeechSynthesizer().hasVoice(code), "$code")
    }

    @Test
    fun windowsSpeaksOtherLanguagesAsItDid() {
        val start = "Add-Type -AssemblyName System.Speech; \$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
        assertEquals(
            listOf(
                "powershell", "-NoProfile", "-Command",
                start + "\$voice = \$s.GetInstalledVoices() | Where-Object { \$_.VoiceInfo.Culture.TwoLetterISOLanguageName -eq 'zh' } | Select-Object -First 1; " +
                    "if (\$voice) { \$s.SelectVoice(\$voice.VoiceInfo.Name) }; \$s.Speak('你好')",
            ),
            windowsSpeechCommand("你好", "zh"),
        )
        assertEquals(start + "\$s.Speak('hi')", windowsSpeechCommand("hi", null).last())
    }
}
