package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.dictionary.PackStatus
import com.tayra.languages.core.domain.repository.DictionaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Manages downloadable dictionary packs and answers lookups from the installed ones. */
class DictionaryService(
    private val store: DictionaryPackStore,
    private val repository: DictionaryRepository,
) : OfflineDictionary {

    private val _packs = MutableStateFlow(DictionaryPacks.all.map { PackStatus(it, PackState.NotInstalled) })

    /** Every known pack with its current state, in catalog order. */
    val packs: StateFlow<List<PackStatus>> = _packs.asStateFlow()

    /** Reads which packs are on the device; call once at start. */
    suspend fun refresh() {
        for (pack in DictionaryPacks.all) {
            val current = stateOf(pack.id)
            if (current is PackState.Downloading) continue
            val size = store.installedSize(pack)
            setState(pack.id, if (size != null) PackState.Installed(size) else PackState.NotInstalled)
        }
    }

    /** Downloads a pack; failures end in [PackState.Failed] rather than an exception. */
    suspend fun download(pack: DictionaryPack) {
        if (stateOf(pack.id) is PackState.Downloading) return
        setState(pack.id, PackState.Downloading(null))
        try {
            repository.close(pack.id)
            store.install(pack) { progress -> setState(pack.id, PackState.Downloading(progress)) }
            setState(pack.id, PackState.Installed(store.installedSize(pack) ?: 0))
        } catch (e: Exception) {
            setState(pack.id, PackState.Failed(e.message ?: "Download failed"))
        }
    }

    suspend fun remove(pack: DictionaryPack) {
        repository.close(pack.id)
        store.remove(pack)
        setState(pack.id, PackState.NotInstalled)
    }

    override suspend fun isAvailable(dictionary: DictionaryId): Boolean =
        stateOf(dictionary) is PackState.Installed && repository.isAvailable(dictionary)

    override suspend fun lookup(dictionary: DictionaryId, text: String): DictionaryLookup {
        if (stateOf(dictionary) !is PackState.Installed) return DictionaryLookup.EMPTY
        val word = text.trim()
        val wordLc = lowercase(word, dictionary)
        if (wordLc.isEmpty()) return DictionaryLookup.EMPTY
        // Among entries for one spelling, the one with the most senses is usually the everyday
        // word ("muchacho" the noun before the adjective), so it supplies the suggestion.
        val own = repository.entries(dictionary, wordLc).sortedWith(compareBy({ caseRank(it.word, word) }, { -it.senses.size }))
        // Headwords that differ only in case are usually a common noun and a name ("word", "Word");
        // the one whose case matches the clicked word wins, so the name does not become the parent.
        val lemmas = repository.lemmas(dictionary, wordLc)
            .filter { !it.equals(wordLc, ignoreCase = true) }
            .groupBy { it.lowercase() }
            .map { (_, variants) -> variants.minBy { caseRank(it, word) } }
        val inherited = lemmas.flatMap { lemma ->
            repository.entries(dictionary, lowercase(lemma, dictionary))
                .filter { it.word == lemma }
                .sortedByDescending { it.senses.size }
                .map { it.copy(isOwnEntry = false) }
        }
        return DictionaryLookup(own + inherited, lemmas.filter { lemma -> inherited.any { it.word == lemma } })
    }

    private fun stateOf(id: DictionaryId): PackState = _packs.value.firstOrNull { it.pack.id == id }?.state ?: PackState.NotInstalled

    private fun setState(id: DictionaryId, state: PackState) {
        _packs.update { list -> list.map { if (it.pack.id == id) it.copy(state = state) else it } }
    }

    /** Packs store keys lowercased the way tools/build_dictionary.py does: Turkish keeps its dotted and dotless i apart. */
    private fun lowercase(word: String, dictionary: DictionaryId): String =
        (if (dictionary.sourceLanguage == "tr") word.replace('I', 'ı').replace('İ', 'i') else word).lowercase()

    /** 0 for the same case as the clicked word, 1 for the same initial case, 2 otherwise. */
    private fun caseRank(candidate: String, word: String): Int = when {
        candidate == word -> 0
        candidate.first().isUpperCase() == word.first().isUpperCase() -> 1
        else -> 2
    }
}
