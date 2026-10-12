package com.tayra.languages.feature.terms.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import com.tayra.languages.core.domain.practice.ExerciseKind
import com.tayra.languages.core.domain.practice.PracticeBuilder
import com.tayra.languages.core.ui.components.description
import com.tayra.languages.core.ui.components.label
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural

/** Asks which kinds of question to practise the [count] chosen words with; any of them can be ticked, all are to begin with. */
@Composable
internal fun PracticeKindsDialog(count: Int, onStart: (Set<ExerciseKind>) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var kinds by remember { mutableStateOf(PracticeBuilder.KINDS.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(trPlural(count, "Practise {0} word", "Practise {0} words")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Kinds of question"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                PracticeBuilder.KINDS.forEach { kind ->
                    val ticked = kind in kinds
                    val toggle = { kinds = if (ticked) kinds - kind else kinds + kind }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (ticked) colors.primary.copy(alpha = 0.08f) else colors.surface)
                            .border(1.dp, if (ticked) colors.primary.copy(alpha = 0.5f) else colors.outlineVariant, RoundedCornerShape(12.dp))
                            .clickable(onClick = toggle).padding(end = 12.dp).testTag("practice-kind-${kind.name}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = ticked, onCheckedChange = { toggle() })
                        Column(Modifier.padding(vertical = 10.dp)) {
                            Text(kind.label(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(kind.description(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onStart(kinds) }, enabled = kinds.isNotEmpty(), modifier = Modifier.testTag("start-practice")) { Text(tr("Start")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}
