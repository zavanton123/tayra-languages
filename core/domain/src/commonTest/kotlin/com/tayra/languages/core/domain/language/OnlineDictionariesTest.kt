package com.tayra.languages.core.domain.language

import com.tayra.languages.core.domain.model.DictionaryType
import com.tayra.languages.core.domain.model.DictionaryUse
import com.tayra.languages.core.domain.model.LanguageDictionary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnlineDictionariesTest {
    private val en = LanguageOption("en", "English")
    private val ru = LanguageOption("ru", "Russian")
    private val de = LanguageOption("de", "German")
    private val pt = LanguageOption("pt", "Portuguese")
    private val cs = LanguageOption("cs", "Czech")
    private val sl = LanguageOption("sl", "Slovene")
    private val la = LanguageOption("la", "Latin")

    private fun url(id: String, source: LanguageOption, target: LanguageOption) =
        OnlineDictionaries.all.first { it.id == id }.url(source, target)

    @Test
    fun wordReferenceUsesSiteCodesAndPairsWithEnglish() {
        assertEquals("https://www.wordreference.com/czen/[WORD]", url("wordreference", cs, ru))
        assertEquals("https://www.wordreference.com/enpt/[WORD]", url("wordreference", en, pt))
        assertEquals("https://www.wordreference.com/enru/[WORD]", url("wordreference", en, ru))
        assertEquals("https://www.wordreference.com/definition/[WORD]", url("wordreference", en, en))
        assertNull(url("wordreference", la, en))
    }

    @Test
    fun lingueeSpellsSloveneItsOwnWay() {
        assertEquals("https://www.linguee.com/english-slovene/search?query=[WORD]", url("linguee", sl, en))
        assertEquals("https://www.linguee.com/english-portuguese/search?query=[WORD]", url("linguee", pt, ru))
    }

    @Test
    fun reversoOnlyOffersPairsTheSiteHas() {
        assertEquals("https://context.reverso.net/translation/portuguese-russian/[WORD]", url("reverso-context", pt, ru))
        assertEquals("https://context.reverso.net/translation/portuguese-german/[WORD]", url("reverso-context", pt, de))
        assertNull(url("reverso-context", cs, en))
    }

    @Test
    fun ponsFallsBackFromUnsupportedEnglishPairs() {
        assertEquals("https://en.pons.com/translate/czech-german/[WORD]", url("pons", cs, de))
        assertNull(url("pons", cs, en))
        assertEquals("https://en.pons.com/translate/portuguese-english/[WORD]", url("pons", pt, ru))
    }

    @Test
    fun everyCatalogLanguageGetsAtLeastTheUniversalSites() {
        for (name in LanguageCatalog.targetLanguages) {
            val source = LanguageOption(LanguageCodes.codeFor(name)!!, name)
            for (target in LanguageCatalog.nativeLanguages) {
                val offered = OnlineDictionaries.all.mapNotNull { it.url(source, target) }
                assertTrue(offered.size >= 5, "$name -> ${target.name} offers ${offered.size}")
                assertTrue(offered.all { "{" !in it }, "unfilled placeholder in $offered")
            }
        }
    }

    @Test
    fun dictionariesSharingAHostGetDistinctLabels() {
        val infopedia = listOf(
            "https://www.infopedia.pt/dicionarios/portugues-ingles/[WORD]",
            "https://www.infopedia.pt/dicionarios/lingua-portuguesa/[WORD]",
            "https://www.infopedia.pt/dicionarios/verbos-portugueses/[WORD]",
            "https://www.verbix.com/webverbix/go.php?&D1=2&T1=[WORD]",
            "https://www.verbix.com/webverbix/go.php?&D1=1002&T1=[WORD]",
        ).mapIndexed { i, url -> com.tayra.languages.core.domain.model.LanguageDictionary(id = i.toLong(), useFor = com.tayra.languages.core.domain.model.DictionaryUse.TERMS, type = com.tayra.languages.core.domain.model.DictionaryType.POPUP, url = url) }
        val byDictionary = OnlineDictionaries.labels(infopedia, pt, en)
        val labels = infopedia.map { byDictionary.getValue(it) }
        assertEquals(listOf("Infopédia \u00b7 PT\u2013EN", "Infopédia \u00b7 PT", "Infopédia \u00b7 verbs", "Verbix \u00b7 verbs", "Verbix \u00b7 nouns"), labels)
    }

    @Test
    fun storedUrlsAreNamedFromTheCatalog() {
        assertEquals("Google Translate", OnlineDictionaries.displayName("https://translate.google.com/?sl=pt&tl=ru&text=[WORD]&op=translate", pt, ru))
        assertEquals("Wiktionary PT", OnlineDictionaries.displayName("https://pt.wiktionary.org/wiki/[WORD]", pt, ru))
        assertEquals("Google Images", OnlineDictionaries.displayName("https://www.google.com/search?tbm=isch&q=[WORD]", pt, ru))
        assertEquals("Google", OnlineDictionaries.displayName("https://www.google.com/search?q=[WORD]", pt, ru))
        assertEquals("google-images", OnlineDictionaries.match("https://www.google.com/search?tbm=isch&q=[WORD]", pt, ru)?.id)
        assertEquals("Michaelis", OnlineDictionaries.displayName("https://michaelis.uol.com.br/busca?palavra=[WORD]", pt, ru))
        assertEquals("dle.rae.es".let { "RAE" }, OnlineDictionaries.displayName("https://dle.rae.es/[WORD]", LanguageOption("es", "Spanish"), en))
        assertEquals("unknown.example", OnlineDictionaries.displayName("https://unknown.example/x/[WORD]", pt, en))
    }

    @Test
    fun linksStoredWithAnOlderPlaceholderUseTheCurrentOne() {
        val url = LanguageDictionary.currentPlaceholder("https://example.org/search?q=[TERM]&lang=pt")
        assertEquals("https://example.org/search?q=[WORD]&lang=pt", url)
        assertEquals("https://example.org/search?q=casa&lang=pt", LanguageDictionary(useFor = DictionaryUse.TERMS, type = DictionaryType.POPUP, url = url).lookupUrl("casa"))
        // The older "###" placeholder still works as it is.
        assertEquals("https://example.org/casa", LanguageDictionary(useFor = DictionaryUse.TERMS, type = DictionaryType.POPUP, url = "https://example.org/###").lookupUrl("casa"))
    }
}
