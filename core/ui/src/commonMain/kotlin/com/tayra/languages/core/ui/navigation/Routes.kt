package com.tayra.languages.core.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations shared by all features. */
sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data object ArchivedBooks : Route

    @Serializable
    data class NewBook(val importUrl: String? = null) : Route

    @Serializable
    data class EditBook(val bookId: Long) : Route

    @Serializable
    data class Read(val bookId: Long, val page: Int? = null) : Route

    @Serializable
    data class EditPage(val bookId: Long, val page: Int) : Route

    @Serializable
    data class NewPage(val bookId: Long, val page: Int, val after: Boolean) : Route

    @Serializable
    data class Bookmarks(val bookId: Long) : Route

    @Serializable
    data class Terms(val termIds: List<Long>? = null, val bookId: Long? = null, val page: Int? = null) : Route

    @Serializable
    data class EditTerm(val termId: Long) : Route

    @Serializable
    data object NewTerm : Route

    @Serializable
    data object TermTags : Route

    @Serializable
    data object ImportTerms : Route

    @Serializable
    data object Languages : Route

    @Serializable
    data class EditLanguage(val languageId: Long) : Route

    @Serializable
    data class NewLanguage(val predefinedName: String? = null) : Route

    @Serializable
    data object PredefinedLanguages : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Shortcuts : Route

    @Serializable
    data object Stats : Route

    @Serializable
    data object About : Route
}
