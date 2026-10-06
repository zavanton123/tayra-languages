package com.tayra.languages.core.domain.frequency

import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.parse.lowercase
import com.tayra.languages.core.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A word known through the vocabulary level, with the forms of it saved as known too. */
data class KnownWord(val text: String, val textLc: String, val forms: List<String>)

/** What setting a vocabulary level changed. */
data class LevelChange(val from: Int, val to: Int, val added: Int, val removed: Int)

/** The vocabulary level of each language and the known terms it saved. */
interface VocabularyLevelRepository {
    /** Null while no level was ever chosen; 0 once the reader said they are just starting out. */
    fun observeLevel(languageId: Long): Flow<Int?>
    suspend fun level(languageId: Long): Int?

    /**
     * Makes [level] the language's level. Each of [known] and its forms is saved as a known term
     * unless the language has a term for it already (left as it is), each form linked to its
     * word. Terms an earlier, higher level saved that are not among [known] and are still known
     * are deleted; ones the reader has given another status since are kept.
     */
    suspend fun setLevel(languageId: Long, level: Int, known: List<KnownWord>): LevelChange
}

/**
 * The vocabulary level: the reader says they know the most common words of the language up to a
 * rank, and those words are saved as known.
 */
class VocabularyLevelService(
    private val lists: FrequencyLists,
    private val levels: VocabularyLevelRepository,
    private val languages: LanguageRepository,
) {
    /** 0 while no level is set. */
    fun observeLevel(languageId: Long): Flow<Int> = levels.observeLevel(languageId).map { it ?: 0 }

    /** Null while the reader has never chosen a level for the language. */
    fun observeChosenLevel(languageId: Long): Flow<Int?> = levels.observeLevel(languageId)

    /** Whether to ask the reader for their level: the language has a frequency list and no level was chosen yet. */
    suspend fun needsLevel(languageId: Long): Boolean = levels.level(languageId) == null && list(languageId) != null

    /** The frequency list of the language with [languageId], or null when the app has none. */
    suspend fun list(languageId: Long): FrequencyList? =
        languages.getById(languageId)?.let { LanguageCodes.codeFor(it.name) }?.let { lists.list(it) }

    /** Sets the level to [level] (0 for none); null when the language has no frequency list. */
    suspend fun setLevel(languageId: Long, level: Int): LevelChange? {
        val language = languages.getById(languageId) ?: return null
        val list = LanguageCodes.codeFor(language.name)?.let { lists.list(it) } ?: return null
        val known = list.words.take(level.coerceIn(0, list.words.size)).map { word ->
            val textLc = language.lowercase(word.word)
            KnownWord(word.word, textLc, word.forms.filter { it != textLc })
        }
        return levels.setLevel(languageId, level.coerceIn(0, list.words.size), known)
    }

    companion object {
        /** The levels offered in the picker: every hundred to 500, then wider steps, up to the list's length. */
        fun choices(wordCount: Int): List<Int> {
            val steps = listOf(0, 100, 200, 300, 400, 500, 700, 1000, 1500, 2000, 2500, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10_000)
            val within = steps.filter { it <= wordCount }
            return if (wordCount in within) within else within + wordCount
        }
    }
}
