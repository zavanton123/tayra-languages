package com.tayra.languages.core.ui.components

import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.language.LanguageCodes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Scaffold

/**
 * The language being learned and the ones to choose from; [languages] are ids with their English names, shown through `tr`.
 * [nativeIds] are listed but cannot be chosen, being the native language. [knownWords] is how many
 * words the reader knows in the current language, shown after its name; null leaves it out.
 */
class LearningLanguageState(
    val languages: List<Pair<Long, String>>,
    val currentId: Long,
    val nativeIds: Set<Long> = emptySet(),
    val knownWords: Int? = null,
    val onSelect: (Long) -> Unit,
) {
    val currentName: String? get() = languages.firstOrNull { it.first == currentId }?.second
}

/** Provided at the root of the app; without it the top bar shows no language selector. */
val LocalLearningLanguage = staticCompositionLocalOf<LearningLanguageState?> { null }

private val badgeTints = AppAccents.sequence.take(7)

/** A round badge with the language's two-letter code, tinted per language. */
@Composable
fun LanguageCodeBadge(languageName: String, size: Dp = 28.dp) {
    val code = LanguageCodes.codeFor(languageName)?.uppercase() ?: languageName.take(2).uppercase()
    val tint = badgeTints[(code.hashCode() and Int.MAX_VALUE) % badgeTints.size]
    Box(Modifier.size(size).clip(AppShapes.circle).background(tint.copy(alpha = AppAlpha.soft)), contentAlignment = Alignment.Center) {
        Text(code, color = tint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

/**
 * The global choice of the language being learned, for the top bar: the name with its flag on
 * wide screens, just the flag on [compact] ones. It opens the language dialog, or calls [onOpen]
 * where the choice is a screen of its own (phones). Shows nothing until languages exist.
 */
@Composable
fun LearningLanguageSelector(compact: Boolean, modifier: Modifier = Modifier, onOpen: (() -> Unit)? = null) {
    val state = LocalLearningLanguage.current ?: return
    val name = state.currentName ?: return
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    // "Portuguese (432)": the words known in the language, as the Word frequency page counts them.
    val label = state.knownWords?.let { "${tr(name)} (${formatCount(it)})" } ?: tr(name)
    Box(modifier) {
        Row(
            Modifier.clip(AppShapes.panel)
                .background(colors.primary.copy(alpha = AppAlpha.wash))
                .border(AppBorders.hairline(colors.primary.copy(alpha = AppAlpha.soft)), AppShapes.panel)
                .clickable { if (onOpen != null) onOpen() else open = true }
                .semantics { contentDescription = tr("Learning language: {0}", label) }
                .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 10.dp),
        ) {
            LanguageFlag(name, if (compact) 18.dp else 22.dp)
            if (!compact) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.icon))
        }
        if (open) LanguageDialog(state, onDismiss = { open = false })
    }
}

/**
 * The phone's language screen: the current language on its card, then every other one as a row
 * with its flag, with a letter index down the right edge that jumps the list. Choosing one goes
 * back, and the app moves on to that language's courses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseLanguageScreen(onBack: () -> Unit) {
    val state = LocalLearningLanguage.current
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val needle = query.trim().lowercase()
    fun shown(name: String) = needle.isEmpty() || tr(name).lowercase().contains(needle) || name.lowercase().contains(needle)
    val others = state?.languages.orEmpty().filter { (id, name) -> id != state?.currentId && shown(name) }
    val currentName = state?.currentName?.takeIf { shown(it) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // The rows before the languages: search, the current language (when shown) and the heading.
    val rowsBefore = if (currentName != null) 4 else 2
    val letters = others.map { (_, name) -> tr(name).first().uppercaseChar() }.distinct()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tr("Choose a language"), fontWeight = FontWeight.Bold) },
                navigationIcon = { AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back"), onClick = onBack, tint = colors.primary) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.surface),
            )
        },
        containerColor = colors.surface,
    ) { padding ->
        if (state == null) return@Scaffold
        Box(Modifier.padding(padding).fillMaxSize().testTag("choose-language")) {
            LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    SearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = tr("Search languages"),
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                        fieldModifier = Modifier.testTag("language-search"),
                    )
                }
                if (currentName != null) {
                    item { PhoneSectionTitle(tr("Current language")) }
                    item {
                        Row(
                            Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(AppShapes.card).background(colors.primary.copy(alpha = AppAlpha.tint))
                                .padding(horizontal = 14.dp, vertical = 14.dp).testTag("current-language"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            LanguageFlag(currentName, 36.dp, Modifier.clip(AppShapes.small))
                            Column(Modifier.weight(1f)) {
                                Text(tr(currentName), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                state.knownWords?.let { Text(trPlural(it, "{0} word", "{0} words", formatCount(it)), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
                            }
                            Icon(Icons.Default.Check, contentDescription = tr("Selected"), tint = colors.primary, modifier = Modifier.size(AppSizes.iconLarge))
                        }
                    }
                }
                item { PhoneSectionTitle(tr("All languages"), trPlural(others.size, "{0} language", "{0} languages")) }
                if (others.isEmpty()) {
                    item { Text(tr("No languages match"), Modifier.padding(horizontal = 16.dp, vertical = 16.dp), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant) }
                }
                itemsIndexed(others, key = { _, (id, _) -> id }) { index, (id, name) ->
                    val native = id in state.nativeIds
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = !native) { onBack(); state.onSelect(id) }.alpha(if (native) AppAlpha.disabled else 1f)
                            .padding(start = 24.dp, end = 40.dp, top = 12.dp, bottom = 12.dp).testTag("language-${LanguageCodes.codeFor(name) ?: name}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        LanguageFlag(name, 30.dp, Modifier.clip(AppShapes.tiny))
                        Text(tr(name), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (native) Text(tr("Your native language"), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
                    }
                    if (index < others.lastIndex) AppDivider(Modifier.padding(start = 16.dp, end = 32.dp), color = colors.outlineVariant.copy(alpha = AppAlpha.muted))
                }
            }
            if (letters.size > 1) {
                Column(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    letters.forEach { letter ->
                        Text(
                            letter.toString(),
                            Modifier.clip(AppShapes.circle)
                                .clickable(onClickLabel = tr("Jump to {0}", letter.toString())) {
                                    val target = others.indexOfFirst { (_, name) -> tr(name).first().uppercaseChar() == letter }
                                    if (target >= 0) scope.launch { list.animateScrollToItem(rowsBefore + target) }
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("index-$letter"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneSectionTitle(title: String, note: String? = null) {
    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (note != null) Text(note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * The languages to choose from, as a dialog: the current one first with the words known in it,
 * then every other one as a tile in a grid, narrowed by a search box that has the focus from the
 * start. The native language is listed but cannot be chosen.
 */
