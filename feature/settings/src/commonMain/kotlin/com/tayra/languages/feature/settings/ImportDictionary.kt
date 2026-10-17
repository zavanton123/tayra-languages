package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.language.LanguageOption
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.i18n.tr
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/** Imports a dictionary file (a Yomitan .zip or an MDict .mdx), whose entries then show with the pack of its language pair. */
@Composable
internal fun ImportDictionaryCard(viewModel: DictionariesViewModel, learning: LanguageOption?, native: String) {
    val state by viewModel.import.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val picker = rememberFilePickerLauncher(type = FileKitType.File(listOf("zip", "mdx"))) { file ->
        if (file != null) scope.launch { viewModel.pickImport(file.readBytes()) }
    }
    ContentCard(
        tr("Import a dictionary"),
        tr("Add a dictionary of your own, made for Yomitan (.zip) or MDict (.mdx); its entries show beside the offline dictionary of its languages."),
        icon = AppIcons.Download,
    ) {
        when (val current = state) {
            is DictionaryImport.Importing -> {
                Text(tr("Importing {0}...", current.title), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { current.progress }, modifier = Modifier.fillMaxWidth())
            }
            DictionaryImport.Reading -> {
                Text(tr("Reading the dictionary..."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            else -> {
                if (current is DictionaryImport.Done) Text(tr("{0} was imported.", current.title), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                if (current is DictionaryImport.Failed) Text(tr("The import failed: {0}", current.message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { picker.launch() }, shape = RoundedCornerShape(10.dp), modifier = Modifier.testTag("import-dictionary")) {
                    Icon(AppIcons.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(tr("Choose a dictionary file"))
                }
            }
        }
    }
    (state as? DictionaryImport.Ready)?.let { ready ->
        ImportLanguagesDialog(
            ready,
            defaultSource = ready.info.sourceLanguage ?: learning?.code ?: "en",
            defaultTarget = ready.info.targetLanguage ?: native,
            onImport = viewModel::startImport,
            onDismiss = viewModel::dismissImport,
        )
    }
}

/** The dictionary's title and revision, and which language's words it explains in which language. */
@Composable
internal fun ImportLanguagesDialog(ready: DictionaryImport.Ready, defaultSource: String, defaultTarget: String, onImport: (String, String) -> Unit, onDismiss: () -> Unit) {
    val options = LanguageCodes.options
    var source by remember { mutableStateOf(options.firstOrNull { it.code == defaultSource }) }
    var target by remember { mutableStateOf(options.firstOrNull { it.code == defaultTarget }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ready.info.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (ready.info.revision.isNotEmpty()) Text(tr("Revision {0}", ready.info.revision), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ready.info.attribution?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Dropdown(options, source, { source = it }, tr("Words of"), { tr(it.name) }, Modifier.fillMaxWidth().testTag("import-source"))
                Dropdown(options, target, { target = it }, tr("Explained in"), { tr(it.name) }, Modifier.fillMaxWidth().testTag("import-target"))
            }
        },
        confirmButton = {
            Button(onClick = { onImport(source!!.code, target!!.code) }, enabled = source != null && target != null, modifier = Modifier.testTag("import-confirm")) { Text(tr("Import")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}
