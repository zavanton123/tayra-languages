package com.tayra.languages.dictionary

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.dictionary.DictionaryAssets
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.shared.resources.Res

/**
 * Gzip-compressed dictionary files bundled as Compose resources under files/dictionaries. The
 * extension is .gzip rather than .gz because the Android asset merger strips ".gz" from asset
 * names, which would break the resource path.
 */
class BundledDictionaryAssets : DictionaryAssets {
    override suspend fun readBytes(dictionary: DictionaryId): ByteArray? = try {
        Res.readBytes(path(dictionary))
    } catch (e: Exception) {
        Logger.w(e) { "No bundled dictionary ${dictionary.name}" }
        null
    }

    override fun uri(dictionary: DictionaryId): String? = try {
        Res.getUri(path(dictionary))
    } catch (e: Exception) {
        Logger.w(e) { "No bundled dictionary ${dictionary.name}" }
        null
    }

    private fun path(dictionary: DictionaryId) = "files/dictionaries/${dictionary.name}.sqlite.gzip"
}
