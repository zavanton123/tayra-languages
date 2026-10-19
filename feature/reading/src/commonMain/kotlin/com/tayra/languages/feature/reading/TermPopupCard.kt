package com.tayra.languages.feature.reading

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.service.TermPopup
import com.tayra.languages.core.ui.components.PopupCard
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/** Hover/tap information for a term. */
@Composable
fun TermPopupCard(popup: TermPopup, modifier: Modifier = Modifier) {
    PopupCard(modifier.widthIn(max = POPUP_WIDTH)) {
        Column(Modifier.padding(AppSpacing.m)) {
            PopupEntry(popup, isMain = true)
            if (popup.parents.isNotEmpty()) {
                Column(Modifier.padding(top = AppSpacing.m)) { popup.parents.forEach { PopupEntry(it, isMain = false) } }
            }
            if (popup.components.isNotEmpty()) {
                Text(tr("Components"), style = MaterialTheme.typography.labelMedium, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = AppSpacing.m))
                popup.components.forEach { PopupEntry(it, isMain = false) }
            }
        }
    }
}

@Composable
private fun PopupEntry(entry: TermPopup, isMain: Boolean) {
    Column(Modifier.padding(vertical = AppSpacing.xxs)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                if (isMain) entry.termAndParentsText else entry.termText,
                style = if (isMain) AppText.itemTitle else AppText.smallTitle,
            )
            if (!isMain && entry.romanization.isNotEmpty()) Text(" (${entry.romanization})", fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodySmall)
        }
        if (entry.flashMessage.isNotEmpty()) Text(entry.flashMessage, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
        if (isMain && entry.romanization.isNotEmpty()) Text(entry.romanization, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyMedium)
        if (entry.translation.isNotEmpty()) Text(entry.translation, style = MaterialTheme.typography.bodyMedium)
    }
}

private val POPUP_WIDTH = 360.dp
