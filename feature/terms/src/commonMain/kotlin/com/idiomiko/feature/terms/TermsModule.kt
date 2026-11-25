package com.idiomiko.feature.terms

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.idiomiko.core.ui.components.screenDialogProperties
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import com.idiomiko.core.domain.service.LocalTranslation
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.feature.terms.examples.ExamplesSearchScreen
import com.idiomiko.feature.terms.examples.ExamplesSearchViewModel
import com.idiomiko.feature.terms.examples.ExampleTerms
import com.idiomiko.feature.terms.form.TermEditDestination
import com.idiomiko.feature.terms.form.TermFormKey
import com.idiomiko.feature.terms.form.TermFormViewModel
import com.idiomiko.feature.terms.list.TermsListViewModel
import com.idiomiko.feature.terms.list.TermsScreen
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.idiomiko.feature.terms.list.PracticeSetupViewModel
import com.idiomiko.core.domain.practice.PracticeSource

val termsModule = module {
    single { ExampleTerms(get(), get()) }
    viewModel { (key: TermFormKey) -> TermFormViewModel(key, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), getOrNull(), getOrNull(), getOrNull(), getOrNull()) }
    viewModel { (termIds: List<Long>?) -> TermsListViewModel(termIds, get(), get(), get(), get(), get(), getOrNull()) }
    viewModel { (source: PracticeSource) -> PracticeSetupViewModel(source, get(), get(), get(), getOrNull(), getOrNull(), getOrNull(), getOrNull()) }
    viewModel { (languageId: Long, text: String) -> ExamplesSearchViewModel(languageId, text, get(), get(), get(), get(), get(), get<LocalTranslation>().translator, getOrNull()) }
}

fun NavGraphBuilder.termsGraph(navController: NavController) {
    val navigate: (Route) -> Unit = { navController.navigate(it) }
    val openParent: (Long, String) -> Unit = { languageId, text -> navController.navigate(Route.EditTermByText(languageId, text)) }
    composable<Route.Terms> { entry ->
        val route = entry.toRoute<Route.Terms>()
        // An empty list of ids does not survive the route and comes back as none, which would mean every term:
        // a reader's page, given by its book, shows its own terms even when it has none yet.
        val termIds = route.termIds ?: route.bookId?.let { emptyList() }
        TermsScreen(termIds = termIds, onNavigate = navigate, onBack = if (termIds != null) ({ navController.popBackStack() }) else null)
    }
    // The editor opens over the screen it was opened from; a click beside it keeps what is being typed.
    val editor = screenDialogProperties(dismissOnClickOutside = false)
    val close = { navController.popBackStack(); Unit }
    dialog<Route.EditTerm>(dialogProperties = editor) { entry ->
        val route = entry.toRoute<Route.EditTerm>()
        TermEditDestination(TermFormKey.ById(route.termId), navigate, close, openParent)
    }
    dialog<Route.EditTermByText>(dialogProperties = editor) { entry ->
        val route = entry.toRoute<Route.EditTermByText>()
        TermEditDestination(TermFormKey.ByText(route.languageId, route.text), navigate, close, openParent)
    }
    dialog<Route.NewTerm>(dialogProperties = editor) {
        TermEditDestination(TermFormKey.New, navigate, close, openParent)
    }
    composable<Route.Examples> { entry ->
        val route = entry.toRoute<Route.Examples>()
        ExamplesSearchScreen(languageId = route.languageId, text = route.text, onNavigate = navigate, onBack = { navController.popBackStack() })
    }
}
