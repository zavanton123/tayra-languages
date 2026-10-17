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
import com.tayra.languages.core.domain.dictionary.KanjiInfo
import com.tayra.languages.core.domain.dictionary.WordFrequency
import com.tayra.languages.core.domain.dictionary.ImportedDictionaries
import com.tayra.languages.core.domain.dictionary.ImportedDictionary
import com.tayra.languages.core.domain.dictionary.ImportedDictionaryInfo
import com.tayra.languages.core.domain.dictionary.DictionaryImporter
import com.tayra.languages.core.domain.morphology.Chinese
import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.domain.morphology.ChineseScripts
import com.tayra.languages.core.domain.morphology.Japanese
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
    /** The dictionaries the user imported, looked up with the pack of their language pair. */
    private val imported: ImportedDictionaries? = null,
    private val importer: DictionaryImporter? = null,
) : OfflineDictionary {

    private val _packs = MutableStateFlow(DictionaryPacks.everything.map { PackStatus(it, PackState.NotInstalled) })

    /** Every known pack with its current state, in catalog order, the imported ones last. */
    val packs: StateFlow<List<PackStatus>> = _packs.asStateFlow()

    /** Whether dictionaries can be imported here. */
    val canImport: Boolean get() = imported != null && importer != null

    /** Reads which packs are on the device; call once at start. */
    suspend fun refresh() {
        imported?.let { registry ->
            registry.load()
            val known = _packs.value.map { it.pack.id }.toSet()
            val added = registry.all.value.filter { it.id !in known }.map { PackStatus(it.pack, PackState.NotInstalled) }
            _packs.update { it + added }
        }
        for (pack in _packs.value.map { it.pack }) {
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
        if (pack.id.imported != null) {
            imported?.remove(pack.id)
            _packs.update { list -> list.filter { it.pack.id != pack.id } }
        } else {
            setState(pack.id, PackState.NotInstalled)
        }
    }

    /** What the dictionary in [file] (Yomitan or MDict) says of itself; throws when it is not one. */
    suspend fun inspect(file: ByteArray): ImportedDictionaryInfo = (importer ?: error("Importing dictionaries is not supported here")).inspect(file)

    /**
     * Imports the dictionary in [file] as words of [sourceLanguage] explained in [targetLanguage];
     * from then on lookups in that pair's pack show its entries too. Throws on failure.
     */
    suspend fun import(file: ByteArray, sourceLanguage: String, targetLanguage: String, onProgress: (Float) -> Unit = {}): ImportedDictionary {
        val registry = imported ?: error("Importing dictionaries is not supported here")
        val converter = importer ?: error("Importing dictionaries is not supported here")
        val info = converter.inspect(file)
        val sqlite = converter.convert(file, sourceLanguage, targetLanguage, onProgress)
        val dictionary = ImportedDictionary(
            ImportedDictionary.keyFor(info.title, registry.all.value.map { it.key }), info.title, info.revision,
            sourceLanguage, targetLanguage, sqlite.size.toLong(),
        )
        store.installFile(dictionary.pack, sqlite)
        registry.add(dictionary)
        _packs.update { it + PackStatus(dictionary.pack, PackState.Installed(store.installedSize(dictionary.pack) ?: sqlite.size.toLong())) }
        return dictionary
    }

    /** Whether [dictionary] answers lookups: it is installed, or another dictionary of its pair that lookups in it show is. */
    override suspend fun isAvailable(dictionary: DictionaryId): Boolean =
        stateOf(dictionary) is PackState.Installed && repository.isAvailable(dictionary) ||
            !dictionary.glossary && dictionary.imported == null && companionsOf(dictionary).any { repository.isAvailable(it.id) }

    override suspend fun lookup(dictionary: DictionaryId, text: String): DictionaryLookup {
        val own = lookupIn(dictionary, text)
        if (dictionary.glossary || dictionary.imported != null) return own
        val pair = companionsOf(dictionary)
        // The pair's other editions and imported dictionaries add their entries after the pack's own,
        // each named, and so does the language's pack of names when it is installed.
        val names = DictionaryId(dictionary.sourceLanguage, DictionaryId.NAMES).takeIf { it != dictionary && stateOf(it) is PackState.Installed }
        val extra = (pair.map { pack -> sourceTitle(pack) to lookupIn(pack.id, text) } + listOfNotNull(names?.let { NAMES_TITLE to lookupIn(it, text) }))
            .filter { !it.second.isEmpty }
        val merged = if (extra.isEmpty()) own else DictionaryLookup(
            own.entries + extra.flatMap { (title, found) -> found.entries.map { it.copy(dictionary = title) } },
            (own.lemmas + extra.flatMap { it.second.lemmas }).distinct(),
        )
        return if (merged.isEmpty || pair.isEmpty()) merged else merged.copy(entries = withFrequencies(merged.entries, pair))
    }

    /** The installed packs of [dictionary]'s language pair besides it: its other editions, then the imported dictionaries. */
    private fun companionsOf(dictionary: DictionaryId): List<DictionaryPack> = _packs.value.filter { status ->
        val id = status.pack.id
        id != dictionary && (id.imported != null || id.edition != null) && !id.glossary &&
            id.sourceLanguage == dictionary.sourceLanguage && id.targetLanguage == dictionary.targetLanguage && status.state is PackState.Installed
    }.map { it.pack }.sortedBy { if (it.id.imported == null) 0 else 1 }

    /** How a pack's entries are labelled among another pack's: an imported dictionary by its title, an edition by its source. */
    private fun sourceTitle(pack: DictionaryPack): String = pack.id.edition?.let(DictionaryPacks::editionName) ?: pack.title

    /** Each entry with the frequencies the imported dictionaries give its word, in its reading where they say which. */
    private suspend fun withFrequencies(entries: List<DictionaryEntry>, packs: List<DictionaryPack>): List<DictionaryEntry> {
        val words = entries.map { it.word }.distinct()
        val given = packs.mapNotNull { pack ->
            val found = repository.frequencies(pack.id, words)
            if (found.isEmpty()) null else Triple(pack.title, repository.meta(pack.id, "frequency_mode") == "rank-based", found)
        }
        if (given.isEmpty()) return entries
        return entries.map { entry ->
            val readings = entry.reading?.split(", ").orEmpty()
            val frequencies = given.flatMap { (title, rankBased, found) ->
                found[entry.word].orEmpty().filter { it.reading == null || it.reading in readings || it.reading == entry.word }
                    .map { WordFrequency(title, it.value, it.display, rankBased) }.distinct()
            }
            if (frequencies.isEmpty()) entry else entry.copy(frequencies = frequencies)
        }
    }

    private suspend fun lookupIn(dictionary: DictionaryId, text: String): DictionaryLookup {
        val found = find(dictionary, text).let { found ->
            // Text read in traditional characters may use Taiwan's variants (為) where the pack files
            // another (爲); every pack lists the simplified spelling as a form of its word.
            if (!found.isEmpty || dictionary.sourceLanguage != "zh" || !ChineseScripts.isReady) found
            else ChineseScripts.convert(text, ChineseScript.SIMPLIFIED).takeIf { it != text }?.let { find(dictionary, it) } ?: found
        }
        if (found.isEmpty || dictionary.sourceLanguage !in READING_LANGUAGES) return found
        val words = found.entries.map { it.word }
        val readings = repository.readings(dictionary, words)
        val pitch = if (dictionary.sourceLanguage == "ja") repository.pitch(dictionary, words) else emptyMap()
        val read = found.entries.map { it.copy(reading = it.reading ?: readings[it.word], pitch = pitch[it.word].orEmpty()) }
        return found.copy(entries = if (dictionary.sourceLanguage == "ja") byReading(read, text) else read)
    }

    /**
     * A Japanese word typed as one of its readings ("ぶつ") shows the entries read that way, as Yomitan
     * does, rather than every entry of the word it found (打つ, read うつ as well).
     */
    private fun byReading(entries: List<DictionaryEntry>, text: String): List<DictionaryEntry> {
        // Typed as the lookup reads it: half-width kana and romaji as kana ("ｳﾂ", "utsu" as うつ).
        val typed = Japanese.katakanaToHiragana(Japanese.alphabeticToKana(Japanese.halfWidthKanaToFullWidth(Japanese.fullWidthAlphanumericToNormal(text.trim()).lowercase())))
        if (typed.isEmpty() || !typed.all { Japanese.isKana(it) || it == 'ー' }) return entries
        fun readsAs(entry: DictionaryEntry) = entry.word == text.trim() ||
            entry.reading?.split(", ")?.any { Japanese.katakanaToHiragana(it) == typed } == true
        return if (entries.any(::readsAs)) entries.filter(::readsAs) else entries
    }

    override suspend fun kanji(dictionary: DictionaryId, text: String): List<KanjiInfo> {
        if (dictionary.sourceLanguage != "ja") return emptyList()
        val characters = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            // Rare kanji lie outside the Basic Multilingual Plane, as surrogate pairs.
            val length = if (text[i].isHighSurrogate() && i + 1 < text.length) 2 else 1
            if (Japanese.isKanji(text[i])) characters += text.substring(i, i + length)
            i += length
        }
        if (characters.isEmpty()) return emptyList()
        val wanted = characters.distinct().take(MAX_KANJI)
        // The pack's own kanji first; an imported kanji dictionary gives those it lacks.
        val found = LinkedHashMap<String, KanjiInfo>()
        val sources = listOf(dictionary).filter { stateOf(it) is PackState.Installed } + companionsOf(dictionary).map { it.id }
        for (source in sources) {
            val missing = wanted.filter { it !in found }
            if (missing.isEmpty()) break
            repository.kanji(source, missing).forEach { found.getOrPut(it.character) { it } }
        }
        return wanted.mapNotNull { found[it] }
    }

    private suspend fun find(dictionary: DictionaryId, text: String): DictionaryLookup {
        if (stateOf(dictionary) !is PackState.Installed) return DictionaryLookup.EMPTY
        // Chinese typed as pinyin is looked up by the reading, normalised as the packs store it.
        val word = if (dictionary.sourceLanguage == "zh" && !Chinese.isPartlyChinese(text)) Chinese.normalizePinyin(text.trim()) else text.trim()
        val wordLc = lowercase(word, dictionary)
        if (wordLc.isEmpty()) return DictionaryLookup.EMPTY
        // Among entries for one spelling, nouns and verbs are usually the everyday word ("maison"
        // the noun before the adjective "homemade"), and among those the one with more senses; a word
        // the pack marks as common (JMdict's, for Japanese) comes before the others.
        // An entry in another case ("CATS" for "cats"), or one whose every sense is obsolete
        // (the archaic noun "went"), is not the word the reader met: it ranks after the lemmas'
        // entries and does not stop an inflected form from getting its parent.
        val (own, otherCase) = repository.entries(dictionary, wordLc)
            .sortedWith(compareBy({ caseRank(it.word, word) }, { !it.common }, { posRank(it.pos) }, { -it.senses.size }))
            .partition { caseRank(it.word, word) < 2 && it.senses.any { sense -> sense.tags.none { tag -> tag in DATED_TAGS } } }
        // Headwords that differ only in case are usually a common noun and a name ("word", "Word");
        // the one whose case matches the clicked word wins, so the name does not become the parent.
        val lemmas = formLemmas(dictionary, wordLc)
            .filter { !it.equals(wordLc, ignoreCase = true) }
            .groupBy { it.lowercase() }
            .map { (_, variants) -> variants.minBy { caseRank(it, word) } }
        val inherited = lemmas.flatMap { lemma ->
            repository.entries(dictionary, lowercase(lemma, dictionary))
                .filter { it.word == lemma }
                .sortedWith(compareBy({ !it.common }, { posRank(it.pos) }, { -it.senses.size }))
                .map { it.copy(isOwnEntry = false) }
        }
        val lookup = DictionaryLookup(
            own + inherited + otherCase.map { it.copy(isOwnEntry = false) },
            lemmas.filter { lemma -> inherited.any { it.word == lemma } },
        )
        if (own.isNotEmpty() || lookup.lemmas.isNotEmpty()) return lookup
        val morphology = Morphology.forLanguage(dictionary.sourceLanguage) ?: return lookup
        deinflected(dictionary, word, wordLc, morphology)?.let { found ->
            return DictionaryLookup(found.entries + lookup.entries, found.lemmas)
        }
        if (dictionary.sourceLanguage in PARTICLE_LANGUAGES) beginning(dictionary, word, morphology)?.let { return it }
        return lookup
    }

    /**
     * A word written with its particles ("학교에서", at school) is looked up by its beginnings, the
     * longest first, as Yomitan scans text: the first one the pack has, or that inflects a word it
     * has, is the word's lemma.
     */
    private suspend fun beginning(dictionary: DictionaryId, word: String, morphology: LanguageMorphology): DictionaryLookup? {
        for (end in word.length - 1 downTo 1) {
            val prefix = word.substring(0, end)
            val prefixLc = lowercase(prefix, dictionary)
            val own = repository.entries(dictionary, prefixLc)
            val found = if (own.isNotEmpty()) DictionaryLookup(own, emptyList()) else deinflected(dictionary, prefix, prefixLc, morphology)
            if (found != null && !found.isEmpty) {
                val entries = found.entries.map { it.copy(isOwnEntry = false) }
                return DictionaryLookup(entries, entries.map { it.word }.distinct())
            }
        }
        return null
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
                    for (lemma in formLemmas(dictionary, key)) {
                        if (lemma !in inherited) inherited[lemma] = repository.entries(dictionary, lowercase(lemma, dictionary)).filter { it.word == lemma }
                    }
                }
            } else {
                // The candidates come nearest first, so a lemma keeps its shortest path of inflections.
                val inflection = morphology.inflectionNames(candidate.transforms)
                for ((lemma, group) in entries.groupBy { it.word }) {
                    if (lemma !in inherited) inherited[lemma] = group.map { it.copy(inflection = inflection) }
                }
            }
        }
        if (own.isEmpty() && inherited.values.all { it.isEmpty() }) return null
        val lemmas = if (own.isEmpty()) inherited.filterValues { it.isNotEmpty() }.keys.toList() else emptyList()
        val entries = own.sortedWith(compareBy({ !it.common }, { posRank(it.pos) }, { -it.senses.size })) +
            inherited.values.flatten().sortedWith(compareBy({ !it.common }, { posRank(it.pos) }, { -it.senses.size })).map { it.copy(isOwnEntry = false) }
        return DictionaryLookup(entries, lemmas)
    }

    /**
     * The lemmas [formLc] is a form of. A spelling that dozens of lemmas list is no inflection but a
     * word their tables name beside the forms: the auxiliary of every German verb ("haben"), the
     * pronouns of a Latvian conjugation, a Vietnamese classifier. Such links are left out; a kana
     * reading or a toneless pinyin, which many words share, is a form on purpose.
     */
    private suspend fun formLemmas(dictionary: DictionaryId, formLc: String): List<String> =
        repository.lemmas(dictionary, formLc).takeIf { it.size <= MAX_LEMMAS_PER_FORM || dictionary.sourceLanguage in SHARED_FORM_LANGUAGES }.orEmpty()

    private fun stateOf(id: DictionaryId): PackState = _packs.value.firstOrNull { it.pack.id == id }?.state ?: PackState.NotInstalled

    private fun setState(id: DictionaryId, state: PackState) {
        _packs.update { list -> list.map { if (it.pack.id == id) it.copy(state = state) else it } }
    }

    /** Packs store keys lowercased the way tools/build_dictionary.py does: Turkish keeps its dotted and dotless i apart. */
    private fun lowercase(word: String, dictionary: DictionaryId): String =
        (if (dictionary.sourceLanguage == "tr") word.replace('I', 'ı').replace('İ', 'i') else word).lowercase()

    private val DATED_TAGS = setOf("obsolete", "archaic", "dated", "rare", "historical")

    /** Languages whose packs give headwords a reading (kana, pinyin). */
    private val READING_LANGUAGES = setOf("ja", "zh")
    /** How the entries of a pack of names are labelled, by its source. */
    private val NAMES_TITLE = "JMnedict"

    /** More would push the meanings out of the pane for a long phrase. */
    private val MAX_KANJI = 8

    /** Languages whose words carry their particles and endings joined on, so a word's beginning may be the dictionary word. */
    private val PARTICLE_LANGUAGES = setOf("ko")

    /** More lemmas than any real inflection is shared by; tools/build_dictionary.py drops such forms too. */
    private val MAX_LEMMAS_PER_FORM = 30

    /** Languages whose packs file readings as forms, many words to one reading. */
    private val SHARED_FORM_LANGUAGES = setOf("ja", "zh", "yue")

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
