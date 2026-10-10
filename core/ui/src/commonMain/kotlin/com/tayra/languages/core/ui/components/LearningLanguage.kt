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
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            state.languages.forEach { (id, language) ->
                val available = id == state.currentId || id !in state.nativeIds
                AppMenuItem(
                    text = {
                        LanguageMenuRow(language, tr(language), chosen = id == state.currentId, unavailableNote = tr("Your native language").takeUnless { available })
                    },
                    onClick = { open = false; if (id != state.currentId) state.onSelect(id) },
                    enabled = available,
                )
            }
        }
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
