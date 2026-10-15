package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.frequency.FrequencyLists
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.parse.Lexicon
import com.tayra.languages.core.domain.parse.Lexicons
import com.tayra.languages.core.domain.parse.SegmentingParser
import com.tayra.languages.core.domain.parse.parser
import com.tayra.languages.core.domain.repository.DictionaryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Gives the parsers of languages written without spaces their lexicons: the words of the
 * language's frequency list, with their forms, and every headword and form of its installed
 * dictionary packs. A lexicon is made again when the installed packs change.
 */
class LexiconService(
    private val dictionaries: DictionaryService,
    private val repository: DictionaryRepository,
    private val frequency: FrequencyLists?,
) {
    private val mutex = Mutex()
    private val loadedFrom = mutableMapOf<String, Set<DictionaryId>>()

    /** Makes sure the language's lexicon is loaded before its texts are parsed; does nothing for spaced languages. */
    suspend fun prepare(language: Language) {
        if (runCatching { language.parser }.getOrNull() !is SegmentingParser) return
        val code = LanguageCodes.codeFor(language.name) ?: return
        val packs = dictionaries.packs.value
            .filter { it.pack.id.sourceLanguage == code && !it.pack.id.glossary && !it.pack.id.isNames && it.state is PackState.Installed }
            .map { it.pack.id }.toSet()
        mutex.withLock {
            if (loadedFrom[code] == packs && Lexicons[code] != null) return
            val words = HashSet<String>()
            frequency?.list(code)?.words?.forEach { word ->
                words += word.key
                words += word.forms
            }
            for (pack in packs) words += repository.words(pack)
            Lexicons.set(code, Lexicon(words.filter { it.isNotBlank() && ' ' !in it }))
            loadedFrom[code] = packs
        }
    }
}
