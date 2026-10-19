package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.data.mdict.DictionaryFiles
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.language.LanguageOption
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppSpacing
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

/** Imports a dictionary of the user's own (Yomitan, MDict or StarDict), whose entries then show with the pack of its language pair. */
@Composable
internal fun ImportDictionaryCard(viewModel: DictionariesViewModel, learning: LanguageOption?, native: String) {
    val state by viewModel.import.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Several files at once for a StarDict dictionary given as its .ifo, .idx and .dict.
    val picker = rememberFilePickerLauncher(type = FileKitType.File(DictionaryFiles.pickerExtensions), mode = FileKitMode.Multiple()) { files ->
        if (!files.isNullOrEmpty()) scope.launch { viewModel.pickImport(DictionaryFiles.bundle(files.map { it.name to it.readBytes() })) }
    }
    ContentCard(
        tr("Import a dictionary"),
        // One line under the title, as on every card; the file details follow in small print.
        tr("Yomitan, MDict or StarDict files"),
        icon = AppIcons.Download,
    ) {
        Text(
            tr("Add a dictionary of your own, made for Yomitan (.zip), MDict (.mdx) or StarDict (its .ifo, .idx and .dict files, or an archive of them); its entries show beside the offline dictionary of its languages."),
            Modifier.padding(bottom = AppSpacing.m),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (val current = state) {
            is DictionaryImport.Importing -> {
                Text(tr("Importing {0}...", current.title), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppProgressBar(current.progress)
            }
            DictionaryImport.Reading -> {
                Text(tr("Reading the dictionary..."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppProgressBar(null)
            }
            else -> {
                if (current is DictionaryImport.Done) Text(tr("{0} was imported.", current.title), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                if (current is DictionaryImport.Failed) Text(tr("The import failed: {0}", current.message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                SecondaryButton(tr("Choose a dictionary file"), onClick = { picker.launch() }, modifier = Modifier.testTag("import-dictionary"), icon = AppIcons.UploadFile)
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
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(ready.info.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                if (ready.info.revision.isNotEmpty()) Text(tr("Revision {0}", ready.info.revision), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ready.info.attribution?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Dropdown(options, source, { source = it }, tr("Words of"), { tr(it.name) }, Modifier.fillMaxWidth().testTag("import-source"))
                Dropdown(options, target, { target = it }, tr("Explained in"), { tr(it.name) }, Modifier.fillMaxWidth().testTag("import-target"))
            }
        },
        confirmButton = {
            PrimaryButton(tr("Import"), onClick = { onImport(source!!.code, target!!.code) }, modifier = Modifier.testTag("import-confirm"), enabled = source != null && target != null)
        },
        dismissButton = { QuietButton(tr("Cancel"), onClick = onDismiss) },
    )
}
