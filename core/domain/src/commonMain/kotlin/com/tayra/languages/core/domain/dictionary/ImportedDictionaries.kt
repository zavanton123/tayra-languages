package com.tayra.languages.core.domain.dictionary

/** What a Yomitan dictionary's index says of it; its languages only when its format has them. */
data class YomitanDictionaryInfo(
    val title: String,
    val revision: String,
    val sourceLanguage: String? = null,
    val targetLanguage: String? = null,
    val attribution: String? = null,
)

/** Turns a Yomitan dictionary, a zip of JSON banks, into the app's pack file. */
interface YomitanImporter {
    /** The dictionary's index; throws when [zip] is not a Yomitan dictionary. */
    suspend fun inspect(zip: ByteArray): YomitanDictionaryInfo

    /** The SQLite pack of the dictionary's terms, read as words of [sourceLanguage] explained in [targetLanguage]. */
    suspend fun convert(zip: ByteArray, sourceLanguage: String, targetLanguage: String, onProgress: (Float) -> Unit = {}): ByteArray
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
