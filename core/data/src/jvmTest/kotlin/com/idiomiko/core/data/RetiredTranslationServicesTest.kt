package com.idiomiko.core.data

import com.russhwolf.settings.MapSettings
import com.idiomiko.core.data.settings.InMemorySecureStore
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.service.TranslationEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** Settings saved for the translation services the app dropped are forgotten, and their engine is replaced. */
class RetiredTranslationServicesTest {

    @Test
    fun aDroppedEngineAndItsKeysAreForgotten() {
        val store = MapSettings()
        val secure = InMemorySecureStore()
        store.putString("translation_engine", "DEEPL")
        store.putString("qwen_model", "qwen-mt-plus")
        store.putString("deepl_api_key", "left in the plain store by an old build")
        secure.put("azure_translator_api_key", "secret")
        secure.put("google_translate_api_key", "kept")

        val settings = SettingsRepositoryImpl(store, secure)

        assertEquals(TranslationEngine.ARGOS, settings.current.translationEngine)
        assertFalse(store.hasKey("qwen_model"))
        assertFalse(store.hasKey("deepl_api_key"))
        assertNull(secure.get("azure_translator_api_key"))
        assertEquals("kept", secure.get("google_translate_api_key"))
    }

    /** MyMemory saved before Argos handed its missing languages over by itself becomes Argos once; chosen again later, it stays. */
    @Test
    fun aSavedMyMemoryBecomesArgosOnce() = runTest {
        val store = MapSettings()
        store.putString("translation_engine", "MYMEMORY")

        val first = SettingsRepositoryImpl(store, InMemorySecureStore())
        assertEquals(TranslationEngine.ARGOS, first.current.translationEngine)
        first.update { it.copy(translationEngine = TranslationEngine.MYMEMORY) }
        assertEquals(TranslationEngine.MYMEMORY, SettingsRepositoryImpl(store, InMemorySecureStore()).current.translationEngine)
    }
}
