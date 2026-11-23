package com.idiomiko.feature.settings

import com.idiomiko.core.ui.components.ScrollColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.service.TranslationEngine
import com.idiomiko.core.domain.settings.UserSettings
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppProgressBar
import com.idiomiko.core.ui.components.AppTextField
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.Notice
import com.idiomiko.core.ui.components.PickerSheet
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SelectButton
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.Tag
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppSpacing

/**
 * The Translation page on a phone: the engine picked from a sheet with its status under it.
 */
@Composable
internal fun PhoneTranslationScreen(
    settings: UserSettings,
    viewModel: OfflineTranslationViewModel,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    local: Boolean,
    checking: Boolean,
    ready: Boolean,
    pairReady: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    var engineSheet by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { AppTopBar(title = tr("Translation"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        ScrollColumn(Modifier.padding(padding).fillMaxWidth(), contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                Text(tr("Choose how translations are generated and stored."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                EngineStatusPill(settings, viewModel, local, checking, ready, pairReady)
            }
            ContentCard(tr("Translation engine"), subtitle = null, icon = AppIcons.Memory, dense = true) {
                val engine = settings.translationEngine
                SelectButton(engine.shortName(viewModel.localName), onClick = { engineSheet = true }, modifier = Modifier.fillMaxWidth().testTag("phone-engine"))
                Spacer(Modifier.height(AppSpacing.m))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    if (local) Tag(tr("Offline"), StatusTints.ok) else Tag(tr("Online"), colors.primary)
                    if (engine == TranslationEngine.GOOGLE) Tag(tr("Paid"), StatusTints.warning) else Tag(tr("Free"), colors.primary)
                }
                Spacer(Modifier.height(AppSpacing.m))
                when {
                    local -> PhoneEngineStatus(settings, viewModel, pairReady)
                    engine == TranslationEngine.GOOGLE -> GoogleSettings(settings, viewModel)
                    else -> AppTextField(
                        value = settings.translationContactEmail,
                        onValueChange = { v -> viewModel.update { it.copy(translationContactEmail = v.trim()) } },
                        label = { Text(tr("Contact email (optional)")) },
                        supportingText = { Text(tr("Raises MyMemory's free daily quota from about 5,000 to 50,000 characters.")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    if (engineSheet) {
        PickerSheet(
            tr("Translation engine"), viewModel.engines, selected = settings.translationEngine,
            label = { it.displayLabel(viewModel.localName.ifEmpty { null }) },
            onDismiss = { engineSheet = false },
            onPick = { e -> engineSheet = false; viewModel.update { it.copy(translationEngine = e) } },
        )
    }
}

/** Whether the on-device translator works, with the button to check or install it; the runtime path field is left to the desktop. */
@Composable
private fun PhoneEngineStatus(settings: UserSettings, viewModel: OfflineTranslationViewModel, pairReady: Boolean) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    // Amber until the pair's packages are in, as the status pill above says.
    val tint = when {
        busy -> MaterialTheme.colorScheme.outline
        ready && pairReady -> StatusTints.ok
        else -> StatusTints.warning
    }
    Notice(
        title = when {
            busy -> tr("Checking…")
            ready && pairReady -> tr("Installed and working")
            ready -> tr("Installed. The language pair still needs a download.")
            else -> tr("Not installed")
        },
        message = status?.let { tr(it) } ?: if (!ready && viewModel.hasRuntimeSetup) tr("Install downloads a private Python into the app folder with argostranslate in it.") else null,
        tint = tint,
        // A tick only when everything works; amber with a warning sign while something is still missing.
        icon = if (ready && pairReady) Icons.Default.CheckCircle else Icons.Default.Warning,
        busy = busy,
        action = {
            if (viewModel.hasRuntimeSetup && !ready && !busy) {
                PrimaryButton(tr("Install"), onClick = viewModel::install)
            } else {
                SecondaryButton(if (viewModel.hasRuntimeSetup) tr("Check installation") else tr("Check models"), onClick = viewModel::check, enabled = !busy)
            }
        },
    ) {
        progress?.let {
            Text(tr(it), style = MaterialTheme.typography.bodySmall)
            AppProgressBar(null)
        }
    }
}
