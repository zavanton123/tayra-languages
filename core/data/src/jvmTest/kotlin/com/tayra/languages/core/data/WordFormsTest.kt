package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.frequency.FrequencyLists
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.practice.WordForms
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A word's forms come from its frequency list group and from the forms linked to the saved word; its rank from the list. */
class WordFormsTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-forms", ".db").also { it.delete() }))
    private val terms = TermRepositoryImpl(provider)
    private val list = FrequencyList.parse(
        """
        # source: test
        de	de
        esquecer	esquecer esqueci esqueço esquecemos
        carteira	carteira carteiras
        casa	casa casas
        """.trimIndent(),
    )
    private val lists = object : FrequencyLists {
        override suspend fun list(languageCode: String) = list.takeIf { languageCode == "pt" }
    }

    @Test
    fun formsComeFromTheListAndTheLinkedWords() = runBlocking {
        val portuguese = Language(id = LanguageRepositoryImpl(provider).save(Language(name = "Portuguese")), name = "Portuguese")
        val forms = WordForms(lists, terms)
        assertEquals(setOf("esqueci", "esquecer", "esqueço", "esquecemos"), forms.of("Esqueci", portuguese))
        assertEquals(setOf("lembrei"), forms.of("lembrei", portuguese), "a word the list does not have is its only form")

        // The learner linked "esqueceu" to "esquecer" as its base.
        val base = terms.save(Term(languageId = portuguese.id, text = "esquecer", textLc = "esquecer", status = TermStatus.NEW_1))
        val form = terms.save(Term(languageId = portuguese.id, text = "esqueceu", textLc = "esqueceu", status = TermStatus.NEW_1))
        terms.setParents(form, listOf(base))
        assertEquals(setOf("esqueceu", "esquecer", "esqueci", "esqueço", "esquecemos"), forms.of("esqueceu", portuguese))

        assertEquals(2, forms.rank("esqueço", portuguese))
        assertNull(forms.rank("lembrei", portuguese))
        assertEquals(listOf("esquecer", "carteira"), forms.ranked(portuguese, 2, 3))
    }
}
