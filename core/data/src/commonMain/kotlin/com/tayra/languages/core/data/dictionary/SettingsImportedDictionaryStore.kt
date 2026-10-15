package com.tayra.languages.core.data.dictionary

import com.russhwolf.settings.Settings
import com.tayra.languages.core.domain.dictionary.ImportedDictionary
import com.tayra.languages.core.domain.dictionary.ImportedDictionaryStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** The imported dictionaries' list, as JSON in the app's settings; their packs are files of their own. */
class SettingsImportedDictionaryStore(private val settings: Settings) : ImportedDictionaryStore {
    override suspend fun list(): List<ImportedDictionary> =
        settings.getStringOrNull(KEY)?.let { text ->
            runCatching { json.decodeFromString(serializer, text) }.getOrDefault(emptyList())
        }.orEmpty().map { ImportedDictionary(it.key, it.title, it.revision, it.sourceLanguage, it.targetLanguage, it.sizeBytes) }

    override suspend fun save(dictionaries: List<ImportedDictionary>) {
        settings.putString(KEY, json.encodeToString(serializer, dictionaries.map { Stored(it.key, it.title, it.revision, it.sourceLanguage, it.targetLanguage, it.sizeBytes) }))
    }

    @Serializable
    private data class Stored(val key: String, val title: String, val revision: String, val sourceLanguage: String, val targetLanguage: String, val sizeBytes: Long)

    private companion object {
        const val KEY = "imported_dictionaries"
        val json = Json { ignoreUnknownKeys = true }
        val serializer = ListSerializer(Stored.serializer())
    }
}
