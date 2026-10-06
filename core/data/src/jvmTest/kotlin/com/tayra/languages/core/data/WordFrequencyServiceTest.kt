package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.frequency.FrequencyLists
import com.tayra.languages.core.domain.frequency.WordFrequencyOverview
import com.tayra.languages.core.domain.frequency.WordFrequencyService
import com.tayra.languages.core.domain.frequency.WordKnowledge
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The frequency list shows each word with the reader's status, found through the word or the forms they saved. */
class WordFrequencyServiceTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-frequency", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)

    private val text = """
        # source: test counts (CC0)
        # words: 3
        ser	foi ser é era
        dizer	diz disse dizer
        casa	casa casas
    """.trimIndent()

    private val lists = object : FrequencyLists {
        override suspend fun list(languageCode: String) = if (languageCode == "pt") FrequencyList.parse(text) else null
    }
    private val service = WordFrequencyService(lists, terms, languages)

    @Test
    fun theListIsReadWithRanksAndForms() {
        val list = FrequencyList.parse(text)
        assertEquals("test counts (CC0)", list.source)
        assertEquals(listOf("ser", "dizer", "casa"), list.words.map { it.word })
        assertEquals(listOf(1, 2, 3), list.words.map { it.rank })
        assertEquals(listOf("diz", "disse", "dizer"), list.words[1].forms)
    }

    @Test
    fun wordsTakeTheirOwnStatusOrTheFurthestOfTheirForms() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        terms.save(Term(languageId = pt, text = "disse", textLc = "disse", status = TermStatus.WELL_KNOWN))
        terms.save(Term(languageId = pt, text = "diz", textLc = "diz", status = TermStatus.NEW_2))
        terms.save(Term(languageId = pt, text = "casas", textLc = "casas", status = TermStatus.IGNORED))
        terms.save(Term(languageId = pt, text = "casa", textLc = "casa", status = TermStatus.LEARNING_3))
        terms.save(Term(languageId = pt, text = "foi embora", textLc = "foi\u200Bembora", status = TermStatus.WELL_KNOWN, tokenCount = 3))

        val overview = service.observe(pt).first()!!
        assertEquals("Portuguese", overview.languageName)
        assertEquals(listOf(TermStatus.UNKNOWN, TermStatus.WELL_KNOWN, TermStatus.LEARNING_3), overview.words.map { it.status }, "a phrase does not count for its words")
        assertEquals(listOf(WordKnowledge.NEW, WordKnowledge.KNOWN, WordKnowledge.LEARNING), overview.words.map { it.knowledge })
        assertEquals(1, overview.count(WordKnowledge.KNOWN))
        assertEquals(0, overview.levelBand, "a band of three words with one known is not yet mastered")
    }

    @Test
    fun savingAWordUpdatesTheList() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        val latest = MutableStateFlow<WordFrequencyOverview?>(null)
        backgroundScope.launch { service.observe(pt).collect { latest.value = it } }
        assertEquals(WordKnowledge.NEW, latest.filterNotNull().first().words[0].knowledge)
        terms.save(Term(languageId = pt, text = "era", textLc = "era", status = TermStatus.WELL_KNOWN))
        terms.save(Term(languageId = pt, text = "dizer", textLc = "dizer", status = TermStatus.WELL_KNOWN))
        terms.save(Term(languageId = pt, text = "casa", textLc = "casa", status = TermStatus.IGNORED))
        val overview = latest.filterNotNull().first { it.words.none { word -> word.knowledge == WordKnowledge.NEW } }
        assertEquals(listOf(WordKnowledge.KNOWN, WordKnowledge.KNOWN, WordKnowledge.IGNORED), overview.words.map { it.knowledge })
        assertNull(overview.levelBand, "every word is known or ignored")
    }

    @Test
    fun aCapitalisedWordMatchesItsLowercaseTerm() = runTest {
        val german = languages.save(Language(name = "German"))
        terms.save(Term(languageId = german, text = "Häuser", textLc = "häuser", status = TermStatus.LEARNING_4))
        val lists = object : FrequencyLists {
            override suspend fun list(languageCode: String) = FrequencyList.parse("Haus\thaus häuser hause\n")
        }
        val word = WordFrequencyService(lists, terms, languages).observe(german).first()!!.words.single()
        assertEquals("Haus", word.word.word)
        assertEquals(TermStatus.LEARNING_4, word.status)
    }

    @Test
    fun aLanguageWithoutAListHasNone() = runTest {
        assertNull(service.observe(languages.save(Language(name = "Spanish"))).first())
        assertNull(service.observe(12345).first())
    }
}