@Composable
private fun LanguageDialog(state: LearningLanguageState, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val width = LocalWindowWidth.current
    val columns = when (width) {
        WindowWidth.COMPACT -> 1
        WindowWidth.MEDIUM -> 2
        WindowWidth.EXPANDED -> 3
    }
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val needle = query.trim().lowercase()
    val others = state.languages.filter { (id, name) -> id != state.currentId && (needle.isEmpty() || tr(name).lowercase().contains(needle) || name.lowercase().contains(needle)) }
    val currentName = state.currentName
    val currentShown = currentName != null && (needle.isEmpty() || tr(currentName).lowercase().contains(needle) || currentName.lowercase().contains(needle))
    AppDialogSurface(onDismissRequest = onDismiss, modifier = Modifier.fillMaxHeight(0.92f), maxWidth = 1160.dp) {
        Column(Modifier.padding(horizontal = if (width.isCompact) 20.dp else 32.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Choose a language"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(tr("Switch the language you're learning"), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
                AppIconButton(Icons.Default.Close, contentDescription = tr("Close"), onClick = onDismiss, style = IconButtonStyle.Tonal, tint = colors.onSurface)
            }
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = tr("Search languages"),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                fieldModifier = Modifier.focusRequester(focus).testTag("language-search"),
            )
            ScrollColumn(Modifier.fillMaxWidth().weight(1f), contentModifier = Modifier.padding(top = 8.dp, end = 14.dp)) {
                if (currentShown) {
                    SectionTitle(tr("Current language"))
                    CurrentLanguageCard(currentName!!, state.knownWords)
                }
                SectionTitle(tr("All languages"), trPlural(others.size, "{0} language", "{0} languages"))
                if (others.isEmpty()) {
                    Text(tr("No languages match"), Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
                others.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { (id, name) ->
                            LanguageTile(name, native = id in state.nativeIds, Modifier.weight(1f)) { onDismiss(); state.onSelect(id) }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, note: String? = null) {
    Row(Modifier.padding(top = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (note != null) Text(note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The language being learned: its flag, name and the words known in it, marked as chosen. */
@Composable
private fun CurrentLanguageCard(name: String, knownWords: Int?) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.primary.copy(alpha = AppAlpha.wash)).border(AppBorders.selected(colors.primary.copy(alpha = AppAlpha.half)), AppShapes.card)
            .padding(horizontal = 20.dp, vertical = 14.dp).testTag("current-language"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LanguageFlag(name, 30.dp)
        Column(Modifier.weight(1f)) {
            Text(tr(name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (knownWords != null) Text(trPlural(knownWords, "{0} word", "{0} words", formatCount(knownWords)), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        Icon(Icons.Default.Check, contentDescription = tr("Selected"), tint = colors.primary, modifier = Modifier.size(AppSizes.iconLarge))
    }
}

/** A language to choose: its flag and name. The native one is dimmed and says why it cannot be chosen. */
@Composable
private fun LanguageTile(name: String, native: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(50.dp).cardSurface(AppShapes.panel)
            .clickable(enabled = !native, onClick = onClick).alpha(if (native) AppAlpha.disabled else 1f).padding(horizontal = 16.dp).testTag("language-${LanguageCodes.codeFor(name) ?: name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LanguageFlag(name, 22.dp)
        Text(tr(name), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (native) Text(tr("Your native language"), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
    }
}

/**
 * One language in a language menu: its flag, [label] and a check when [chosen]. One that cannot be
 * chosen is dimmed, with [unavailableNote] saying why.
 */
@Composable
fun LanguageMenuRow(flagName: String, label: String, chosen: Boolean, unavailableNote: String? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.alpha(if (unavailableNote != null) AppAlpha.disabled else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LanguageFlag(flagName, 18.dp)
        Text(label, Modifier.weight(1f, fill = false), fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal)
        Spacer(Modifier.weight(1f))
        if (unavailableNote != null) Text(unavailableNote, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
        if (chosen) Icon(Icons.Default.Check, contentDescription = tr("Selected"), tint = colors.primary, modifier = Modifier.size(AppSizes.iconCompact))
    }
}
