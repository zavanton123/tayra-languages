package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.SentenceTranslationCache
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CachedSentenceTranslatorTest {
    private val language = Language(id = 1, name = "Portuguese")

    private class FakeStore : SentenceTranslationCache {
        val rows = HashMap<Pair<String, String>, Pair<String, Long>>()
        override suspend fun get(sentence: String, targetLanguage: String, notBefore: Long): String? =
            rows[sentence to targetLanguage]?.takeIf { it.second >= notBefore }?.first
        override suspend fun put(sentence: String, targetLanguage: String, translation: String, createdAt: Long) {
            rows[sentence to targetLanguage] = translation to createdAt
        }
        override suspend fun prune(cutoff: Long) { rows.entries.removeAll { it.value.second < cutoff } }
        override suspend fun clear() = rows.clear()
    }

    private class CountingTranslator(private val answer: String?) : SentenceTranslator {
        var calls = 0
        override suspend fun translate(text: String, language: Language): String? { calls++; return answer }
    }

    @Test
    fun answersFromMemoryAndFromTheStore() = runTest {
        val store = FakeStore()
        val inner = CountingTranslator("They say")
        val first = CachedSentenceTranslator(inner, store, targetLanguage = { "en" }, minIntervalMs = 0)
        assertEquals("They say", first.translate("Dizem", language))
        assertEquals("They say", first.translate("Dizem", language))
        assertEquals(1, inner.calls, "second call served from memory")

        val second = CachedSentenceTranslator(inner, store, targetLanguage = { "en" }, minIntervalMs = 0)
        assertEquals("They say", second.translate("Dizem", language))
        assertEquals(1, inner.calls, "a new instance reads the stored translation")

        assertEquals("They say", second.translate("Dizem", Language(id = 2, name = "Spanish")))
        assertEquals(1, inner.calls, "the key is the sentence and target language only")
    }

    @Test
    fun targetLanguageIsPartOfTheKey() = runTest {
        val store = FakeStore()
        val inner = CountingTranslator("x")
        var target = "en"
        val translator = CachedSentenceTranslator(inner, store, targetLanguage = { target }, minIntervalMs = 0)
        translator.translate("Dizem", language)
        target = "ru"
        translator.translate("Dizem", language)
        assertEquals(2, inner.calls)
    }

    @Test
    fun storedTranslationsExpireAfterADay() = runTest {
        val store = FakeStore()
        store.put("Dizem", "en", "old", createdAt = 0L)
        val inner = CountingTranslator("fresh")
        val translator = CachedSentenceTranslator(inner, store, targetLanguage = { "en" }, minIntervalMs = 0)
        assertEquals("fresh", translator.translate("Dizem", language), "an entry from long ago is ignored")
        assertEquals(1, inner.calls)
        assertNull(store.rows["Dizem" to "en"]?.takeIf { it.second == 0L }, "the stale row was replaced")
    }

    @Test
    fun clearForgetsEverything() = runTest {
        val store = FakeStore()
        val inner = CountingTranslator("x")
        val translator = CachedSentenceTranslator(inner, store, targetLanguage = { "en" }, minIntervalMs = 0)
        translator.translate("Dizem", language)
        translator.clearCache()
        translator.translate("Dizem", language)
        assertEquals(2, inner.calls)
        assertEquals(1, store.rows.size)
    }

    @Test
    fun failuresAreNotStored() = runTest {
        val store = FakeStore()
        val inner = CountingTranslator(null)
        val translator = CachedSentenceTranslator(inner, store, targetLanguage = { "en" }, minIntervalMs = 0)
        assertNull(translator.translate("Dizem", language))
        assertEquals(0, store.rows.size)
    }
}
