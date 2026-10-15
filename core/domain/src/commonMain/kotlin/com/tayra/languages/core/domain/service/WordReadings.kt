package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.morphology.Furigana
import com.tayra.languages.core.domain.morphology.Japanese
import com.tayra.languages.core.domain.settings.SettingsRepository

/**
 * The readings shown above the words of Japanese and Chinese texts: furigana over each kanji of a
 * word, split as Yomitan splits them (an inflected form keeps its okurigana bare), and pinyin over
 * each Chinese character, from an installed dictionary pack of the language (the native language's
 * first). A word no pack reads gets none.
 */
class WordReadings(private val dictionary: OfflineDictionary, private val settings: SettingsRepository) {
    private val cache = LinkedHashMap<String, List<Furigana>>()

    /** The furigana of each of [words] that has a reading, by word. */
    suspend fun of(language: Language, words: Collection<String>): Map<String, List<Furigana>> {
        val code = LanguageCodes.codeFor(language.name)?.takeIf { it in LANGUAGES } ?: return emptyMap()
        val native = settings.current.nativeLanguage
        val packs = DictionaryPacks.all.filter { it.id.sourceLanguage == code && !it.id.isNames }.sortedBy { if (it.id.targetLanguage == native) 0 else 1 }
        val pack = packs.firstOrNull { dictionary.isAvailable(it.id) } ?: return emptyMap()
        val out = HashMap<String, List<Furigana>>()
        for (word in words.distinct()) {
            if (word.none { Japanese.isKanji(it) }) continue
            val key = "${pack.id.name}|$word"
            val furigana = cache[key] ?: run {
                val lookup = dictionary.lookup(pack.id, word)
                val entry = lookup.entries.firstOrNull { it.reading != null && it.isOwnEntry } ?: lookup.entries.firstOrNull { it.reading != null }
                (entry?.let { furiganaOf(code, word, it) } ?: emptyList()).also { remember(key, it) }
            }
            if (furigana.any { it.reading.isNotEmpty() }) out[word] = furigana
        }
        return out
    }

    private fun furiganaOf(code: String, word: String, entry: DictionaryEntry): List<Furigana> {
        val reading = entry.reading!!.substringBefore(", ")
        return when (code) {
            "ja" -> if (entry.word == word) Japanese.distributeFurigana(word, reading) else Japanese.distributeFuriganaInflected(entry.word, reading, word)
            else -> pinyinOf(word, reading)
        }
    }

    /** Pinyin over each character when the reading has a syllable for each, else over the whole word. */
    private fun pinyinOf(word: String, reading: String): List<Furigana> {
        val syllables = reading.split(' ').filter { it.isNotBlank() }
        return if (syllables.size == word.length) word.mapIndexed { i, c -> Furigana(c.toString(), syllables[i]) } else listOf(Furigana(word, reading))
    }

    private fun remember(key: String, value: List<Furigana>) {
        if (cache.size >= MAX_CACHED) cache.remove(cache.keys.first())
        cache[key] = value
    }

    private companion object {
        val LANGUAGES = setOf("ja", "zh")
        const val MAX_CACHED = 5000
    }
}
