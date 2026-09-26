package com.tayra.languages.dictionary

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.dictionary.DictionaryAssets
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.shared.resources.Res

/** Reads dictionary files bundled as Compose resources under files/dictionaries. */
class BundledDictionaryAssets : DictionaryAssets {
    override suspend fun readJson(dictionary: DictionaryId): String? = try {
        Res.readBytes("files/dictionaries/${dictionary.name}.json").decodeToString()
    } catch (e: Exception) {
        Logger.w(e) { "No bundled dictionary ${dictionary.name}" }
        null
    }
}
