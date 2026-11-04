package com.tayra.languages.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.stats.SavedWord
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/** The words saved today, the latest first; choosing one opens it. */
@Composable
internal fun SavedTodayDialog(words: List<SavedWord>, onOpen: (termId: Long) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("saved-today"),
        title = { Text(tr("Words saved today")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                if (words.isEmpty()) {
                    Text(tr("No words saved yet today."), color = colors.onSurfaceVariant)
                    return@Column
                }
                ScrollColumn(Modifier.heightIn(max = LIST_HEIGHT)) {
                    words.forEach { word ->
                        Column(
                            Modifier.fillMaxWidth().clip(AppShapes.control).clickable { onOpen(word.termId) }
                                .padding(horizontal = AppSpacing.s, vertical = AppSpacing.s).testTag("saved-word-${word.termId}"),
                        ) {
                            Text(word.text, style = AppText.itemTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            word.translation?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { QuietButton(tr("Close"), onClick = onDismiss, modifier = Modifier.testTag("saved-today-close")) },
    )
}

private val LIST_HEIGHT = 360.dp
