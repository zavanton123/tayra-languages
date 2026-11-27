package com.idiomiko.feature.languages

import com.idiomiko.core.ui.i18n.tr
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.language.LanguageCatalog
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.ui.components.AppMenu
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.SettingsSection
import com.idiomiko.core.ui.components.SettingsPane
import com.idiomiko.core.ui.components.rememberToastState
import com.idiomiko.core.ui.components.ToastHost
import com.idiomiko.core.ui.components.AddLanguageDialog
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.LanguageFlag
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.navigation.Route
import org.koin.compose.viewmodel.koinViewModel
import com.idiomiko.core.ui.components.LanguageMenuRow
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** The language being learned, the native language meanings are shown in, and the language of the interface. */
@Composable
fun LanguagesScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: LanguagesViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val learning = LocalLearningLanguage.current
    val native = LanguageCatalog.nativeOption(settings.nativeLanguage)
    // The language learned and the native one are never the same: each picker lists the other's dimmed.
    val learningCode = learning?.currentName?.let(LanguageCodes::codeFor)
    val ui = LanguageCatalog.interfaceOption(settings.uiLanguage)
    val compact = LocalWindowWidth.current.isCompact
    // Counts the changes made here, each saved at once and said so in a snackbar, in place of a Save button.
    var changes by remember { mutableIntStateOf(0) }
    val toast = rememberToastState()
    ToastHost(toast)
    LaunchedEffect(changes) { if (changes > 0) toast.show(tr("Changes saved")) }
    val removing by viewModel.removing.collectAsStateWithLifecycle()
    val removals by viewModel.removals.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    removing?.let { RemoveLanguageDialog(it, onConfirm = viewModel::remove, onDismiss = viewModel::dismissRemoval) }

    // Added, a language is the one learned from then on, asking for its level when it never had one.
    if (adding && learning != null) {
        AddLanguageDialog(learning, onDismiss = { adding = false }, onSelect = { learning.select(it); changes++ })
    }
    if (compact) {
        PhoneLanguagesScreen(settings, learning, viewModel, onNavigate, onBack, onChanged = { changes++ })
        return
    }
    Scaffold(
        topBar = { AppTopBar(title = "Idiomiko", onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        SettingsPane(SettingsSection.LANGUAGES, padding, onNavigate) { inner ->
            // The room beside the settings sidebar, which may hold one column only.
            val wide = LocalWindowWidth.current.isExpanded
            PageColumn(inner) {
                ScreenHeader(tr("Languages"), null, onBackToSettings = onBack)
                // The languages being learned, switched between and added to here, then the two other choices.
                if (learning != null) MyLanguagesCard(learning, viewModel::askToRemove, Modifier.fillMaxWidth(), removals, onLearn = { learning.select(it); changes++ })
                PrimaryButton(tr("Add a language"), onClick = { adding = true }, icon = Icons.Default.Add, modifier = Modifier.testTag("add-language-button"))
                val nativeTile: @Composable (Modifier) -> Unit = { m ->
                    LanguageTile(tr("Meanings"), tr("Show meanings in"), modifier = m) {
                        FlagPicker(
                            options = LanguageCatalog.nativeLanguages,
                            selected = native,
                            name = { tr(it.name) },
                            onSelect = { viewModel.setNativeLanguage(it.code, learningCode); changes++ },
                            modifier = Modifier.testTag("native-language"),
                            flagName = { it.name },
                            unavailable = { if (it.code == learningCode) tr("You are learning it") else null },
                        )
                    }
                }
                val uiTile: @Composable (Modifier) -> Unit = { m ->
                    LanguageTile(tr("App"), tr("Interface language"), modifier = m) {
                        FlagPicker(
                            options = LanguageCatalog.interfaceLanguages,
                            selected = ui,
                            name = { it.name },
                            onSelect = { viewModel.setInterfaceLanguage(it.code); changes++ },
                            modifier = Modifier.testTag("interface-language"),
                            flagName = { LanguageCatalog.nativeOption(it.code).name },
                        )
                    }
                }
                if (wide) {
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        nativeTile(Modifier.weight(1f).fillMaxHeight())
                        uiTile(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                        nativeTile(Modifier.fillMaxWidth())
                        uiTile(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/** One of the language choices: a small caps label, a title, then the picker and its notes. */
@Composable
internal fun LanguageTile(
    label: String,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.panel
    Column(modifier.clip(shape).background(colors.surface).border(AppBorders.hairline(colors.outlineVariant), shape)) {
        val inset = if (LocalWindowWidth.current.isCompact) AppSpacing.xl else AppSpacing.xxl
        Column(Modifier.padding(start = inset, end = inset, top = inset + AppSpacing.xxs, bottom = inset)) {
            Text(label.uppercase(), style = AppText.overline.copy(color = colors.onSurfaceVariant))
            Spacer(Modifier.height(AppSpacing.s))
            Text(title, style = AppText.sectionTitle)
            Spacer(Modifier.height(AppSpacing.l))
            content()
        }
    }
}

/** A field showing the chosen language with its flag; it opens a menu of the [options], each with its flag. [name] is the name to show. */
@Composable
internal fun <T> FlagPicker(
    options: List<T>,
    selected: T?,
    name: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    /** The English name the flag is looked up by, when [name] is not it. */
    flagName: (T) -> String = name,
    /** Why an option cannot be chosen, or null when it can; such options are listed dimmed. */
    unavailable: (T) -> String? = { null },
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(AppShapes.compact).background(colors.surface)
                .border(AppBorders.hairline(colors.outline.copy(alpha = AppAlpha.muted)), AppShapes.compact)
                .clickable(enabled = options.isNotEmpty()) { open = true }
                .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected != null) {
                LanguageFlag(flagName(selected), 26.dp)
                Spacer(Modifier.width(AppSpacing.l))
            }
            Text(
                selected?.let(name) ?: "",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant)
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                val chosen = option == selected
                val note = if (chosen) null else unavailable(option)
                AppMenuItem(
                    text = { LanguageMenuRow(flagName(option), name(option), chosen, note) },
                    onClick = { open = false; if (!chosen) onSelect(option) },
                    enabled = note == null,
                )
            }
        }
    }
}

