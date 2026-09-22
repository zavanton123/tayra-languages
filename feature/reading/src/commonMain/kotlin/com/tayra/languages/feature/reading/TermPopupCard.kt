package com.tayra.languages.feature.reading

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.tayra.languages.core.domain.service.TermPopup

/** Hover/tap information for a term. */
@Composable
fun TermPopupCard(popup: TermPopup, modifier: Modifier = Modifier) {
    Surface(modifier.widthIn(max = 360.dp), tonalElevation = 4.dp, shadowElevation = 6.dp, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(12.dp)) {
            PopupEntry(popup, isMain = true)
            popup.images.forEach { (source, _) ->
                AsyncImage(model = source, contentDescription = null, modifier = Modifier.size(120.dp).padding(top = 6.dp))
            }
            if (popup.parents.isNotEmpty()) {
                Column(Modifier.padding(top = 10.dp)) { popup.parents.forEach { PopupEntry(it, isMain = false) } }
            }
            if (popup.components.isNotEmpty()) {
                Text("Components", style = MaterialTheme.typography.labelMedium, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 10.dp))
                popup.components.forEach { PopupEntry(it, isMain = false) }
            }
        }
    }
}

@Composable
private fun PopupEntry(entry: TermPopup, isMain: Boolean) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                if (isMain) entry.termAndParentsText else entry.termText,
                style = if (isMain) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (!isMain && entry.romanization.isNotEmpty()) Text(" (${entry.romanization})", fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodySmall)
        }
        if (entry.tags.isNotEmpty()) {
            Row { entry.tags.forEach { SuggestionChip(onClick = {}, label = { Text(it, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.padding(end = 4.dp)) } }
        }
        if (entry.flashMessage.isNotEmpty()) Text(entry.flashMessage, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
        if (isMain && entry.romanization.isNotEmpty()) Text(entry.romanization, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyMedium)
        if (entry.translation.isNotEmpty()) Text(entry.translation, style = MaterialTheme.typography.bodyMedium)
    }
}
