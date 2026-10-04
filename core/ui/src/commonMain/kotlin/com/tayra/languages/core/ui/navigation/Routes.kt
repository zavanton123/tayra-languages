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
    data class Examples(val languageId: Long, val text: String) : Route


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
