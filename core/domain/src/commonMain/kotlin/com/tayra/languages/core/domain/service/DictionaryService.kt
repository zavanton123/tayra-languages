package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.dictionary.PackStatus
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.morphology.LanguageMorphology
import com.tayra.languages.core.domain.morphology.Morphology
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
        // Among entries for one spelling, nouns and verbs are usually the everyday word ("maison"
        // the noun before the adjective "homemade"), and among those the one with more senses.
        // An entry in another case ("CATS" for "cats"), or one whose every sense is obsolete
        // (the archaic noun "went"), is not the word the reader met: it ranks after the lemmas'
        // entries and does not stop an inflected form from getting its parent.
        val (own, otherCase) = repository.entries(dictionary, wordLc)
            .sortedWith(compareBy({ caseRank(it.word, word) }, { posRank(it.pos) }, { -it.senses.size }))
            .partition { caseRank(it.word, word) < 2 && it.senses.any { sense -> sense.tags.none { tag -> tag in DATED_TAGS } } }
        // Headwords that differ only in case are usually a common noun and a name ("word", "Word");
        // the one whose case matches the clicked word wins, so the name does not become the parent.
        val lemmas = repository.lemmas(dictionary, wordLc)
            .filter { !it.equals(wordLc, ignoreCase = true) }
            .groupBy { it.lowercase() }
            .map { (_, variants) -> variants.minBy { caseRank(it, word) } }
        val inherited = lemmas.flatMap { lemma ->
            repository.entries(dictionary, lowercase(lemma, dictionary))
                .filter { it.word == lemma }
                .sortedWith(compareBy({ posRank(it.pos) }, { -it.senses.size }))
                .map { it.copy(isOwnEntry = false) }
        }
        val lookup = DictionaryLookup(
            own + inherited + otherCase.map { it.copy(isOwnEntry = false) },
            lemmas.filter { lemma -> inherited.any { it.word == lemma } },
        )
        if (own.isNotEmpty() || lookup.lemmas.isNotEmpty()) return lookup
        val morphology = Morphology.forLanguage(dictionary.sourceLanguage) ?: return lookup
        return deinflected(dictionary, word, wordLc, morphology)?.let { found ->
            DictionaryLookup(found.entries + lookup.entries, found.lemmas)
        } ?: lookup
    }

    /**
     * What the pack has for the word's other spellings and the words it may be an inflection of,
     * by the language's Yomitan rules, when it has nothing under the word itself: another
     * spelling ("еще" for "ещё") counts as the word, an inflection's dictionary word ("walk" for
     * "walked", when the pack's forms lack it) as its lemma.
     */
    private suspend fun deinflected(dictionary: DictionaryId, word: String, wordLc: String, morphology: LanguageMorphology): DictionaryLookup? {
        val candidates = morphology.candidates(word).filter { it.text.isNotBlank() && lowercase(it.text, dictionary) != wordLc }
        if (candidates.isEmpty()) return null
        val keys = candidates.map { lowercase(it.text, dictionary) }.distinct().take(MAX_LOOKUPS)
        val found = repository.entries(dictionary, keys)
        val own = mutableListOf<DictionaryEntry>()
        val inherited = LinkedHashMap<String, List<DictionaryEntry>>()
        for (candidate in candidates) {
            val key = lowercase(candidate.text, dictionary)
            val entries = found[key].orEmpty().filter { morphology.accepts(candidate.conditions, it.pos) }
            if (candidate.transforms.isEmpty()) {
                // A spelling of the word itself, and the lemmas the pack lists for that spelling.
                entries.filter { it !in own }.let { own += it }
                if (own.isEmpty()) {
                    for (lemma in repository.lemmas(dictionary, key)) {
                        if (lemma !in inherited) inherited[lemma] = repository.entries(dictionary, lowercase(lemma, dictionary)).filter { it.word == lemma }
                    }
                }
            } else {
                for ((lemma, group) in entries.groupBy { it.word }) if (lemma !in inherited) inherited[lemma] = group
            }
        }
        if (own.isEmpty() && inherited.values.all { it.isEmpty() }) return null
        val lemmas = if (own.isEmpty()) inherited.filterValues { it.isNotEmpty() }.keys.toList() else emptyList()
        val entries = own.sortedWith(compareBy({ posRank(it.pos) }, { -it.senses.size })) +
            inherited.values.flatten().sortedWith(compareBy({ posRank(it.pos) }, { -it.senses.size })).map { it.copy(isOwnEntry = false) }
        return DictionaryLookup(entries, lemmas)
    }

    private fun stateOf(id: DictionaryId): PackState = _packs.value.firstOrNull { it.pack.id == id }?.state ?: PackState.NotInstalled

    private fun setState(id: DictionaryId, state: PackState) {
        _packs.update { list -> list.map { if (it.pack.id == id) it.copy(state = state) else it } }
    }

    /** Packs store keys lowercased the way tools/build_dictionary.py does: Turkish keeps its dotted and dotless i apart. */
    private fun lowercase(word: String, dictionary: DictionaryId): String =
        (if (dictionary.sourceLanguage == "tr") word.replace('I', 'ı').replace('İ', 'i') else word).lowercase()

    private val DATED_TAGS = setOf("obsolete", "archaic", "dated", "rare", "historical")

    /** Words looked up at most for one clicked word's spellings and dictionary forms. */
    private val MAX_LOOKUPS = 2000

    private fun posRank(pos: String): Int = when (pos) {
        "noun", "verb" -> 0
        "adj", "adv" -> 1
        else -> 2
    }

    /** 0 for the same case as the clicked word, 1 for the same initial case, 2 otherwise. */
    private fun caseRank(candidate: String, word: String): Int = when {
        candidate == word -> 0
        candidate.first().isUpperCase() == word.first().isUpperCase() -> 1
        else -> 2
    }
}
