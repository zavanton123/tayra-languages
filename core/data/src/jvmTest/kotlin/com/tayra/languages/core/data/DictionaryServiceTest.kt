package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.DictionaryRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryAssets
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.service.DictionaryService
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DictionaryServiceTest {
    private val json = """
        {"format":1,"entries":[
          {"word":"Word","pos":"noun","senses":[{"glosses":["английская фамилия"]}]},
          {"word":"word","pos":"noun","senses":[{"glosses":["слово"]}]},
          {"word":"cat","pos":"noun","ipa":"[kæt]","senses":[{"glosses":["кошка, кот"]},{"glosses":["сварливая женщина"],"tags":["derogatory"]}]},
          {"word":"do","pos":"verb","senses":[{"glosses":["делать"]}]},
          {"word":"left","pos":"adj","senses":[{"glosses":["левый"]}]},
          {"word":"leave","pos":"verb","senses":[{"glosses":["уходить"]}]},
          {"word":"go","pos":"verb","senses":[{"glosses":["идти"]}]},
          {"word":"wend","pos":"verb","senses":[{"glosses":["направляться"]}]}
        ],"forms":[
          {"form":"cats","lemma":"cat","generated":true},{"form":"did","lemma":"do"},{"form":"Done","lemma":"do"},
          {"form":"words","lemma":"Word"},{"form":"words","lemma":"word"},
          {"form":"left","lemma":"leave"},{"form":"went","lemma":"go"},{"form":"went","lemma":"wend"}
        ]}
    """.trimIndent()

    private val enRu = DictionaryId("en", "ru")

    private fun service(assets: DictionaryAssets): Pair<DictionaryService, DictionaryRepositoryImpl> {
        val file = File.createTempFile("tayra-dict", ".db").also { it.delete() }
        val repository = DictionaryRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        return DictionaryService(assets, repository) to repository
    }

    @Test
    fun importsOnceAndResolvesForms() = runTest {
        var reads = 0
        val (service, repository) = service(object : DictionaryAssets {
            override suspend fun readJson(dictionary: DictionaryId): String? = json.also { reads++ }
        })
        assertTrue(!service.isAvailable(enRu))
        service.importIfNeeded()
        service.importIfNeeded()
        assertEquals(1, reads, "a dictionary in the current format is not imported again")
        assertTrue(service.isAvailable(enRu))
        assertEquals(DictionaryService.FORMAT, repository.importedFormat(enRu))

        val cats = service.lookup(enRu, "Cats")
        assertEquals(listOf("cat"), cats.lemmas)
        assertEquals("cat", cats.parentSuggestion)
        assertEquals("кошка, кот", cats.suggestedTranslation)
        assertEquals(listOf("derogatory"), cats.entries.single().senses[1].tags)
        assertEquals("[kæt]", cats.entries.single().ipa)
        assertTrue(!cats.entries.single().isOwnEntry)

        val done = service.lookup(enRu, "done")
        assertEquals("do", done.parentSuggestion)

        val left = service.lookup(enRu, "left")
        assertEquals(listOf("left", "leave"), left.entries.map { it.word })
        assertNull(left.parentSuggestion, "a headword of its own is not given a parent")
        assertEquals("левый", left.suggestedTranslation)

        val went = service.lookup(enRu, "went")
        assertEquals(listOf("go", "wend"), went.lemmas)
        assertNull(went.parentSuggestion, "ambiguous forms get no parent")

        val words = service.lookup(enRu, "words")
        assertEquals(listOf("word"), words.lemmas, "the common noun beats the surname for a lowercase word")
        assertEquals("word", words.parentSuggestion)
        assertEquals("слово", words.suggestedTranslation)
        assertEquals(listOf("Word"), service.lookup(enRu, "Words").lemmas)
        assertEquals(listOf("word", "Word"), service.lookup(enRu, "word").entries.map { it.word })

        assertTrue(service.lookup(enRu, "xyzzy").isEmpty)
        assertTrue(service.lookup(enRu, "  ").isEmpty)
    }

    @Test
    fun missingAssetLeavesDictionaryUnavailable() = runTest {
        val (service, _) = service(object : DictionaryAssets {
            override suspend fun readJson(dictionary: DictionaryId): String? = null
        })
        service.importIfNeeded()
        assertTrue(!service.isAvailable(enRu))
        assertTrue(service.lookup(enRu, "cat").isEmpty)
    }
}
