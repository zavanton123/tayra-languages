package com.tayra.languages.feature.terms.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.practice.ExerciseKind
import com.tayra.languages.core.domain.practice.PracticeBuilder
import com.tayra.languages.core.ui.components.AppCheckbox
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.TagStyle
import com.tayra.languages.core.ui.components.description
import com.tayra.languages.core.ui.components.icon
import com.tayra.languages.core.ui.components.label
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes

/** About how long a question takes, for the estimate in the dialog. */
private const val SECONDS_A_QUESTION = 20

/** Asks which kinds of question to practise the [count] chosen words with; any of them can be ticked, all are to begin with. */
@Composable
fun PracticeKindsDialog(count: Int, onStart: (Set<ExerciseKind>) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    var kinds by remember { mutableStateOf(PracticeBuilder.KINDS.toSet()) }
    val minutes = ((PracticeBuilder.questionCount(count, kinds.size) * SECONDS_A_QUESTION + 59) / 60).coerceAtLeast(1)
    AppDialogSurface(onDismissRequest = onDismiss, color = colors.surfaceContainerLow) {
        Column {
            Column(Modifier.padding(start = 32.dp, end = 20.dp, top = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconTile(AppIcons.BarChart, size = 56)
                    Text(trPlural(count, "Practise {0} word", "Practise {0} words"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (!compact) {
                        Tag(tr("{0} words · ~{1} min", count, minutes), style = TagStyle.Neutral)
                    }
                    Spacer(Modifier.weight(1f))
                    AppIconButton(Icons.Default.Close, tr("Close"), onDismiss)
                }
                Text(tr("Choose one or more question types."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                val card: @Composable (ExerciseKind, Modifier) -> Unit = { kind, modifier ->
                    val ticked = kind in kinds
                    val toggle = { kinds = if (ticked) kinds - kind else kinds + kind }
                    Row(
                        modifier.heightIn(min = 112.dp).clip(AppShapes.card)
                            .background(if (ticked) colors.primary.copy(alpha = AppAlpha.wash) else colors.surface)
                            .border(if (ticked) AppBorders.selected() else AppBorders.hairline(), AppShapes.card)
                            .clickable(onClick = toggle).padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp).testTag("practice-kind-${kind.name}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppCheckbox(checked = ticked, onCheckedChange = { toggle() })
                        Icon(kind.icon(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconXLarge))
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
            AppDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(trPlural(kinds.size, "{0} question type selected", "{0} question types selected"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                QuietButton(tr("Cancel"), onClick = onDismiss, size = ButtonSize.Large)
                Spacer(Modifier.width(8.dp))
                PrimaryButton(tr("Start practice"), onClick = { onStart(kinds) }, modifier = Modifier.testTag("start-practice"), enabled = kinds.isNotEmpty(), size = ButtonSize.Large)
            }
        }
    }
}
