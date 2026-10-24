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
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.practice.PracticeSetup
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
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.core.ui.components.PracticeQuestion

/** About how long a question takes, for the estimate in the dialog. */
private const val SECONDS_A_QUESTION = 20
private val KIND_CARD_MIN_HEIGHT = 112.dp

/** Asks which kinds of question to practise the [count] chosen words with; any of them can be ticked, all are to begin with. */
@Composable
fun PracticeKindsDialog(count: Int, onStart: (PracticeSetup) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    var kinds by remember { mutableStateOf(PracticeQuestion.all.toSet()) }
    val setup = PracticeSetup(types = kinds.map { it.type }.toSet(), responses = kinds.map { it.response }.toSet())
    val minutes = ((PracticeBuilder.questionCount(count, setup.rotation.size) * SECONDS_A_QUESTION + 59) / 60).coerceAtLeast(1)
    AppDialogSurface(onDismissRequest = onDismiss, color = colors.surfaceContainerLow) {
        Column {
            Column(Modifier.padding(start = AppSpacing.xxxl, end = AppSpacing.xl, top = AppSpacing.xxl, bottom = AppSpacing.xxl), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                    IconTile(AppIcons.DoneAll, size = 56)
                    Text(trPlural(count, "Practise {0} word", "Practise {0} words"), style = AppText.headline)
                    if (!compact) {
                        // The title gives the count already; the tag adds only the time.
                        Tag(tr("~{0} min", minutes), style = TagStyle.Neutral)
                    }
                    Spacer(Modifier.weight(1f))
                    AppIconButton(Icons.Default.Close, tr("Close"), onDismiss)
                }
                Text(tr("Choose one or more question types."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                val card: @Composable (PracticeQuestion, Modifier) -> Unit = { kind, modifier ->
                    val ticked = kind in kinds
                    val toggle = { kinds = if (ticked) kinds - kind else kinds + kind }
                    Row(
                        modifier.heightIn(min = KIND_CARD_MIN_HEIGHT).clip(AppShapes.card)
                            .background(if (ticked) colors.primary.copy(alpha = AppAlpha.wash) else colors.surface)
                            .border(if (ticked) AppBorders.selected() else AppBorders.hairline(), AppShapes.card)
                            .clickable(onClick = toggle).padding(start = AppSpacing.m, end = AppSpacing.l, top = AppSpacing.m, bottom = AppSpacing.m).testTag("practice-kind-${kind.name}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
                    ) {
                        AppCheckbox(checked = ticked, onCheckedChange = { toggle() })
                        Icon(kind.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconXLarge))
                        Spacer(Modifier.width(AppSpacing.xs))
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            Text(kind.label, style = AppText.itemTitle)
                            Text(kind.description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                }
                Column(Modifier.padding(end = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                    PracticeQuestion.all.chunked(if (compact) 1 else 2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                            row.forEach { kind -> card(kind, Modifier.weight(1f)) }
                        }
                    }
                }
            }
            AppDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.xxxl, vertical = AppSpacing.l), verticalAlignment = Alignment.CenterVertically) {
                // The count takes what the buttons leave, wrapping in a narrow window, so the buttons keep their size.
                Text(trPlural(kinds.size, "{0} question type selected", "{0} question types selected"), Modifier.weight(1f).padding(end = AppSpacing.m), style = AppText.emphasis)
                QuietButton(tr("Cancel"), onClick = onDismiss, size = ButtonSize.Large)
                Spacer(Modifier.width(AppSpacing.s))
                PrimaryButton(tr("Start practice"), onClick = { onStart(setup) }, modifier = Modifier.testTag("start-practice"), enabled = kinds.isNotEmpty(), size = ButtonSize.Large)
            }
        }
    }
}
