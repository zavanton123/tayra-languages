package com.tayra.languages.feature.terms.form

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Standalone term editor (from the term listing). */
@Composable
fun TermEditScreen(
    key: TermFormKey,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenParent: (languageId: Long, text: String) -> Unit,
) {
    val viewModel = koinViewModel<TermFormViewModel>(key = "term-form-$key") { parametersOf(key) }
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is TermFormEvent.Saved -> if (!event.keepOpen) onDone()
            TermFormEvent.Deleted -> onDone()
            is TermFormEvent.OpenParent -> onOpenParent(event.languageId, event.text)
        }
    }
    val title = when (key) {
        is TermFormKey.New -> "New term"
        else -> "Edit term"
    }
    Scaffold(topBar = { AppTopBar(title = title, onNavigate = onNavigate, onBack = onBack) }) { padding ->
        TermFormPanel(
            viewModel = viewModel,
            modifier = Modifier.padding(padding).fillMaxSize().widthIn(max = 720.dp),
            onDuplicateClick = { onNavigate(Route.EditTerm(it)) },
            onOpenExamples = { languageId, text -> onNavigate(Route.Examples(languageId, text)) },
        )
    }
}
