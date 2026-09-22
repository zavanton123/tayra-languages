package com.tayra.languages.feature.books

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.domain.service.PagePosition
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val booksModule = module {
    viewModel { (archived: Boolean) -> BooksViewModel(archived, get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (bookId: Long?, importUrl: String?) -> BookFormViewModel(bookId, importUrl, get(), get(), get(), get(), get()) }
    viewModel { (bookId: Long) -> BookmarksViewModel(bookId, get()) }
    viewModel { (mode: PageEditMode) -> PageEditViewModel(mode, get(), get(), get()) }
}

fun NavGraphBuilder.booksGraph(navController: NavController) {
    val navigate: (Route) -> Unit = { navController.navigate(it) }
    composable<Route.Home> {
        BooksScreen(archived = false, onNavigate = navigate)
    }
    composable<Route.ArchivedBooks> {
        BooksScreen(archived = true, onNavigate = navigate, onBack = { navController.popBackStack() })
    }
    composable<Route.NewBook> { entry ->
        val route = entry.toRoute<Route.NewBook>()
        BookFormScreen(
            bookId = null,
            importUrl = route.importUrl,
            onNavigate = navigate,
            onBack = { navController.popBackStack() },
            onSaved = { id, _ -> navController.navigate(Route.Read(id, 1)) { popUpTo<Route.Home>() } },
        )
    }
    composable<Route.EditBook> { entry ->
        val route = entry.toRoute<Route.EditBook>()
        BookFormScreen(
            bookId = route.bookId,
            importUrl = null,
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
            mode = PageEditMode.Edit(route.bookId, route.page),
            onNavigate = navigate,
            onBack = { navController.popBackStack() },
            onSaved = { page -> navController.navigate(Route.Read(route.bookId, page)) { popUpTo<Route.Home>() } },
        )
    }
    composable<Route.NewPage> { entry ->
        val route = entry.toRoute<Route.NewPage>()
        PageEditScreen(
            mode = PageEditMode.New(route.bookId, route.page, if (route.after) PagePosition.AFTER else PagePosition.BEFORE),
            onNavigate = navigate,
            onBack = { navController.popBackStack() },
            onSaved = { page -> navController.navigate(Route.Read(route.bookId, page)) { popUpTo<Route.Home>() } },
        )
    }
}
