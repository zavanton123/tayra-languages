package com.tayra.languages.feature.terms.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tayra.languages.core.domain.practice.ExerciseKind
import com.tayra.languages.core.domain.practice.PracticeBuilder
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.description
import com.tayra.languages.core.ui.components.icon
import com.tayra.languages.core.ui.components.label
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural

/** About how long a question takes, for the estimate in the dialog. */
private const val SECONDS_A_QUESTION = 20

/** Asks which kinds of question to practise the [count] chosen words with; any of them can be ticked, all are to begin with. */
@Composable
fun PracticeKindsDialog(count: Int, onStart: (Set<ExerciseKind>) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    var kinds by remember { mutableStateOf(PracticeBuilder.KINDS.toSet()) }
    val minutes = ((PracticeBuilder.questionCount(count, kinds.size) * SECONDS_A_QUESTION + 59) / 60).coerceAtLeast(1)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(16.dp).widthIn(max = 760.dp), shape = RoundedCornerShape(24.dp), color = colors.surfaceContainerLow) {
            Column {
                Column(Modifier.padding(start = 32.dp, end = 20.dp, top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(colors.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Icon(AppIcons.BarChart, contentDescription = null, tint = colors.primary, modifier = Modifier.size(28.dp))
                        }
                        Text(trPlural(count, "Practise {0} word", "Practise {0} words"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (!compact) {
                            Text(
                                tr("{0} words · ~{1} min", count, minutes),
                                Modifier.clip(RoundedCornerShape(50)).background(colors.onSurface.copy(alpha = 0.06f)).padding(horizontal = 14.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = tr("Close"), tint = colors.onSurfaceVariant) }
                    }
                    Text(tr("Choose one or more question types."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                    val card: @Composable (ExerciseKind, Modifier) -> Unit = { kind, modifier ->
                        val ticked = kind in kinds
                        val toggle = { kinds = if (ticked) kinds - kind else kinds + kind }
                        Row(
                            modifier.heightIn(min = 112.dp).clip(RoundedCornerShape(14.dp))
                                .background(if (ticked) colors.primary.copy(alpha = 0.07f) else colors.surface)
                                .border(if (ticked) 1.5.dp else 1.dp, if (ticked) colors.primary else colors.outlineVariant, RoundedCornerShape(14.dp))
                                .clickable(onClick = toggle).padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp).testTag("practice-kind-${kind.name}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(checked = ticked, onCheckedChange = { toggle() })
                            Icon(kind.icon(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(kind.label(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(kind.description(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                    Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PracticeBuilder.KINDS.chunked(if (compact) 1 else 2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { kind -> card(kind, Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                HorizontalDivider(color = colors.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(trPlural(kinds.size, "{0} question type selected", "{0} question types selected"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(tr("Cancel"), style = MaterialTheme.typography.titleMedium) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onStart(kinds) }, enabled = kinds.isNotEmpty(), shape = RoundedCornerShape(50), modifier = Modifier.height(52.dp).testTag("start-practice")) {
                        Text(tr("Start practice"), Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
