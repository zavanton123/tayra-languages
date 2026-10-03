package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.LearningTranslations
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Words being learned (statuses 1 to 4) get a translation looked up and stored. */
class LearningTranslationsTest {

    private class Env(answer: (String) -> String? = { "<$it>" }) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-learning", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider, translationLanguage = { "ru" })
        val settings = SettingsRepositoryImpl(MapSettings())
        val engine = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = answer(text)
        }
        val offline = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
        }
        val learning = LearningTranslations(terms, languages, WordTranslationService(terms, offline, engine, settings))
        val service = TermService(terms, languages, translationsWanted = learning::request)
    }

    /** Runs in real time, as the database works on its own threads; the filler runs in the scope given to [block]. */
    private fun real(block: suspend (scope: CoroutineScope) -> Unit) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            block(scope)
        } finally {
            scope.cancel()
        }
    }

    private suspend fun Env.translationOf(language: Long, text: String, expected: String?) =
        withTimeout(5_000) { while (terms.findByTextLc(language, text)?.translation != expected) delay(20) }

    @Test
    fun aWordStartingToBeLearnedGetsATranslation() = real { scope ->
        val env = Env()
        val language = env.languages.save(Language(name = "Portuguese"))
        env.learning.start(scope)
        val placeholder = env.terms.save(Term(languageId = language, text = "lobo", textLc = "lobo", status = TermStatus.UNKNOWN))

        env.service.setStatus(listOf(placeholder), TermStatus.NEW_1)
        env.translationOf(language, "lobo", "<lobo>")

        env.service.save(env.service.findOrNew(language, "trabalhando").copy(status = TermStatus.NEW_1, statusExplicitlySet = true))
        env.translationOf(language, "trabalhando", "<trabalhando>")
    }

    @Test
    fun knownWordsAndOwnTranslationsAreLeftAlone() = real { scope ->
        val env = Env()
        val language = env.languages.save(Language(name = "Portuguese"))
        env.learning.start(scope)
        val known = env.terms.save(Term(languageId = language, text = "casa", textLc = "casa", status = TermStatus.UNKNOWN))
        env.service.setStatus(listOf(known), TermStatus.WELL_KNOWN)
        env.service.save(env.service.findOrNew(language, "lobo").copy(status = TermStatus.NEW_2, statusExplicitlySet = true, translation = "wolf"))
        env.service.save(env.service.findOrNew(language, "dia").copy(status = TermStatus.NEW_1, statusExplicitlySet = true))
        env.translationOf(language, "dia", "<dia>")

        assertNull(env.terms.findByTextLc(language, "casa")?.translation, "known words are not translated")
        assertEquals("wolf", env.terms.findByTextLc(language, "lobo")?.translation)
        env.service.save(env.service.draftOf(env.terms.findByTextLc(language, "lobo")!!).copy(translation = ""))
        assertEquals("wolf", env.terms.findByTextLc(language, "lobo")?.translation, "a word being learned keeps its translation")
    }

    @Test
    fun learningWordsWithoutATranslationAreFilledAtStart() = real { scope ->
        var online = false
        val env = Env { if (online) "<$it>" else null }
        val language = env.languages.save(Language(name = "Portuguese"))
        env.learning.start(scope)
        env.service.save(env.service.findOrNew(language, "noite").copy(status = TermStatus.NEW_1, statusExplicitlySet = true))
        withTimeout(5_000) { while (env.terms.learningWithoutTranslation().isEmpty()) delay(20) }
        delay(200)
        assertNull(env.terms.findByTextLc(language, "noite")?.translation, "nothing to store while the engine has no answer")

        online = true
        val restarted = LearningTranslations(env.terms, env.languages, WordTranslationService(env.terms, env.offline, env.engine, env.settings))
        restarted.start(scope)
        env.translationOf(language, "noite", "<noite>")
    }
}
