package com.tayra.languages.core.data.network

import com.tayra.languages.core.domain.dictionary.InflectionTables
import com.tayra.languages.core.domain.dictionary.WordHint
import com.tayra.languages.core.domain.dictionary.WordHints
import com.tayra.languages.core.domain.dictionary.expressionsOf
import com.tayra.languages.core.domain.repository.SentenceTranslationCache
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WiktApiClientTest {

    /** Saved WiktApi answers, by edition, path after it and language. */
    private val saved = mapOf(
        "en/word/comem|pt" to "en-comem-pt", "en/word/fui|pt" to "en-fui-pt", "en/word/comer|pt" to "en-comer-pt",
        "en/word/comer/forms|pt" to "en-comer-forms-pt", "en/word/кошка|ru" to "en-koshka-ru", "en/word/кошки|ru" to "en-koshki-ru",
        "en/word/食べる|ja" to "en-taberu-ja", "en/word/Haus|de" to "en-Haus-de", "en/search|pt" to "en-search-comer-pt",
        "pt/word/comer|pt" to "pt-comer-pt",
    )

    private val requests = mutableListOf<String>()

    private class MemoryStore : SentenceTranslationCache {
        val rows = mutableMapOf<Pair<String, String>, Pair<String, Long>>()
        override suspend fun get(sentence: String, targetLanguage: String, notBefore: Long) = rows[sentence to targetLanguage]?.takeIf { it.second >= notBefore }?.first
        override suspend fun put(sentence: String, targetLanguage: String, translation: String, createdAt: Long) { rows[sentence to targetLanguage] = translation to createdAt }
        override suspend fun prune(cutoff: Long) { rows.values.removeAll { it.second < cutoff } }
    }

    private fun client(store: SentenceTranslationCache? = null, offline: Boolean = false, now: () -> Long = { 1_000_000L }): WiktApiClient {
        val engine = MockEngine { request ->
            val key = request.url.segments.drop(1).joinToString("/") + "|" + request.url.parameters["lang"]
            requests += key
            if (offline) throw IOException("no network")
            val name = saved[key] ?: return@MockEngine respond("""{"error":true,"status":404}""", HttpStatusCode.NotFound)
            respond(resource(name), HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        return WiktApiClient(HttpClient(engine), store, baseUrl = "https://wiktapi.test/v1", now = now, minIntervalMs = 0)
    }

    private fun resource(name: String): String = javaClass.getResource("/wiktapi/$name.json")!!.readText()

    @Test
    fun anInflectedFormPointsToItsLemma() = runTest {
        val page = client().page("comem", "pt", "en")!!
        assertEquals("en", page.edition)
        assertEquals("comer", page.parentSuggestion)
        assertEquals(listOf("third-person plural present indicative of comer"), page.formOf.map { it.text })
        assertEquals(listOf("/ˈkõ.mẽj̃/", "[ˈkõ.mẽɪ̯̃]", "/ˈkɔ.mɐ̃j̃/"), page.pronunciations.map { it.ipa })
        assertEquals(listOf("Portugal"), page.pronunciations.last().tags)
        // Only pointers: the dictionary has no meaning to show, the lemma instead.
        assertTrue(page.toLookup().isEmpty)
        assertEquals(listOf("comer"), page.toLookup().lemmas)
    }

    @Test
    fun aFormOfTwoVerbsThatIsAWordToo() = runTest {
        val page = client().page("fui", "pt", "en")!!
        assertEquals(listOf("ir", "ser"), page.lemmas)
        // "a farewell" is the word's own meaning, so neither verb is its parent.
        assertNull(page.parentSuggestion)
        assertEquals(2, page.formOf.size)
        assertEquals("a farewell", page.toLookup().entries.single().senses.single().glosses.single())
        assertEquals("https://commons.wikimedia.org/wiki/File:LL-Q5146_(por)-Santamarcanda-fui.wav", page.audio.single().pageUrl)
    }

    @Test
    fun aLemmaHasItsMeaningsSynonymsAudioAndConjugation() = runTest {
        val page = client().page("comer", "pt", "en")!!
        assertEquals(listOf("noun", "verb"), page.entries.map { it.pos })
        assertTrue(page.synonyms.containsAll(listOf("comida", "refeição", "alimentar")))
        val lookup = page.toLookup()
        assertEquals("food; meal", lookup.entries.first().senses.first().glosses.single())
        assertEquals("O comer está na mesa!", lookup.entries.first().senses.first().examples.single().text)
        assertTrue(page.audio.single().mp3Url!!.endsWith(".mp3"))
        assertEquals(listOf("Portugal"), page.audio.single().tags)

        val conjugation = InflectionTables.tables(page).single { it.pos == "verb" }
        assertEquals(listOf("first-person", "singular"), conjugation.columns.first())
        assertEquals(6, conjugation.columns.size)
        val present = conjugation.rows.single { it.tags == listOf("present", "indicative") }
        assertEquals(listOf("como", "comes", "come", "comemos", "comeis", "comem"), present.cells.map { it.single() })
        val all = conjugation.rows.flatMap { it.cells.flatten() } + conjugation.other.map { it.form }
        assertTrue("pt-conj" !in all && "no-table-tags" !in all)
        assertTrue(conjugation.other.any { it.form == "comendo" && it.tags == listOf("gerund") })
        assertEquals(listOf(WordHint(listOf("first-person", "singular", "present", "indicative"), "como"), WordHint(listOf("first-person", "singular", "preterite", "indicative"), "comi")), WordHints.of(page))
    }

    @Test
    fun theFormsEndpointNamesEachPartOfSpeech() = runTest {
        val groups = client().forms("comer", "pt", "en")
        assertEquals(listOf("noun", "verb"), groups.map { it.pos })
        val table = InflectionTables.table("verb", groups.last().forms)
        assertEquals(6, table.columns.size)
        assertEquals(listOf("en/word/comer/forms|pt"), requests)
    }

    @Test
    fun aRussianNounDeclinesByCaseAndNumber() = runTest {
        val api = client()
        val koshka = api.page("кошка", "ru", "en")!!
        val table = InflectionTables.tables(koshka).single()
        assertEquals(listOf(listOf("singular"), listOf("plural")), table.columns)
        assertEquals(listOf("ко́шка", "ко́шки"), table.rows.first { it.tags == listOf("nominative") }.cells.map { it.single() })
        assertEquals(listOf("accusative", "animate"), table.rows.first { "accusative" in it.tags }.tags)
        assertEquals(WordHint(listOf("feminine")), WordHints.of(koshka).first())
        assertEquals(WordHint(listOf("plural"), "ко́шки"), WordHints.of(koshka)[1])

        val koshki = api.page("кошки", "ru", "en")!!
        // The stressed spelling is shown, the page it links to opened.
        assertEquals("кошка", koshki.parentSuggestion)
        assertEquals("ко́шка", koshki.formOf.first().formOf.single().shown)
        assertEquals(3, koshki.formOf.size)
    }

    @Test
    fun aGermanNounHasItsGenderAndPlural() = runTest {
        val page = client().page("Haus", "de", "en")!!
        assertEquals(listOf("name", "noun"), page.entries.map { it.pos })
        assertEquals(listOf(WordHint(listOf("neuter")), WordHint(listOf("plural"), "Häuser")), WordHints.of(page))
        val declension = InflectionTables.tables(page).single { it.pos == "noun" }
        assertEquals(listOf(listOf("singular"), listOf("definite", "plural")), declension.columns)
        assertEquals(listOf("Haus", "Hause"), declension.rows.single { it.tags == listOf("dative") }.cells.first())
    }

    @Test
    fun aJapaneseConjugationIsAListOfTaggedForms() = runTest {
        val page = client().page("食べる", "ja", "en")!!
        assertEquals("verb", page.entries.first().pos)
        val table = InflectionTables.tables(page).first()
        assertTrue(table.rows.isEmpty())
        assertTrue(table.other.any { it.form == "食べて [tabete]" && it.tags == listOf("conjunctive") })
        assertTrue(table.other.none { "error-unrecognized-form" in it.tags || it.form == "ja-conj-ex" })
    }

    @Test
    fun aMissIsAskedAgainInLowerCaseAndThenRemembered() = runTest {
        val api = client()
        assertNull(api.page("Muito", "pt", "en"))
        assertEquals(listOf("en/word/Muito|pt", "en/word/muito|pt"), requests)
        assertNull(api.page("Muito", "pt", "en"))
        assertEquals(2, requests.size)
    }

    @Test
    fun theNativeEditionIsAskedFirstAndEnglishWhenItLacksTheWord() = runTest {
        val api = client()
        assertEquals("pt", api.page("comer", "pt", "pt")!!.edition)
        assertEquals("en", api.page("comem", "pt", "pt")!!.edition)
        assertEquals(listOf("pt/word/comer|pt", "pt/word/comem|pt", "en/word/comem|pt"), requests)
        // A gloss language with no edition of its own goes straight to English.
        api.page("fui", "pt", "sw")
        assertEquals("en/word/fui|pt", requests.last())
    }

    @Test
    fun wiktionaryCodesAreAskedFor() = runTest {
        val api = client()
        api.page("kuća", "sr", "en")
        api.page("hus", "no", "en")
        assertEquals(listOf("en/word/kuća|sh", "en/word/hus|nb"), requests)
    }

    @Test
    fun aSecondLookupAsksNobody() = runTest {
        val store = MemoryStore()
        client(store).page("comem", "pt", "en")
        assertEquals(1, requests.size)
        client(store).page("comem", "pt", "en")!!.parentSuggestion.let { assertEquals("comer", it) }
        assertEquals(1, requests.size)
        // A day later the stored answer is too old.
        client(store, now = { 1_000_000L + 25 * 60 * 60 * 1000 }).page("comem", "pt", "en")
        assertEquals(2, requests.size)
    }

    @Test
    fun aFailedConnectionPausesTheRequests() = runTest {
        val api = client(offline = true)
        assertNull(api.page("comem", "pt", "en"))
        assertNull(api.page("comer", "pt", "en"))
        assertEquals(1, requests.size)
    }

    @Test
    fun expressionsAreTheMultiWordResults() = runTest {
        val results = client().search("comer", "pt", "en")
        assertEquals("comer", results.first().word)
        val expressions = expressionsOf(results, "comer")
        assertTrue("comer com os olhos" in expressions)
        assertTrue(expressions.none { !it.contains(' ') })
    }
}
