package com.tayra.languages.core.domain.dictionary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WiktionaryTest {

    private fun form(form: String, vararg tags: String, source: String? = "conjugation") = WiktionaryForm(form, tags.toList(), source)

    private fun formOfPage(word: String, vararg lemmas: String) = WiktionaryPage(
        word, "en", "pt",
        listOf(WiktionaryEntry("verb", lemmas.map { WiktionarySense(listOf("form of $it"), formOf = listOf(WiktionaryLemma(it, it))) })),
    )

    @Test
    fun editionsAreTheNativeOneThenEnglish() {
        assertEquals(listOf("pt", "en"), WiktionaryCodes.editionsFor("pt"))
        assertEquals(listOf("en"), WiktionaryCodes.editionsFor("en"))
        // No Swahili or Simple English edition glosses in the reader's language.
        assertEquals(listOf("en"), WiktionaryCodes.editionsFor("sw"))
        assertEquals(listOf("en"), WiktionaryCodes.editionsFor("simple"))
    }

    @Test
    fun wiktionaryFilesSomeLanguagesUnderOtherCodes() {
        assertEquals("sh", WiktionaryCodes.languageCode("sr"))
        assertEquals("sh", WiktionaryCodes.languageCode("hr"))
        assertEquals("nb", WiktionaryCodes.languageCode("no"))
        assertEquals("zh", WiktionaryCodes.languageCode("yue"))
        assertEquals("pt", WiktionaryCodes.languageCode("pt"))
    }

    @Test
    fun theEntryLinksToItsLanguagesSection() {
        assertEquals("https://en.wiktionary.org/wiki/dar_certo#Portuguese", WiktionaryCodes.entryUrl(WiktionaryPage("dar certo", "en", "pt", emptyList()), "Portuguese"))
        assertEquals("https://en.wiktionary.org/wiki/kuća#Serbo-Croatian", WiktionaryCodes.entryUrl(WiktionaryPage("kuća", "en", "sh", emptyList()), "Serbian"))
        // Another edition names its sections in its own language.
        assertEquals("https://de.wiktionary.org/wiki/comer", WiktionaryCodes.entryUrl(WiktionaryPage("comer", "de", "pt", emptyList()), "Portuguese"))
    }

    @Test
    fun pseudoRowsAreNoForms() {
        val forms = listOf(
            form("no-table-tags", "table-tags"), form("pt-conj", "inflection-template"), form("kóška", "romanization", source = null),
            form("ко́шка", "canonical", "feminine", source = null), form("velar-stem", "class"), form("-", "plural"), form("como", "first-person", "singular", "present"),
        )
        assertEquals(listOf("como"), InflectionTables.realForms(forms).map { it.form })
    }

    @Test
    fun aConjugationIsGroupedByTenseAndPerson() {
        val forms = listOf(
            form("comer", "impersonal", "infinitive"),
            form("como", "first-person", "indicative", "present", "singular"), form("comes", "indicative", "present", "second-person", "singular"),
            form("comemos", "first-person", "indicative", "plural", "present"),
            form("comi", "first-person", "indicative", "preterite", "singular"), form("comemos", "first-person", "indicative", "plural", "preterite"),
            form("comido", "masculine", "participle", "past", "singular"),
        )
        val table = InflectionTables.table("verb", forms)
        assertEquals(listOf(listOf("first-person", "singular"), listOf("second-person", "singular"), listOf("first-person", "plural")), table.columns)
        assertEquals(listOf(listOf("present", "indicative"), listOf("preterite", "indicative")), table.rows.map { it.tags })
        assertEquals(listOf(listOf("comi"), emptyList(), listOf("comemos")), table.rows[1].cells)
        // An infinitive or a participle has no person: it is listed under the grid.
        assertEquals(listOf("comer", "comido"), table.other.map { it.form })
        assertEquals(listOf("past", "participle", "masculine", "singular"), table.other.last().tags)
    }

    @Test
    fun tagsThatFitNoGridAreAList() {
        val table = InflectionTables.table("verb", listOf(form("食べて", "conjunctive"), form("食べない", "negative"), form("食べた", "past", "singular")))
        assertTrue(table.rows.isEmpty() && table.columns.isEmpty())
        assertEquals(listOf("食べて", "食べない", "食べた"), table.other.map { it.form })
    }

    @Test
    fun theHeadLinesFormsGiveWayToTheTable() {
        val table = InflectionTables.table(
            "noun",
            listOf(
                form("Häuser", "plural", source = null),
                form("Haus", "nominative", "singular", source = "declension"), form("Häuser", "nominative", "plural", source = "declension"),
                form("Hauses", "genitive", "singular", source = "declension"), form("Häuser", "genitive", "plural", source = "declension"),
            ),
        )
        assertEquals(listOf(listOf("Haus", "Häuser"), listOf("Hauses", "Häuser")).map { row -> row.map { listOf(it) } }, table.rows.map { it.cells })
        assertTrue(table.other.isEmpty())
    }

    @Test
    fun anOfflineAnswerAlwaysWins() {
        val online = formOfPage("comem", "comer")
        assertEquals("comer", parentSuggestion(DictionaryLookup.EMPTY, online))
        // The pack's own forms named another lemma.
        val offline = DictionaryLookup(listOf(DictionaryEntry("comer", "verb", null, emptyList(), isOwnEntry = false)), listOf("comer"))
        assertEquals("comer", parentSuggestion(offline, formOfPage("comem", "other")))
        // The pack has the word as a headword: no parent, whatever Wiktionary says.
        val headword = DictionaryLookup(listOf(DictionaryEntry("comem", "verb", null, emptyList())), emptyList())
        assertNull(parentSuggestion(headword, online))
        assertNull(parentSuggestion(DictionaryLookup.EMPTY, formOfPage("fui", "ir", "ser")))
        assertNull(parentSuggestion(DictionaryLookup.EMPTY, null))
    }

    @Test
    fun formsAreComparedWithoutStressMarks() {
        assertEquals("кошка", WiktionaryCodes.plain("Ко́шка"))
    }
}
