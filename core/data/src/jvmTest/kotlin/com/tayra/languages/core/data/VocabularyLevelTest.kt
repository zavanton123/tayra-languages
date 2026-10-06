package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.VocabularyLevelRepositoryImpl
import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.frequency.FrequencyLists
import com.tayra.languages.core.domain.frequency.LevelChange
import com.tayra.languages.core.domain.frequency.VocabularyLevelService
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Setting a vocabulary level saves the words up to it as known, and lowering it takes back only what it saved. */
class VocabularyLevelTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-level", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val levels = VocabularyLevelRepositoryImpl(provider)

    private val list = FrequencyList.parse(
        """
        # source: test
        ser	foi ser é era
        ir	vai foi ir
        dizer	diz disse dizer
        Casa	casa casas
        """.trimIndent(),
    )
    private val service = VocabularyLevelService(object : FrequencyLists {
        override suspend fun list(languageCode: String) = list.takeIf { languageCode == "pt" }
    }, levels, languages)

    private suspend fun status(language: Long, text: String) = terms.findByTextLc(language, text)?.status

    @Test
    fun raisingTheLevelSavesWordsAndFormsAsKnownAndLeavesSavedOnesAlone() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        terms.save(Term(languageId = pt, text = "disse", textLc = "disse", status = TermStatus.LEARNING_3))
        assertEquals(0, service.observeLevel(pt).first())

        val change = service.setLevel(pt, 3)
        assertEquals(LevelChange(from = 0, to = 3, added = 8, removed = 0), change, "ser foi é era, ir vai, dizer diz; foi once")
        assertEquals(3, service.observeLevel(pt).first())
        for (known in listOf("ser", "foi", "é", "era", "ir", "vai", "dizer", "diz")) assertEquals(TermStatus.WELL_KNOWN, status(pt, known), known)
        assertEquals(TermStatus.LEARNING_3, status(pt, "disse"), "a saved word keeps its status")
        assertNull(status(pt, "casa"), "words above the level are not saved")
        assertEquals(listOf("ser"), terms.parents(terms.findByTextLc(pt, "era")!!.id).map { it.text }, "a form is linked to its word")
    }

    @Test
    fun wordsAndFormsOnlySeenOnAPageBecomeKnown() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        // Opening a page saves its words with status 0 until the reader judges them.
        for (seen in listOf("dizer", "disse", "foi")) terms.save(Term(languageId = pt, text = seen, textLc = seen, status = TermStatus.UNKNOWN))
        val change = service.setLevel(pt, 3)!!
        for (known in listOf("dizer", "disse", "foi")) assertEquals(TermStatus.WELL_KNOWN, status(pt, known), known)
        assertEquals(9, change.added, "ser foi é era, ir vai, dizer diz disse")
        service.setLevel(pt, 0)
        assertNull(status(pt, "disse"), "a level's known terms go when it is lowered, whether it made them or raised them from 0")
    }

    @Test
    fun loweringTheLevelTakesBackOnlyWhatTheLevelSavedAndTheReaderLeftKnown() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        service.setLevel(pt, 4)
        assertEquals("Casa", terms.findByTextLc(pt, "casa")?.text, "a word is saved as the list writes it")
        terms.updateStatus(listOf(terms.findByTextLc(pt, "diz")!!.id), TermStatus.IGNORED)

        val change = service.setLevel(pt, 2)!!
        assertEquals(4, change.from)
        assertEquals(4, change.removed, "dizer, disse, casa and casas; diz was changed by the reader")
        assertNull(status(pt, "dizer"))
        assertNull(status(pt, "casas"))
        assertEquals(TermStatus.IGNORED, status(pt, "diz"))
        assertEquals(TermStatus.WELL_KNOWN, status(pt, "foi"), "a form shared with a word still within the level stays")

        service.setLevel(pt, 0)
        assertNull(status(pt, "ser"))
        assertEquals(TermStatus.IGNORED, status(pt, "diz"))
        assertEquals(0, service.observeLevel(pt).first())
        service.setLevel(pt, 2)
        assertEquals(TermStatus.IGNORED, status(pt, "diz"), "a term the reader changed is not taken over again")
    }

    @Test
    fun aLevelIsAskedForUntilOneIsChosenEvenJustStartingOut() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        val custom = languages.save(Language(name = "Klingon"))
        assertEquals(true, service.needsLevel(pt))
        assertEquals(false, service.needsLevel(custom), "a language without a list is not asked about")
        assertNull(service.observeChosenLevel(pt).first())
        service.setLevel(pt, 0)
        assertEquals(false, service.needsLevel(pt), "starting out is a choice")
        assertEquals(0, service.observeChosenLevel(pt).first())
    }

    @Test
    fun theLevelIsKeptPerLanguageAndNeedsAList() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        val de = languages.save(Language(name = "German"))
        service.setLevel(pt, 2)
        assertEquals(0, service.observeLevel(de).first())
        assertNull(service.setLevel(de, 2))
        assertEquals(listOf(0, 100, 200, 300, 400, 500, 700, 1000, 1500, 2000, 2500, 3000, 3840), VocabularyLevelService.choices(3840))
    }
}
