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
 * language's frequency list, with their forms, the app's own word list for the language where it has one (Lao's), and
 * every headword and form of its installed dictionary packs, each spelled as the parser reads it; the own list's words
 * that neither the frequency list nor a pack has are marked so (Lexicon.isOwnOnly). A lexicon is made again when the installed packs change.
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
        val parser = runCatching { language.parser }.getOrNull() as? LexiconParser ?: return
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
                val key = parser.lexiconWord(word.key)
                val forms = word.forms.map(parser::lexiconWord)
                words += key
                words += forms
                for (form in forms + key) ranks.getOrPut(form) { word.rank }
                heads.getOrPut(key) { word.rank }
            }
            val packWords = HashSet<String>()
            for (pack in packs) repository.words(pack).forEach { packWords += parser.lexiconWord(it) }
            val own = frequency?.lexicon(code)?.map(parser::lexiconWord).orEmpty()
            val ownOnly = own.filter { it !in words && it !in packWords }
            words += own
            words += packWords
            Lexicons.set(code, Lexicon(words.filter { it.isNotBlank() && ' ' !in it }, ranks, heads, ownOnly))
            loadedFrom[code] = packs
        }
    }
}
