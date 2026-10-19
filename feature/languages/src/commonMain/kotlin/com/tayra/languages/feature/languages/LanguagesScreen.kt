package com.tayra.languages.feature.languages

import com.tayra.languages.core.ui.i18n.tr
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
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
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LanguageFlag
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.navigation.Route
import org.koin.compose.viewmodel.koinViewModel
import com.tayra.languages.core.ui.components.LanguageMenuRow
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

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
    // Counts the changes made here, so each one shows "Changes saved" for a moment.
    var changes by remember { mutableIntStateOf(0) }
    val wide = LocalWindowWidth.current.isExpanded

    if (compact) {
        PhoneLanguagesScreen(settings, learning, viewModel, onNavigate, onBack)
        return
    }
    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader(tr("Languages"), tr("Choose what you learn, how translations appear, and the language of the app."), onBackToSettings = onBack) {
                if (!compact) SavedNote(changes)
            }
            Column(
                Modifier.fillMaxWidth().cardSurface(AppShapes.card)
                    .padding(if (compact) AppSpacing.l else AppSpacing.xxl),
            ) {
                Text(tr("Language setup"), style = AppText.sectionTitle)
                Spacer(Modifier.height(AppSpacing.xl))
                val learningTile: @Composable (Modifier) -> Unit = { m ->
                    LanguageTile(tr("Studying"), tr("I'm learning"), highlighted = true, modifier = m) {
                        FlagPicker(
                            options = learning?.languages.orEmpty(),
                            selected = learning?.languages?.firstOrNull { it.first == learning.currentId },
                            name = { tr(it.second) },
                            // Chosen as in the header, which also asks for the vocabulary level of a language new to the reader.
                            onSelect = { (id, _) -> learning?.onSelect?.invoke(id); changes++ },
                            modifier = Modifier.testTag("learning-language"),
                            flagName = { it.second },
                            unavailable = { if (learning != null && it.first in learning.nativeIds) tr("Your native language") else null },
                        )
                        TileDescription(tr("Used for books, courses, vocabulary and flashcards."))
                    }
                }
                val nativeTile: @Composable (Modifier) -> Unit = { m ->
                    LanguageTile(tr("Translations"), tr("Show meanings in"), modifier = m) {
                        FlagPicker(
                            options = LanguageCatalog.nativeLanguages,
                            selected = native,
                            name = { tr(it.name) },
                            onSelect = { viewModel.setNativeLanguage(it.code, learningCode); changes++ },
                            modifier = Modifier.testTag("native-language"),
                            flagName = { it.name },
                            unavailable = { if (it.code == learningCode) tr("You are learning it") else null },
                        )
                        TileDescription(tr("Used for translations, definitions and example sentences."))
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
                        TileDescription(tr("Used for menus, buttons and messages."))
                    }
                }
                if (wide) {
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        learningTile(Modifier.weight(1f).fillMaxHeight())
                        nativeTile(Modifier.weight(1f).fillMaxHeight())
                        uiTile(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                        learningTile(Modifier.fillMaxWidth())
                        nativeTile(Modifier.fillMaxWidth())
                        uiTile(Modifier.fillMaxWidth())
                    }
                }
            }
            if (compact) SavedNote(changes)
        }
    }
}

/** Settings are saved as soon as they change; after each of the [changes] this says so for a few seconds, in place of a Save button. */
@Composable
private fun SavedNote(changes: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(changes) {
        if (changes == 0) return@LaunchedEffect
        visible = true
        delay(2_500)
        visible = false
    }
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusTints.ok, modifier = Modifier.size(AppSizes.iconLarge))
            Spacer(Modifier.width(AppSpacing.m))
            Text(tr("Changes saved"), style = AppText.emphasis, color = StatusTints.ok)
        }
    }
}

/** One of the three choices: a small caps label, a title, then the picker and its notes; the language being learned is [highlighted]. */
@Composable
private fun LanguageTile(
    label: String,
    title: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.panel
    val background = if (highlighted) {
        Modifier.background(Brush.verticalGradient(listOf(colors.primary.copy(alpha = AppAlpha.tint), colors.primary.copy(alpha = AppAlpha.faint))))
    } else {
        Modifier.background(colors.surface)
    }
    Column(modifier.clip(shape).then(background).border(AppBorders.hairline(if (highlighted) colors.primary.copy(alpha = AppAlpha.disabled) else colors.outlineVariant), shape)) {
        if (highlighted) Box(Modifier.fillMaxWidth().height(HIGHLIGHT_STRIPE).background(colors.primary))
        val inset = if (LocalWindowWidth.current.isCompact) AppSpacing.xl else AppSpacing.xxl
        Column(Modifier.padding(start = inset, end = inset, top = inset + AppSpacing.xxs + if (highlighted) 0.dp else HIGHLIGHT_STRIPE, bottom = inset)) {
            Text(label.uppercase(), style = AppText.overline.copy(color = if (highlighted) colors.primary else colors.onSurfaceVariant))
            Spacer(Modifier.height(AppSpacing.s))
            Text(title, style = AppText.sectionTitle)
            Spacer(Modifier.height(AppSpacing.l))
            content()
        }
    }
}

@Composable
private fun TileDescription(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = AppSpacing.l))
}

/** A field showing the chosen language with its flag; it opens a menu of the [options], each with its flag. [name] is the name to show. */
@Composable
private fun <T> FlagPicker(
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

private val HIGHLIGHT_STRIPE = 4.dp
