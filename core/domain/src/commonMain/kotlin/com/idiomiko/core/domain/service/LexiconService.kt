package com.idiomiko.core.domain.service

import com.idiomiko.core.domain.dictionary.DictionaryId
import com.idiomiko.core.domain.dictionary.PackState
import com.idiomiko.core.domain.frequency.FrequencyLists
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.morphology.ChineseScriptTables
import com.idiomiko.core.domain.morphology.ChineseScripts
import com.idiomiko.core.domain.parse.Lexicon
import com.idiomiko.core.domain.parse.Lexicons
import com.idiomiko.core.domain.parse.LexiconParser
import com.idiomiko.core.domain.parse.parser
import com.idiomiko.core.domain.repository.DictionaryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Gives the parsers of languages written without spaces, and Korean's and Arabic's, their lexicons: the words of the
 * language's frequency list, with their forms, and every headword and form of its installed
 * dictionary packs. A lexicon is made again when the installed packs change.
 */
class LexiconService(
    private val dictionaries: DictionaryService,
    private val repository: DictionaryRepository,
    private val frequency: FrequencyLists?,
    /** OpenCC's tables, for a Chinese language read in one script. */
    private val chineseTables: ChineseScriptTables? = null,
) {
    private val mutex = Mutex()
    private val loadedFrom = mutableMapOf<String, Set<DictionaryId>>()

    /** Makes sure the language's lexicon is loaded before its texts are parsed; does nothing for other spaced languages. */
    suspend fun prepare(language: Language) {
        if (language.chineseScript != null) chineseTables?.let { ChineseScripts.prepare(it) }
        if (runCatching { language.parser }.getOrNull() !is LexiconParser) return
        val code = LanguageCodes.codeFor(language.name) ?: return
        val packs = dictionaries.packs.value
            .filter { it.pack.id.sourceLanguage == code && !it.pack.id.glossary && !it.pack.id.isNames && it.state is PackState.Installed }
            .map { it.pack.id }.toSet()
        mutex.withLock {
            if (loadedFrom[code] == packs && Lexicons[code] != null) return
            val words = HashSet<String>()
            // How common each word is, for the parsers that cut by frequency: a form takes its word's rank.
            val ranks = HashMap<String, Int>()
            val heads = HashMap<String, Int>()
            frequency?.list(code)?.inScript(language.chineseScript)?.words?.forEach { word ->
                words += word.key
                words += word.forms
                for (form in word.forms + word.key) ranks.getOrPut(form) { word.rank }
                heads.getOrPut(word.key) { word.rank }
            }
            for (pack in packs) words += repository.words(pack)
            Lexicons.set(code, Lexicon(words.filter { it.isNotBlank() && ' ' !in it }, ranks, heads))
            loadedFrom[code] = packs
        }
    }
}
