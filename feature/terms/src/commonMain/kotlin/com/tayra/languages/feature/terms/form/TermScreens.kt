package com.tayra.languages.feature.terms.form

import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppSpacing
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.Crumb
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SecondaryButton
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is TermFormEvent.Saved -> if (!event.keepOpen) onDone()
            TermFormEvent.Deleted -> onDone()
            is TermFormEvent.OpenParent -> onOpenParent(event.languageId, event.text)
            is TermFormEvent.OpenTerm -> onOpenParent(event.languageId, event.text)
        }
    }
    val isNew = key is TermFormKey.New
    val compact = LocalWindowWidth.current.isCompact
    val gutter = if (compact) AppSpacing.l else AppSpacing.xxxl
    Scaffold(topBar = { AppTopBar(title = if (isNew) tr("New term") else tr("Edit term"), onNavigate = onNavigate, section = NavSection.TERMS, onBack = if (compact) onBack else null) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PageHeader(
                title = if (isNew) tr("New term") else tr("Edit term"),
                compact = compact,
                gutter = gutter,
                saving = state.saving,
                canSave = state.draft.text.isNotBlank(),
                onBack = onBack,
                onTerms = { onNavigate(Route.Terms()) },
                onSave = viewModel::save,
            )
            TermFormPanel(
                viewModel = viewModel,
                modifier = Modifier.weight(1f).padding(horizontal = gutter - AppSpacing.l),
                onDuplicateClick = { onNavigate(Route.EditTerm(it)) },
                onOpenExamples = { languageId, text -> onNavigate(Route.Examples(languageId, text)) },
                onManageDictionaries = { languageId -> onNavigate(Route.ManageDictionaries(languageId)) },
            )
        }
    }
}

@Composable
private fun PageHeader(
    title: String,
    compact: Boolean,
    gutter: androidx.compose.ui.unit.Dp,
    saving: Boolean,
    canSave: Boolean,
    onBack: () -> Unit,
    onTerms: () -> Unit,
    onSave: () -> Unit,
) {
    // On a phone the bar has the way back and the title; the buttons stay.
    val crumbs = if (compact) emptyList() else listOf(Crumb(tr("Vocabulary"), onTerms), Crumb(title))
    Column(Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = AppSpacing.l)) {
        ScreenHeader(title, subtitle = null, crumbs = crumbs, onBack = onBack) {
            SecondaryButton(tr("Cancel"), onClick = onBack)
            PrimaryButton(if (saving) tr("Saving...") else tr("Save changes"), onClick = onSave, enabled = canSave && !saving)
        }
    }
}
