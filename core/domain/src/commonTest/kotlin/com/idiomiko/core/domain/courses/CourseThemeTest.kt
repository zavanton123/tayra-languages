package com.idiomiko.core.domain.courses

import kotlin.test.Test
import kotlin.test.assertEquals

class CourseThemeTest {
    private fun course(topic: String, title: String = "Um curso") =
        Course(id = "c", languageCode = "pt", title = title, description = "", level = CourseLevel.B1, topic = topic, lessons = emptyList())

    @Test
    fun theTopicsOfTheAppsCoursesFindTheirThemes() {
        val expected = mapOf(
            "Home and family" to CourseTheme.HOME,
            "Daily routine" to CourseTheme.DAILY_LIFE,
            "Food and shopping" to CourseTheme.FOOD,
            "Getting around the city" to CourseTheme.TRANSPORT,
            "Everyday technology" to CourseTheme.TECHNOLOGY,
            "Everyday local news" to CourseTheme.NEWS,
            "Travel and places in Brazil" to CourseTheme.TRAVEL,
            "Art and culture" to CourseTheme.ART,
            "Free time and hobbies" to CourseTheme.LEISURE,
            "Nature and the environment" to CourseTheme.NATURE,
            "Career and studies" to CourseTheme.WORK,
            "Public health" to CourseTheme.HEALTH,
            "Sport" to CourseTheme.SPORT,
            "The science of cooking" to CourseTheme.FOOD,
            "Astronomy and the universe" to CourseTheme.SPACE,
            "The sea and the coast" to CourseTheme.SEA,
            "Brazilian music" to CourseTheme.MUSIC,
            "Theatre, circus and performance" to CourseTheme.THEATRE,
            "Film and television" to CourseTheme.FILM,
            "Scenes from Brazilian history" to CourseTheme.HISTORY,
            "Everyday philosophy" to CourseTheme.MIND,
            "Climate and energy" to CourseTheme.WEATHER,
            "Disaster prevention and resilience" to CourseTheme.SAFETY,
            "Everyday rights" to CourseTheme.LAW,
            "Animal behaviour" to CourseTheme.ANIMALS,
            "Time" to CourseTheme.TIME,
            "Community and neighbourhood life" to CourseTheme.CITY,
        )
        assertEquals(expected, expected.mapValues { (topic, _) -> CourseTheme.of(course(topic)) })
    }

    @Test
    fun shortKeywordsMatchWholeWordsOnly() {
        // "art" is not in "article", nor "sea" in "season".
        assertEquals(CourseTheme.NEWS, CourseTheme.of("Newspaper articles"))
        assertEquals(CourseTheme.WEATHER, CourseTheme.of("Seasons and weather"))
        assertEquals(CourseTheme.ART, CourseTheme.of("Visual arts"))
        assertEquals(CourseTheme.SCIENCE, CourseTheme.of("Mathematics in everyday life"))
    }

    @Test
    fun aCourseWithoutAKnownTopicFallsBackToItsTitleOrTheGeneralTheme() {
        assertEquals(CourseTheme.TRAVEL, CourseTheme.of(course(topic = "", title = "My travel diary")))
        assertEquals(CourseTheme.GENERAL, CourseTheme.of(course(topic = "Ensaios finais", title = "Minhas leituras")))
        assertEquals(CourseTheme.GENERAL, CourseTheme.of(course(topic = "")))
    }
}
