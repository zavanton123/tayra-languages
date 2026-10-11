package com.tayra.languages.feature.books

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val booksModule = module {
    viewModel { (archived: Boolean) -> BooksViewModel(archived, get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (bookId: Long?) -> BookFormViewModel(bookId, get(), get(), get(), get()) }
    viewModel { (bookId: Long) -> BookmarksViewModel(bookId, get()) }
    viewModel { (bookId: Long, page: Int) -> PageEditViewModel(bookId, page, get(), get(), get()) }
}

fun NavGraphBuilder.booksGraph(navController: NavController) {
    val navigate: (Route) -> Unit = { navController.navigate(it) }
    composable<Route.Books> {
        BooksScreen(archived = false, onNavigate = navigate)
    }
    composable<Route.ArchivedBooks> {
        BooksScreen(archived = true, onNavigate = navigate, onBack = { navController.popBackStack() })
    }
    composable<Route.NewBook> {
        BookFormScreen(
            bookId = null,
            onNavigate = navigate,
            onBack = { navController.popBackStack() },
            onSaved = { id, _ -> navController.navigate(Route.Read(id, 1)) { popUpTo<Route.NewBook> { inclusive = true } } },
        )
    }
    composable<Route.EditBook> { entry ->
        val route = entry.toRoute<Route.EditBook>()
        BookFormScreen(
            bookId = route.bookId,
            onNavigate = navigate,
            onBack = { navController.popBackStack() },
            onSaved = { _, _ -> navController.popBackStack() },
        )
    }
    composable<Route.Bookmarks> { entry ->
        val route = entry.toRoute<Route.Bookmarks>()
        BookmarksScreen(bookId = route.bookId, onNavigate = navigate, onBack = { navController.popBackStack() })
    }
    composable<Route.EditPage> { entry ->
        val route = entry.toRoute<Route.EditPage>()
        PageEditScreen(
            bookId = route.bookId,
            page = route.page,
            onNavigate = navigate,
            onBack = { navController.popBackStack() },
            // The page edited replaces the reader it was opened from.
            onSaved = { page -> navController.navigate(Route.Read(route.bookId, page)) { popUpTo<Route.Read> { inclusive = true } } },
        )
    }
}
