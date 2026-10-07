package com.tayra.languages.core.domain.stats

import com.tayra.languages.core.domain.model.TermStatus
import kotlinx.datetime.LocalDate

/** What the database holds about the reader's work in one language, beyond the words read. */
data class LanguageActivity(
    val termsByStatus: Map<TermStatus, Int> = emptyMap(),
    /** Words the reader saved, by the day they were saved; those the vocabulary level marked known are left out. */
    val termsSaved: Map<LocalDate, Int> = emptyMap(),
    /** Flashcard answers by day, and how many of them were not "Again". */
    val reviews: Map<LocalDate, Int> = emptyMap(),
    val reviewsRemembered: Int = 0,
    val pagesRead: Int = 0,
    val booksFinished: Int = 0,
)

/** Reads [LanguageActivity] from the database. */
fun interface LearningStatsRepository {
    suspend fun activity(languageId: Long): LanguageActivity
}

/** One language's progress, ready to show. */
data class LanguageOverview(
    val languageName: String,
    val wordsRead: Int,
    val wordsReadThisWeek: Int,
    val daysRead: Int,
    /** Words read on an average day with reading. */
    val wordsPerReadingDay: Int,
    val streak: Int,
    val longestStreak: Int,
    /** Words read by day, for the days with reading. */
    val readingDays: Map<LocalDate, Int>,
    val known: Int,
    val learning: Int,
    val ignored: Int,
    val termsByStatus: Map<TermStatus, Int>,
    /** Words saved in each of the last weeks, oldest first, each week ending on [today]'s weekday. */
    val savedByWeek: List<Int>,
    val savedThisWeek: Int,
    val reviewsByDay: Map<LocalDate, Int>,
    val reviewsTotal: Int,
    /** The share of answers that were not "Again", from 0 to 100; null without reviews. */
    val rememberedPercent: Int?,
    val pagesRead: Int,
    val booksFinished: Int,
    val today: LocalDate,
)
