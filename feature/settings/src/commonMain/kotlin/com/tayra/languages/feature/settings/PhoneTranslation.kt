package com.tayra.languages.feature.settings

import com.tayra.languages.core.ui.components.ScrollColumn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.service.LocalPackage
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppBottomSheet
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LanguageFlag
import com.tayra.languages.core.ui.components.LanguagePickerSheet
import com.tayra.languages.core.ui.components.LinkText
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.Notice
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SelectButton
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.components.largeText
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/**
 * The Translation page on a phone: the language pair as two chips, the engine picked from a sheet
 * with its status under it, and the packages the pair needs with one button to install them all;
 * the whole catalogue is a sheet behind "Browse all".
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
    pair: PairState,
    packages: List<LocalPackage>,
    wanted: Set<String>,
    learningName: String?,
    nativeName: String,
) {
    val colors = MaterialTheme.colorScheme
    val learningEnglish = LocalLearningLanguage.current?.currentName
    val learningCode = learningEnglish?.let(LanguageCodes::codeFor)
    var nativeSheet by remember { mutableStateOf(false) }
    var engineSheet by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { AppTopBar(title = tr("Translation"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        ScrollColumn(Modifier.padding(padding).fillMaxWidth(), contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                Text(tr("Choose how translations are generated and stored."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                EngineStatusPill(settings, viewModel, local, checking, ready, pair)
            }
            ContentCard(tr("Language pair"), subtitle = null, icon = AppIcons.SwapHoriz, dense = true) {
                // The language learned is set in Languages, so here it is plain text, not a control that does nothing.
                val from: @Composable (Modifier) -> Unit = { modifier ->
                    Row(modifier.heightIn(min = AppSizes.control), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                        if (learningEnglish != null) LanguageFlag(learningEnglish, 24.dp)
                        Text(learningName ?: tr("Your language"), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                val to: @Composable (Modifier) -> Unit = { modifier ->
                    SelectButton(
                        nativeName,
                        onClick = { nativeSheet = true },
                        modifier = modifier.testTag("phone-native"),
                        leading = { LanguageFlag(LanguageCatalog.nativeOption(settings.nativeLanguage).name, 24.dp) },
                    )
                }
                // Large text leaves no room for two names side by side, so they stack.
                if (largeText) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        from(Modifier.fillMaxWidth())
                        Icon(AppIcons.ArrowDown, contentDescription = tr("translated into"), tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconMedium))
                        to(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                        from(Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = tr("translated into"), tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconMedium))
                        to(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(AppSpacing.m))
                Text(
                    learningName?.let { tr("{0} terms will be translated into {1}.", it, nativeName) } ?: tr("Your terms will be translated into {0}.", nativeName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                LinkText(tr("Change the language you learn"), onClick = { onNavigate(Route.Languages) }, style = MaterialTheme.typography.bodyMedium)
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
                    local -> PhoneEngineStatus(settings, viewModel, pair.ready)
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
            if (local) DownloadsLinkCard(modelsInDownloads(), onNavigate)
        }
    }
    if (nativeSheet) {
        LanguagePickerSheet(
            tr("Native language"), LanguageCatalog.nativeLanguages, selected = LanguageCatalog.nativeOption(settings.nativeLanguage),
            name = { tr(it.name) }, flagName = { it.name },
            unavailable = { if (it.code == learningCode) tr("You are learning it") else null },
            onDismiss = { nativeSheet = false },
            onPick = { option -> nativeSheet = false; viewModel.update { it.copy(nativeLanguage = option.code) } },
        )
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
