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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tayra.languages.core.ui.i18n.trPlural

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

private val badgeTints = listOf(
    Color(0xFF3B6FE0), Color(0xFF1FA463), Color(0xFF7C4DDB), Color(0xFFDC4A4A), Color(0xFFC98A05), Color(0xFF0E8FA3), Color(0xFFD9488B),
)

/** A round badge with the language's two-letter code, tinted per language. */
@Composable
fun LanguageCodeBadge(languageName: String, size: Dp = 28.dp) {
    val code = LanguageCodes.codeFor(languageName)?.uppercase() ?: languageName.take(2).uppercase()
    val tint = badgeTints[(code.hashCode() and Int.MAX_VALUE) % badgeTints.size]
    Box(Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Text(code, color = tint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

/**
 * The global choice of the language being learned, for the top bar: the name with its flag on
 * wide screens, just the flag on [compact] ones. Shows nothing until languages exist.
 */
@Composable
fun LearningLanguageSelector(compact: Boolean, modifier: Modifier = Modifier) {
    val state = LocalLearningLanguage.current ?: return
    val name = state.currentName ?: return
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    // "Portuguese (432)": the words known in the language, as the Word frequency page counts them.
    val label = state.knownWords?.let { "${tr(name)} (${formatCount(it)})" } ?: tr(name)
    Box(modifier) {
        Row(
            Modifier.clip(RoundedCornerShape(12.dp))
                .background(colors.primary.copy(alpha = 0.06f))
                .border(1.dp, colors.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .clickable { open = true }
                .semantics { contentDescription = tr("Learning language: {0}", label) }
                .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 10.dp),
        ) {
            LanguageFlag(name, if (compact) 18.dp else 22.dp)
            if (!compact) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        if (open) LanguageDialog(state, onDismiss = { open = false })
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
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(16.dp).widthIn(max = 1160.dp).fillMaxHeight(0.92f), shape = RoundedCornerShape(24.dp), color = colors.surface) {
            Column(Modifier.padding(horizontal = if (width.isCompact) 20.dp else 32.dp, vertical = 24.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Choose a language"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(tr("Switch the language you're learning"), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.clip(CircleShape).background(colors.onSurface.copy(alpha = 0.06f))) {
                        Icon(Icons.Default.Close, contentDescription = tr("Close"), tint = colors.onSurfaceVariant)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 24.dp).height(50.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier.weight(1f).focusRequester(focus).testTag("language-search"),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) Text(tr("Search languages"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                                inner()
                            }
                        },
                    )
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = tr("Clear search"),
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(24.dp).clip(CircleShape).clickable { query = "" }.padding(4.dp),
                        )
                    }
                }
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.primary.copy(alpha = 0.07f)).border(1.5.dp, colors.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp).testTag("current-language"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LanguageFlag(name, 30.dp)
        Column(Modifier.weight(1f)) {
            Text(tr(name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (knownWords != null) Text(trPlural(knownWords, "{0} word", "{0} words", formatCount(knownWords)), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        Icon(Icons.Default.Check, contentDescription = tr("Selected"), tint = colors.primary, modifier = Modifier.size(24.dp))
    }
}

/** A language to choose: its flag and name. The native one is dimmed and says why it cannot be chosen. */
@Composable
private fun LanguageTile(name: String, native: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(50.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(enabled = !native, onClick = onClick).alpha(if (native) 0.45f else 1f).padding(horizontal = 16.dp).testTag("language-${LanguageCodes.codeFor(name) ?: name}"),
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
        Modifier.alpha(if (unavailableNote != null) 0.45f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LanguageFlag(flagName, 18.dp)
        Text(label, Modifier.weight(1f, fill = false), fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal)
        Spacer(Modifier.weight(1f))
        if (unavailableNote != null) Text(unavailableNote, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
        if (chosen) Icon(Icons.Default.Check, contentDescription = tr("Selected"), tint = colors.primary, modifier = Modifier.size(18.dp))
    }
}
