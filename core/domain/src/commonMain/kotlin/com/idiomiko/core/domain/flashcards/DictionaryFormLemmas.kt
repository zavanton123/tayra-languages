package com.idiomiko.core.domain.flashcards

import com.idiomiko.core.domain.dictionary.DictionaryId
import com.idiomiko.core.domain.dictionary.DictionaryPacks
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.parse.PersianParser
import com.idiomiko.core.domain.repository.DictionaryRepository
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.settings.SettingsRepository

/**
 * The dictionary forms a word is a form of, from the language's installed dictionary, for a card
 * to find the form its sentence holds ("temia" of "temer"). Nothing without an installed pack.
 */
class DictionaryFormLemmas(
    private val languages: LanguageRepository,
    private val dictionaries: DictionaryRepository,
    private val settings: SettingsRepository,
) {
    suspend fun lemmas(languageId: Long, formLc: String): List<String> {
        val code = languages.getById(languageId)?.let { LanguageCodes.codeFor(it.name) } ?: return emptyList()
        val native = settings.current.nativeLanguage.ifBlank { "en" }
        val dictionary = DictionaryPacks.find(code, native)?.id ?: DictionaryId(code, native)
        if (!dictionaries.isAvailable(dictionary)) return emptyList()
        // A spelling dozens of lemmas list is a word their tables name (an auxiliary), no form of any one of them.
        return dictionaries.lemmas(dictionary, if (code == "fa") PersianParser.packForm(formLc) else formLc).takeIf { it.size <= MAX_LEMMAS }.orEmpty()
    }

    private companion object {
        const val MAX_LEMMAS = 30
    }
}
