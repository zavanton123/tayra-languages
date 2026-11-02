package com.tayra.languages.feature.terms.form

import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.Crumb
import com.tayra.languages.core.ui.components.DialogCard
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** How wide the term editor's dialog grows on a large window. */
internal val TERM_DIALOG_WIDTH = 1200.dp

/**
 * The term editor as a navigation destination's dialog window: over the screen it was opened
 * from on wider windows, and filling a phone's screen as a page of its own.
 */
@Composable
fun TermEditDestination(
    key: TermFormKey,
    onNavigate: (Route) -> Unit,
    onClose: () -> Unit,
    onOpenParent: (languageId: Long, text: String) -> Unit,
) {
    if (LocalWindowWidth.current.isCompact) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
            TermEditScreen(key, onNavigate, onClose, onClose, onOpenParent)
        }
    } else {
        DialogCard(Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing).testTag("term-dialog"), maxWidth = TERM_DIALOG_WIDTH) {
            TermEditScreen(key, onNavigate, onClose, onClose, onOpenParent, dialog = true)
        }
    }
}

/** The term editor: a phone's page, or the content of a dialog over another screen. */
@Composable
fun TermEditScreen(
    key: TermFormKey,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenParent: (languageId: Long, text: String) -> Unit,
    /** Shown over another screen: a title and close above the form, its delete and save below. */
    dialog: Boolean = false,
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
    val title = if (isNew) tr("New term") else tr("Edit term")
    val panel: @Composable (Modifier) -> Unit = { modifier ->
        TermFormPanel(
            viewModel = viewModel,
            modifier = modifier,
            onDuplicateClick = { onNavigate(Route.EditTerm(it)) },
            onOpenExamples = { languageId, text -> onNavigate(Route.Examples(languageId, text)) },
            onManageDictionaries = { languageId -> onNavigate(Route.ManageDictionaries(languageId)) },
            showDelete = !dialog,
        )
    }
    if (dialog) {
        var confirmDelete by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = AppSpacing.xxxl, end = AppSpacing.xl, top = AppSpacing.l, bottom = AppSpacing.l), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = AppText.screenTitle)
                AppIconButton(Icons.Default.Close, contentDescription = tr("Close"), onClick = onBack, modifier = Modifier.testTag("term-dialog-close"), style = IconButtonStyle.Outlined)
            }
            AppDivider()
            panel(Modifier.weight(1f).padding(horizontal = AppSpacing.l))
            AppDivider()
            Row(
                Modifier.fillMaxWidth().padding(start = AppSpacing.xl, end = AppSpacing.xxxl, top = AppSpacing.l, bottom = AppSpacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
            ) {
                if (!isNew && !state.loading) QuietButton(tr("Delete term"), onClick = { confirmDelete = true }, modifier = Modifier.testTag("term-delete"), icon = Icons.Default.Delete, tone = ButtonTone.Danger)
                Spacer(Modifier.weight(1f))
                SecondaryButton(tr("Cancel"), onClick = onBack)
                PrimaryButton(if (state.saving) tr("Saving...") else tr("Save changes"), onClick = viewModel::save, enabled = state.draft.text.isNotBlank() && !state.saving)
            }
        }
        if (confirmDelete) ConfirmDeleteTerm(state.draft.text, onConfirm = { viewModel.delete(); confirmDelete = false }, onDismiss = { confirmDelete = false })
        return
    }
    val compact = LocalWindowWidth.current.isCompact
    val gutter = if (compact) AppSpacing.l else AppSpacing.xxxl
    Scaffold(topBar = { AppTopBar(title = title, onNavigate = onNavigate, section = NavSection.TERMS, onBack = if (compact) onBack else null) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PageHeader(
                title = title,
                compact = compact,
                gutter = gutter,
                saving = state.saving,
                canSave = state.draft.text.isNotBlank(),
                onBack = onBack,
                onTerms = { onNavigate(Route.Terms()) },
                onSave = viewModel::save,
            )
            panel(Modifier.weight(1f).padding(horizontal = gutter - AppSpacing.l))
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
