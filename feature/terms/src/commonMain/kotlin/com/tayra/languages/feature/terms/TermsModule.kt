package com.tayra.languages.feature.terms

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.feature.terms.form.TermEditScreen
import com.tayra.languages.feature.terms.form.TermFormKey
import com.tayra.languages.feature.terms.form.TermFormViewModel
import com.tayra.languages.feature.terms.import.TermImportScreen
import com.tayra.languages.feature.terms.import.TermImportViewModel
import com.tayra.languages.feature.terms.list.TermsListViewModel
import com.tayra.languages.feature.terms.list.TermsScreen
import kotlinx.serialization.Serializable
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val termsModule = module {
    viewModel { (key: TermFormKey) -> TermFormViewModel(key, get(), get(), get(), get(), get(), get()) }
    viewModel { (termIds: List<Long>?) -> TermsListViewModel(termIds, get(), get(), get(), get()) }
    viewModel { TermImportViewModel(get()) }
}

/** Editing a term found by text, e.g. when following a parent link. */
@Serializable
internal data class EditTermByText(val languageId: Long, val text: String)

fun NavGraphBuilder.termsGraph(navController: NavController) {
    val navigate: (Route) -> Unit = { navController.navigate(it) }
    val openParent: (Long, String) -> Unit = { languageId, text -> navController.navigate(EditTermByText(languageId, text)) }
    composable<Route.Terms> { entry ->
        val route = entry.toRoute<Route.Terms>()
        TermsScreen(termIds = route.termIds, onNavigate = navigate, onBack = if (route.termIds != null) ({ navController.popBackStack() }) else null)
    }
    composable<Route.EditTerm> { entry ->
        val route = entry.toRoute<Route.EditTerm>()
        TermEditScreen(TermFormKey.ById(route.termId), navigate, { navController.popBackStack() }, { navController.popBackStack() }, openParent)
    }
    composable<EditTermByText> { entry ->
        val route = entry.toRoute<EditTermByText>()
        TermEditScreen(TermFormKey.ByText(route.languageId, route.text), navigate, { navController.popBackStack() }, { navController.popBackStack() }, openParent)
    }
    composable<Route.NewTerm> {
        TermEditScreen(TermFormKey.New, navigate, { navController.popBackStack() }, { navController.popBackStack() }, openParent)
    }
    composable<Route.ImportTerms> { TermImportScreen(onNavigate = navigate) }
}
