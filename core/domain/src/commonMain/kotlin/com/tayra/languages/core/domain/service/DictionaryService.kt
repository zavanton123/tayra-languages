package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.dictionary.DictionaryAssets
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.DictionarySense
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.repository.DictionaryForm
import com.tayra.languages.core.domain.repository.DictionaryRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Imports bundled dictionaries into the database and answers lookups from it. */
class DictionaryService(
    private val assets: DictionaryAssets,
    private val repository: DictionaryRepository,
) : OfflineDictionary {

    /** Imports every bundled dictionary whose data is missing or in an older format. */
    suspend fun importIfNeeded() {
        for (dictionary in DictionaryId.bundled) {
            if (repository.importedFormat(dictionary) == FORMAT) continue
            val json = assets.readJson(dictionary) ?: continue
            val file = parser.decodeFromString<DictionaryFile>(json)
            repository.replace(
                dictionary,
                FORMAT,
                file.entries.map { e ->
                    DictionaryEntry(e.word, e.pos, e.ipa, e.senses.map { s -> DictionarySense(s.glosses, s.tags) })
                },
                file.forms.map { DictionaryForm(it.form, it.lemma, it.generated) },
            )
        }
    }

    override suspend fun isAvailable(dictionary: DictionaryId): Boolean = repository.importedFormat(dictionary) != null

    override suspend fun lookup(dictionary: DictionaryId, text: String): DictionaryLookup {
        val word = text.trim()
        val wordLc = word.lowercase()
        if (wordLc.isEmpty()) return DictionaryLookup.EMPTY
        val own = repository.entries(dictionary, wordLc).sortedBy { caseRank(it.word, word) }
        // Headwords that differ only in case are usually a common noun and a name ("word", "Word");
        // the one whose case matches the clicked word wins, so the name does not become the parent.
        val lemmas = repository.lemmas(dictionary, wordLc)
            .filter { !it.equals(wordLc, ignoreCase = true) }
            .groupBy { it.lowercase() }
            .map { (_, variants) -> variants.minBy { caseRank(it, word) } }
        val inherited = lemmas.flatMap { lemma ->
            repository.entries(dictionary, lemma.lowercase())
                .filter { it.word == lemma }
                .map { it.copy(isOwnEntry = false) }
        }
        return DictionaryLookup(own + inherited, lemmas.filter { lemma -> inherited.any { it.word == lemma } })
    }

    /** 0 for the same case as the clicked word, 1 for the same initial case, 2 otherwise. */
    private fun caseRank(candidate: String, word: String): Int = when {
        candidate == word -> 0
        candidate.first().isUpperCase() == word.first().isUpperCase() -> 1
        else -> 2
    }

    @Serializable
    private data class DictionaryFile(val format: Int = 1, val entries: List<EntryDto>, val forms: List<FormDto>)

    @Serializable
    private data class EntryDto(val word: String, val pos: String, val ipa: String? = null, val senses: List<SenseDto>)

    @Serializable
    private data class SenseDto(val glosses: List<String>, val tags: List<String> = emptyList())

    @Serializable
    private data class FormDto(val form: String, val lemma: String, val generated: Boolean = false)

    companion object {
        /** Bumped when the bundled files change so existing installs re-import. */
        const val FORMAT = 1
        private val parser = Json { ignoreUnknownKeys = true }
    }
}
