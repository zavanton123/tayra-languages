package com.tayra.languages.feature.languages

import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCatalog
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
import com.tayra.languages.core.ui.navigation.Route
import org.koin.compose.viewmodel.koinViewModel

/** The language being learned, the native language meanings are shown in, and the language of the interface. */
@Composable
fun LanguagesScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: LanguagesViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val learning = LocalLearningLanguage.current
    val learningName = learning?.currentName
    val native = LanguageCatalog.nativeOption(settings.nativeLanguage)
    val ui = LanguageCatalog.interfaceOption(settings.uiLanguage)
    val compact = LocalWindowWidth.current.isCompact
    // Counts the changes made here, so each one shows "Changes saved" for a moment.
    var changes by remember { mutableIntStateOf(0) }
    val wide = LocalWindowWidth.current.isExpanded

    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader(tr("Languages"), tr("Choose what you learn, how translations appear, and the language of the app."), onBackToSettings = onBack) {
                if (!compact) SavedNote(changes)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(if (compact) 16.dp else 24.dp),
            ) {
                Text(tr("Language setup"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
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
                            onSelect = { viewModel.setNativeLanguage(it.code); changes++ },
                            modifier = Modifier.testTag("native-language"),
                            flagName = { it.name },
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
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        learningTile(Modifier.weight(1f).fillMaxHeight())
                        nativeTile(Modifier.weight(1f).fillMaxHeight())
                        uiTile(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        learningTile(Modifier.fillMaxWidth())
                        nativeTile(Modifier.fillMaxWidth())
                        uiTile(Modifier.fillMaxWidth())
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SetupSummary(learningName, native.name, ui.name, wide)
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
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusTints.ok, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(tr("Changes saved"), style = MaterialTheme.typography.bodyLarge, color = StatusTints.ok, fontWeight = FontWeight.Medium)
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
    val shape = RoundedCornerShape(12.dp)
    val background = if (highlighted) {
        Modifier.background(Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.10f), colors.primary.copy(alpha = 0.03f))))
    } else {
        Modifier.background(colors.surface)
    }
    Column(modifier.clip(shape).then(background).border(1.dp, if (highlighted) colors.primary.copy(alpha = 0.35f) else colors.outlineVariant, shape)) {
        if (highlighted) Box(Modifier.fillMaxWidth().height(4.dp).background(colors.primary))
        val inset = if (LocalWindowWidth.current.isCompact) 18.dp else 24.dp
        Column(Modifier.padding(start = inset, end = inset, top = inset + if (highlighted) 2.dp else 6.dp, bottom = inset)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (highlighted) colors.primary else colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun TileDescription(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
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
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(colors.surface)
                .border(1.dp, colors.outline.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .clickable(enabled = options.isNotEmpty()) { open = true }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected != null) {
                LanguageFlag(flagName(selected), 26.dp)
                Spacer(Modifier.width(16.dp))
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
                AppMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LanguageFlag(flagName(option), 18.dp)
                            Text(name(option), Modifier.weight(1f), fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal)
                            if (chosen) Icon(Icons.Default.Check, contentDescription = tr("Selected"), tint = colors.primary, modifier = Modifier.size(18.dp))
                        }
                    },
                    onClick = { open = false; if (!chosen) onSelect(option) },
                )
            }
        }
    }
}

/**
 * The three choices in one line: learning → native, in words, and the interface language.
 * [learning] and [native] are English names; [ui] is the interface language in its own name.
 */
@Composable
private fun SetupSummary(learning: String?, native: String, ui: String, wide: Boolean) {
    val sentence = if (learning != null) {
        tr("Learn {0} with {1} translations", languageInSentence(learning), languageInSentence(native))
    } else {
        tr("Meanings in {0}", languageInSentence(native, LanguageCase.PREPOSITIONAL))
    }
    val pair: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (learning != null) {
                LanguageChip(learning, wide)
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = tr("with translations in"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = if (wide) 14.dp else 8.dp).size(22.dp),
                )
            }
            LanguageChip(native, wide)
        }
    }
    val note: @Composable (String) -> Unit = { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    if (wide) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Your setup"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(40.dp))
            pair()
            VerticalDivider(Modifier.padding(horizontal = 32.dp).height(48.dp), color = MaterialTheme.colorScheme.outlineVariant)
            note(sentence)
            VerticalDivider(Modifier.padding(horizontal = 32.dp).height(48.dp), color = MaterialTheme.colorScheme.outlineVariant)
            note(tr("App interface: {0}", ui))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr("Your setup"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            pair()
            note(sentence)
            note(tr("App interface: {0}", ui))
        }
    }
}

/** A language by its English [name], shown in the interface language. */
@Composable
private fun LanguageChip(name: String, roomy: Boolean) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
            .padding(horizontal = if (roomy) 18.dp else 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LanguageFlag(name, 22.dp)
        Spacer(Modifier.width(if (roomy) 12.dp else 8.dp))
        Text(tr(name), style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
    }
}
