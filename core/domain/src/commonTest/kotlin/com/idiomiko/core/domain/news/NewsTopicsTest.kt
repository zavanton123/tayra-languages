package com.idiomiko.core.domain.news

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The feeds' own sections, as they name them, sorted into the few topics the news is filtered by. */
class NewsTopicsTest {
    private fun of(category: String, language: String = "pt") = NewsTopics.of(category, language)

    @Test
    fun aPortugueseFeedsSectionsFallIntoBrazilsTopics() {
        // Notícias ao Minuto's, Agência Brasil's, DW's and Global Voices' sections.
        assertEquals(setOf(NewsTopic.WORLD), of("Mundo"))
        assertEquals(setOf(NewsTopic.POLITICS), of("Política"))
        assertEquals(setOf(NewsTopic.SPORT), of("Desporto"), "Portugal's word for sport is Brazil's Esporte")
        assertEquals(setOf(NewsTopic.SPORT), of("Esporte"))
        assertEquals(setOf(NewsTopic.LIFESTYLE), of("Lifestyle"))
        assertEquals(setOf(NewsTopic.LIFESTYLE), of("Auto"))
        assertEquals(setOf(NewsTopic.CULTURE), of("Fama"))
        assertEquals(setOf(NewsTopic.SCIENCE), of("Tech"))
        assertEquals(setOf(NewsTopic.SCIENCE), of("Ciência e Saúde"), "health is science's")
        assertEquals(setOf(NewsTopic.SCIENCE), of("Meio Ambiente"))
        assertEquals(setOf(NewsTopic.HOME), of("Brasil"))
        assertEquals(setOf(NewsTopic.HOME), of("Rio Grande do Sul"))
        assertEquals(setOf(NewsTopic.WORLD), of("América Latina"))
        // A feed's front page, Portugal's own news, and its citizen media are no topic.
        for (none in listOf("NOTÍCIAS", "País", "Mídia Cidadã", "Destaque", "Alemanha", "trama golpista", "")) assertEquals(emptySet(), of(none), none)
    }

    @Test
    fun aSectionIsMatchedWhateverItsCaseAndAccents() {
        assertEquals(setOf(NewsTopic.POLITICS), of("POLíTICA", "ca"))
        assertEquals(setOf(NewsTopic.SPORT), of("ΑΘΛΗΤΙΣΜΟΣ", "el"))
        assertEquals(setOf(NewsTopic.WORLD), of("Κοσμος", "el"))
        assertEquals(setOf(NewsTopic.CULTURE), of("Πολιτισμός", "el"), "culture is not politics")
        assertEquals(setOf(NewsTopic.HOME), of("ΕΛΛΑΔΑ", "el"))
    }

    @Test
    fun aSectionOfTwoTopicsIsInBoth() {
        assertEquals(setOf(NewsTopic.POLITICS, NewsTopic.ECONOMY), of("Політика й економіка", "uk"))
        assertEquals(setOf(NewsTopic.CULTURE, NewsTopic.LIFESTYLE), of("Культура и стиль жизни", "ru"))
    }

    @Test
    fun aWordIsMatchedFromTheStartOfAWord() {
        assertEquals(setOf(NewsTopic.SPORT), of("Sport", "de"))
        assertEquals(emptySet(), of("Transport", "de"))
        assertEquals(setOf(NewsTopic.SCIENCE), of("Mokslas ir IT", "lt"))
        assertEquals(emptySet(), of("Italia straordinaria", "en"), "IT is a word of its own")
        // Scripts without spaces are matched anywhere.
        assertEquals(setOf(NewsTopic.SCIENCE), of("科技创新", "zh"))
        assertEquals(setOf(NewsTopic.SPORT), of("กีฬา", "th"))
    }

    @Test
    fun sectionsThatOnlyLookLikeATopicAreNone() {
        assertEquals(emptySet(), of("Politi og kriminalitet", "no"), "politi is the police")
        assertEquals(setOf(NewsTopic.POLITICS), of("Politikk", "no"))
        assertEquals(emptySet(), of("Global Voices Podcast", "de"))
        assertEquals(emptySet(), of("Егемен Қазақстан", "kk"), "a paper's own name on every article")
    }

    @Test
    fun homeIsTheCountryALanguagesOutletsWriteAbout() {
        assertEquals(setOf(NewsTopic.HOME), of("Inland", "de"))
        assertEquals(setOf(NewsTopic.HOME), of("Kotimaa", "fi"))
        assertEquals(emptySet(), of("Deutschland", "pt"), "Germany is no home of Portuguese")
        assertEquals(emptySet(), of("UK news", "en"), "English has no one home")
        assertNull(NewsTopics.label(NewsTopic.HOME, "en"))
    }

    @Test
    fun anArticlesTopicsFollowItsSections() {
        val source = NewsSource("pt", "Notícias ao Minuto", "https://example.org/rss", "")
        val article = NewsArticle(source, "https://example.org/a", "A", "", null, topics = listOf("Destaque", "Economia", "Política", "Economia"))
        assertEquals(listOf(NewsTopic.ECONOMY, NewsTopic.POLITICS), NewsTopics.of(article))
    }

    @Test
    fun portugueseNamesTheTopicsAsBrazilDoes() {
        assertEquals(
            listOf("Brasil", "Mundo", "Política", "Economia", "Esporte", "Cultura", "Ciência & tech", "Estilo de vida"),
            NewsTopic.entries.map { NewsTopics.label(it, "pt") },
        )
    }

    @Test
    fun everyLanguageWithNewsNamesEveryTopicButPerhapsHome() {
        val languages = NewsSources.all.map { it.languageCode }.toSet()
        assertEquals(languages, NewsTopics.languages)
        for (code in languages) for (topic in NewsTopic.entries - NewsTopic.HOME) {
            assertTrue(!NewsTopics.label(topic, code).isNullOrBlank(), "$code has no name for $topic")
        }
    }
}
