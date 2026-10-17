package com.tayra.languages.core.domain.dictionary

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What a dictionary file says of itself (a Yomitan dictionary's index, an MDict file's header); its languages only when its format has them. */
data class ImportedDictionaryInfo(
    val title: String,
    val revision: String,
    val sourceLanguage: String? = null,
    val targetLanguage: String? = null,
    val attribution: String? = null,
    /** "rank-based" when its frequencies are ranks (1 the most common), "occurrence-based" when counts. */
    val frequencyMode: String? = null,
)

/** Turns a dictionary file the user has (a Yomitan zip of JSON banks, an MDict .mdx) into the app's pack file. */
interface DictionaryImporter {
    /** What the dictionary says of itself; throws when [file] is not a dictionary this importer reads. */
    suspend fun inspect(file: ByteArray): ImportedDictionaryInfo

    /** The SQLite pack of the dictionary's terms, read as words of [sourceLanguage] explained in [targetLanguage]. */
    suspend fun convert(file: ByteArray, sourceLanguage: String, targetLanguage: String, onProgress: (Float) -> Unit = {}): ByteArray
}

/** A dictionary the user imported, looked up together with the pack of its language pair. */
data class ImportedDictionary(
    /** Unique among the imported dictionaries: it names the pack's file. */
    val key: String,
    val title: String,
    val revision: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val sizeBytes: Long,
) {
    val id: DictionaryId get() = DictionaryId(sourceLanguage, targetLanguage, imported = key)

    /** The imported dictionary as a pack; it has no download, its file was written on import. */
    val pack: DictionaryPack get() = DictionaryPack(id, title, url = "$IMPORTED_URL/${id.name}.sqlite.gzip", downloadSize = sizeBytes)

    companion object {
        /** Where the web app's cache keeps imported packs; nothing is served there. */
        const val IMPORTED_URL = "https://imported.tayra.invalid"

        /** A file-safe key for a dictionary's title: lowercase letters and digits joined by hyphens. */
        fun keyFor(title: String, taken: Collection<String>): String {
            val base = title.lowercase().map { if (it in 'a'..'z' || it in '0'..'9') it else '-' }.joinToString("")
                .split('-').filter { it.isNotEmpty() }.joinToString("-").take(40).ifEmpty { "dictionary" }
            return generateSequence(1) { it + 1 }.map { if (it == 1) base else "$base-$it" }.first { it !in taken }
        }
    }
}

/** Keeps the list of imported dictionaries. */
interface ImportedDictionaryStore {
    suspend fun list(): List<ImportedDictionary>
    suspend fun save(dictionaries: List<ImportedDictionary>)
}

/** The imported dictionaries, kept in [store] and known in memory so their packs can be opened by id. */
class ImportedDictionaries(private val store: ImportedDictionaryStore) {
    private val _all = MutableStateFlow<List<ImportedDictionary>>(emptyList())
    val all: StateFlow<List<ImportedDictionary>> = _all.asStateFlow()

    suspend fun load() {
        _all.value = store.list()
    }

    fun pack(id: DictionaryId): DictionaryPack? = _all.value.firstOrNull { it.id == id }?.pack

    suspend fun add(dictionary: ImportedDictionary) {
        _all.value = _all.value.filter { it.key != dictionary.key } + dictionary
        store.save(_all.value)
    }

    suspend fun remove(id: DictionaryId) {
        _all.value = _all.value.filter { it.id != id }
        store.save(_all.value)
    }
}
