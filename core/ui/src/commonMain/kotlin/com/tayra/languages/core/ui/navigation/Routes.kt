package com.tayra.languages.core.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations shared by all features. */
sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data object ArchivedBooks : Route

    @Serializable
    data object NewBook : Route

    @Serializable
    data class EditBook(val bookId: Long) : Route

    @Serializable
    data class Read(val bookId: Long, val page: Int? = null) : Route

    /** Exercises on the words being learned on a page. */
    @Serializable
    data class Practice(val bookId: Long, val page: Int) : Route
    @Serializable
    data class EditPage(val bookId: Long, val page: Int) : Route

    @Serializable
    data class Bookmarks(val bookId: Long) : Route

    @Serializable
    data class Terms(val termIds: List<Long>? = null, val bookId: Long? = null, val page: Int? = null) : Route

    @Serializable
    data class EditTerm(val termId: Long) : Route

    /** The term with this text, or a new one for it. */
    @Serializable
    data class EditTermByText(val languageId: Long, val text: String) : Route

    @Serializable
    data object NewTerm : Route

    /** The most common words of the language being learned, by how often they are used. */
    @Serializable
    data object WordFrequency : Route

    /** The vocabulary level and other vocabulary settings. */
    @Serializable
    data object VocabularySettings : Route

    @Serializable
    data class Examples(val languageId: Long, val text: String) : Route


    /** The courses for the language being learned. */
    @Serializable
    data object Courses : Route
    /** A course and its lessons. */
    @Serializable
    data class Course(val courseId: String) : Route

    /** A new course of the reader's own, for the language being learned. */
    @Serializable
    data object NewCourse : Route

    @Serializable
    data class EditCourse(val courseId: String) : Route

    /** A new lesson at the end of one of the reader's courses. */
    @Serializable
    data class NewLesson(val courseId: String) : Route

    @Serializable
    data class EditLesson(val courseId: String, val lessonId: String) : Route
    /** Reviewing the flashcards of the language being learned. */
    @Serializable
    data object Flashcards : Route
    @Serializable
    data object FlashcardSettings : Route
    @Serializable
    data object Languages : Route

    @Serializable
    data class EditLanguage(val languageId: Long) : Route

    @Serializable
    data class ManageDictionaries(val languageId: Long) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Shortcuts : Route

    /** Argos Translate runtime and language packages (desktop only). */
    @Serializable
    data object OfflineTranslation : Route

    /** Downloadable offline dictionary packs. */
    @Serializable
    data object OfflineDictionaries : Route

    /** Downloadable course packs, the ready-made courses of a language as one file. */
    @Serializable
    data object CoursePacks : Route

    /** Speech engines and their voices. */
    @Serializable
    data object Speech : Route

    /** Backups of all data and settings. */
    @Serializable
    data object Backups : Route

    @Serializable
    data object Stats : Route

    @Serializable
    data object About : Route
}
