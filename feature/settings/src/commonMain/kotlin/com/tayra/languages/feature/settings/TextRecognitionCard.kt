package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.tr

/**
 * The text recognition models downloaded for reading photos of pages, which can be removed to free
 * the space; shown once there is one.
 */
@Composable
internal fun TextRecognitionCard(viewModel: DictionariesViewModel) {
    val packs by viewModel.ocrPacks.collectAsStateWithLifecycle()
    if (packs.isEmpty()) return
    ContentCard(
        tr("Text recognition"),
        tr("Models for reading photos of pages, downloaded when a photo in their language was first imported."),
        icon = AppIcons.UploadFile,
    ) {
        packs.forEachIndexed { index, pack ->
            if (index > 0) HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr(pack.title), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(formatSize(pack.sizeBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                QuietButton(tr("Remove"), onClick = { viewModel.removeOcr(pack) })
            }
        }
    }
}
