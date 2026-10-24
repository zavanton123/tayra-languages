package com.tayra.languages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/**
 * What a list is narrowed to, in plain words ("Learning 1–4 · 12 terms"), on a tinted pill that
 * reopens the filters when [onEdit] is given, with [clearLabel] beside it to show everything again.
 * A list that only shows part of itself says so here, so it never looks as if the rest were gone.
 */
@Composable
fun ActiveFilterSummary(summary: String, clearLabel: String, onClear: () -> Unit, modifier: Modifier = Modifier, onEdit: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Row(
            Modifier.weight(1f, fill = false).heightIn(min = AppSizes.touch).clip(AppShapes.control)
                .background(colors.primary.copy(alpha = AppAlpha.tint))
                .then(if (onEdit != null) Modifier.clickable(onClickLabel = tr("Filters"), onClick = onEdit) else Modifier)
                .padding(horizontal = AppSpacing.l, vertical = AppSpacing.s)
                .testTag("active-filter-summary"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        ) {
            Icon(AppIcons.Tune, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconCompact))
            Text(summary, style = AppText.label, color = colors.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        QuietButton(clearLabel, onClick = onClear, modifier = Modifier.testTag("clear-filters"))
    }
}
