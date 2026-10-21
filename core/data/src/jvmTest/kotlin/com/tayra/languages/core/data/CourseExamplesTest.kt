package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.courses.COURSES_PAGE_PREFIX
import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseExamplesProvider
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.Lesson
import com.tayra.languages.core.domain.courses.SampleCourseSource
import com.tayra.languages.core.domain.language.LanguageDefinitions
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.parse.Lexicon
import com.tayra.languages.core.domain.parse.Lexicons
import com.tayra.languages.core.domain.service.ExampleLesson
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSort
import com.tayra.languages.core.domain.service.ExampleSource
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CourseExamplesTest {

    private val db = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-course-examples", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(db)
    private val terms = TermRepositoryImpl(db)

    @AfterTest
    fun noLexicon() = Lexicons.set("ja", null)

    private class Packs(var courses: Map<String, List<Course>>) : SampleCourseSource {
        var reads = 0
        override suspend fun courses(languageCode: String): List<Course> {
            reads++
            return courses[languageCode].orEmpty()
        }
        override suspend fun courseIds(languageCode: String) = courses[languageCode].orEmpty().map { it.id }.toSet()
    }

    private fun course(id: String, code: String, vararg lessons: String) = Course(
        id = id, languageCode = code, title = "Course $id", description = "", level = CourseLevel.A1, topic = "",
        lessons = lessons.mapIndexed { i, text -> Lesson(id = "$id-$i", title = "Lesson ${i + 1}", summary = "", text = text) },
    )

    private suspend fun language(name: String): Language {
        val definition = LanguageDefinitions.all.first { it.name == name }.language
        return definition.copy(id = languages.save(definition))
    }

    private fun query(text: String, language: Language, sort: ExampleSort = ExampleSort.COURSE_ORDER, minWords: Int? = null, maxWords: Int? = null, limit: Int = 10) =
        ExampleSearchQuery(text, language, "en", minWords = minWords, maxWords = maxWords, sort = sort, limit = limit, source = ExampleSource.COURSES)

    private val portugueseCourses = listOf(
        course("a", "pt", "Eu falo português. A casa é grande.\nEles falam muito. Casa nova!", "Ela fala com a casa amarela."),
        course("b", "pt", "Não tenho tempo para falar. Uma casa, duas casas."),
    )

    @Test
    fun aWordMatchesAsAWholeWordInAnyCaseAndPhrasesMatchAsARun() = runTest {
        val portuguese = language("Portuguese")
        val provider = CourseExamplesProvider(Packs(mapOf("pt" to portugueseCourses)), terms)
        val casa = provider.search(query("casa", portuguese))
        assertEquals(listOf("A casa é grande.", "Casa nova!", "Ela fala com a casa amarela.", "Uma casa, duas casas."), casa.sentences.map { it.text })
        assertEquals(4, casa.total)
        assertEquals(ExampleLesson("a", "a-1", "Course a", "Lesson 2"), casa.sentences[2].lesson)

        assertEquals(listOf("A casa é grande."), provider.search(query("a CASA é", portuguese)).sentences.map { it.text })
        assertEquals(emptyList(), provider.search(query("casa grande", portuguese)).sentences)
    }

    @Test
    fun theTermsSavedFormsAreFoundToo() = runTest {
        val portuguese = language("Portuguese")
        val parent = terms.save(Term(languageId = portuguese.id, text = "falar", textLc = "falar"))
        for (form in listOf("falo", "fala")) terms.setParents(terms.save(Term(languageId = portuguese.id, text = form, textLc = form)), listOf(parent))
        val provider = CourseExamplesProvider(Packs(mapOf("pt" to portugueseCourses)), terms)
        assertEquals(
            listOf("Eu falo português.", "Ela fala com a casa amarela.", "Não tenho tempo para falar."),
            provider.search(query("falar", portuguese)).sentences.map { it.text },
        )
        // A form finds the term it is a form of, and the term's other forms.
        assertEquals(3, provider.search(query("falo", portuguese)).total)
        // "falam" is no saved form.
        assertTrue(provider.search(query("falar", portuguese)).sentences.none { "falam" in it.text })
    }

    @Test
    fun aJapanesePhraseIsFoundByTheWordsTheParserCutsItInto() = runTest {
        Lexicons.set("ja", Lexicon(listOf("私", "は", "猫", "が", "好き", "です", "犬", "大好き")))
        val japanese = language("Japanese")
        val provider = CourseExamplesProvider(Packs(mapOf("ja" to listOf(course("j", "ja", "私は猫が好きです。犬が大好きです。猫です。")))))
        assertEquals(listOf("私は猫が好きです。", "猫です。"), provider.search(query("猫", japanese)).sentences.map { it.text })
        assertEquals(listOf("私は猫が好きです。"), provider.search(query("猫が好き", japanese)).sentences.map { it.text })
        // 好き is only part of the word 大好き in the second sentence.
        assertEquals(listOf("私は猫が好きです。"), provider.search(query("好き", japanese)).sentences.map { it.text })
    }

    @Test
    fun filtersSortsAndPagesCountWordsAsTheReaderDoes() = runTest {
        val portuguese = language("Portuguese")
        val provider = CourseExamplesProvider(Packs(mapOf("pt" to portugueseCourses)), terms, seeds = { 3 })
        val short = provider.search(query("casa", portuguese, sort = ExampleSort.SHORTEST, maxWords = 4))
        assertEquals(listOf("Casa nova!", "A casa é grande.", "Uma casa, duas casas."), short.sentences.map { it.text })
        assertEquals(3, short.total)

        val first = provider.search(query("casa", portuguese, sort = ExampleSort.LONGEST, limit = 3))
        assertEquals(4, first.total)
        assertEquals(listOf("Ela fala com a casa amarela.", "Uma casa, duas casas.", "A casa é grande."), first.sentences.map { it.text })
        assertTrue(first.nextPage!!.startsWith(COURSES_PAGE_PREFIX))
        val second = provider.nextPage(first.nextPage!!, "en")
        assertEquals(listOf("Casa nova!"), second.sentences.map { it.text })
        assertNull(second.nextPage)

        val random = provider.search(query("casa", portuguese, sort = ExampleSort.RANDOM, limit = 2))
        val rest = provider.nextPage(random.nextPage!!, "en")
        assertEquals(4, (random.sentences + rest.sentences).map { it.text }.toSet().size, "the pages of a random search share one order")
    }

    @Test
    fun theSourceIsOfferedWithAPackAndItsSentencesFollowThePacks() = runTest {
        val portuguese = language("Portuguese")
        val packs = Packs(mapOf("pt" to portugueseCourses.take(1)))
        val provider = CourseExamplesProvider(packs, terms)
        assertEquals(listOf(ExampleSource.COURSES), provider.sources(portuguese).map { it.source })
        assertEquals(emptyList(), provider.sources(language("German")))

        assertEquals(3, provider.search(query("casa", portuguese)).total)
        provider.search(query("falo", portuguese))
        assertEquals(1, packs.reads, "the lessons are read once")

        packs.courses = mapOf("pt" to portugueseCourses)
        assertEquals(4, provider.search(query("casa", portuguese)).total)
        packs.courses = emptyMap()
        assertEquals(emptyList(), provider.search(query("casa", portuguese)).sentences)
        assertEquals(emptyList(), provider.sources(portuguese))
    }
}
