package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.language.LanguageDefinitions
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.domain.morphology.ChineseScriptTables
import com.tayra.languages.core.domain.service.ChineseScriptService
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.term.TermTextNormalizer
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ChineseScriptTest {

    /** The tables the app bundles, read from where core/ui keeps them. */
    private val tables = ChineseScriptTables { name -> File("../ui/src/commonMain/composeResources/files/opencc/$name").takeIf { it.isFile }?.readText() }

    private val file: File = File.createTempFile("tayra-chinese", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)

    @Test
    fun aScriptConvertsTheSavedTermsAndMergesTheOnesSavedTwice() = runBlocking<Unit> {
        val mandarin = LanguageDefinitions.all.first { it.name == "Mandarin Chinese" }.language
        val id = languages.save(mandarin)
        val language = languages.getById(id)!!
        fun term(text: String, status: TermStatus, translation: String? = null) = TermTextNormalizer.termFromText(language, text).copy(status = status, translation = translation)
        terms.save(term("我們", TermStatus.LEARNING_4, "we"))
        terms.save(term("我们", TermStatus.NEW_1))
        terms.save(term("頭髮", TermStatus.WELL_KNOWN, "hair"))
        terms.save(term("电脑", TermStatus.NEW_2, "computer"))

        val change = ChineseScriptService(languages, tables, terms, books).setScript(id, ChineseScript.SIMPLIFIED)
        assertEquals(ChineseScriptService.Change(converted = 1, merged = 1), change)
        val simplified = languages.getById(id)!!
        assertEquals(ChineseScript.SIMPLIFIED, simplified.chineseScript)
        // Keys as the language makes them: without a lexicon each character is a word of its own.
        fun key(text: String) = TermTextNormalizer.termFromText(simplified, text).textLc
        val we = terms.findByTextLc(id, key("我们"))!!
        assertEquals(TermStatus.LEARNING_4 to "we", we.status to we.translation, "the more advanced status wins, the translation stays")
        assertEquals(key("我们"), key("我們"), "a traditional spelling is read as the simplified one")
        assertEquals("hair", terms.findByTextLc(id, key("头发"))?.translation)
        assertEquals(TermStatus.NEW_2, terms.findByTextLc(id, key("电脑"))?.status)
        assertEquals(3, terms.list(com.tayra.languages.core.domain.repository.TermListFilter(languageId = id), com.tayra.languages.core.domain.repository.TermListSort(), 0, 100).totalCount)

        // From now on a traditional text is read, and its words saved, in simplified characters.
        val service = TermService(terms, languages, chineseTables = tables)
        assertEquals(we.id, service.find(id, "我們")?.id)
        assertEquals("头发", TermTextNormalizer.normalize(simplified, "頭髮").replace("\u200B", ""))
    }

    @Test
    fun theFrequencyListIsShownInTheScript() = runBlocking<Unit> {
        com.tayra.languages.core.domain.morphology.ChineseScripts.prepare(tables)
        val list = FrequencyList.parse("# source: test\n我們\t我们\n電腦\t电脑\n")
        val simplified = list.inScript(ChineseScript.SIMPLIFIED)
        assertEquals(listOf("我们", "电脑"), simplified.words.map { it.word })
        assertEquals(listOf("我们"), simplified.words.first().forms)
        assertEquals(listOf("我們", "我們", "我们"), list.inScript(ChineseScript.TRADITIONAL).words.first().let { listOf(it.word) + it.forms }, "the forms as counted are kept")
        assertEquals(list, list.inScript(null))
    }
}
