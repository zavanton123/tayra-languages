package com.idiomiko.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.idiomiko.core.data.mdict.DictionaryFiles
import com.idiomiko.core.domain.dictionary.DictionaryPack
import com.idiomiko.core.domain.dictionary.DictionaryPacks
import com.idiomiko.core.domain.dictionary.ImportedDictionaryInfo
import com.idiomiko.core.domain.dictionary.PackState
import com.idiomiko.core.domain.dictionary.PackStatus
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.service.DictionaryService
import com.idiomiko.core.ui.components.AppDialog
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppProgressBar
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.Dropdown
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.formatSize
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppSpacing
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Dictionaries of the user's own: importing a file, and the ones imported, to remove. */
class DictionaryImportViewModel(private val dictionaries: DictionaryService) : ViewModel() {
    val canImport: Boolean get() = dictionaries.canImport

    /** The dictionaries imported so far, in the order they came. */
    val imported: StateFlow<List<PackStatus>> = dictionaries.packs
        .map { list -> list.filter { it.pack.id.imported != null } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, dictionaries.packs.value.filter { it.pack.id.imported != null })

    private val _import = MutableStateFlow<DictionaryImport>(DictionaryImport.Idle)

    /** How far importing a dictionary file has got. */
    val import: StateFlow<DictionaryImport> = _import.asStateFlow()

    /** Reads the picked file's index, so the user can say which languages it is for. */
    fun pickImport(file: ByteArray) = viewModelScope.launch {
        _import.value = DictionaryImport.Reading
        _import.value = try {
            DictionaryImport.Ready(dictionaries.inspect(file), file)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DictionaryImport.Failed(e.message ?: "Not a dictionary this app can import")
        }
    }

    fun startImport(sourceLanguage: String, targetLanguage: String) {
        val ready = _import.value as? DictionaryImport.Ready ?: return
        viewModelScope.launch {
            _import.value = DictionaryImport.Importing(ready.info.title, 0f)
            _import.value = try {
                val imported = dictionaries.import(ready.file, sourceLanguage, targetLanguage) { progress ->
                    _import.value = DictionaryImport.Importing(ready.info.title, progress)
                }
                DictionaryImport.Done(imported.title)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DictionaryImport.Failed(e.message ?: "The import failed")
            }
        }
    }

    fun dismissImport() {
        _import.value = DictionaryImport.Idle
    }

    fun remove(pack: DictionaryPack) {
        viewModelScope.launch { dictionaries.remove(pack) }
    }
}

/** Importing a dictionary file, from picking it to its pack being installed. */
sealed interface DictionaryImport {
    data object Idle : DictionaryImport
    data object Reading : DictionaryImport
    class Ready(val info: ImportedDictionaryInfo, val file: ByteArray) : DictionaryImport
    data class Importing(val title: String, val progress: Float) : DictionaryImport
    data class Done(val title: String) : DictionaryImport
    data class Failed(val message: String) : DictionaryImport
}

/** Imports a dictionary of the user's own (Yomitan, MDict or StarDict), whose entries then show with the pack of its language pair. */
@Composable
internal fun ImportDictionaryCard(viewModel: DictionaryImportViewModel, learningCode: String?, native: String, modifier: Modifier = Modifier) {
    val state by viewModel.import.collectAsStateWithLifecycle()
    val imported by viewModel.imported.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf<Confirmation?>(null) }
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
        modifier = modifier,
        // Dense on a phone, as the engine card above it is.
        dense = LocalWindowWidth.current.isCompact,
    ) {
        Text(
            tr("Add a dictionary of your own; its entries show beside the offline dictionary of its languages."),
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
        if (imported.isNotEmpty()) Spacer(Modifier.height(AppSpacing.m))
        imported.forEach { status ->
            AppDivider()
            DownloadRow(
                status.pack.title,
                "${tr(DictionaryPacks.languageName(status.pack.id.sourceLanguage))} \u2192 ${tr(DictionaryPacks.languageName(status.pack.id.targetLanguage))}",
                RowState.Installed((status.state as? PackState.Installed)?.sizeBytes),
                "imported-${status.pack.id.imported}",
                onDownload = null,
                onRemove = {
                    confirming = Confirmation(
                        tr("Remove the {0} dictionary?", status.pack.title),
                        tr("Its entries no longer show when you look words up. You can import it again at any time."),
                    ) { viewModel.remove(status.pack) }
                },
            )
        }
    }
    confirming?.let { c -> RemovePackDialog(c.question, c.consequence, onRemove = c.remove, onDismiss = { confirming = null }) }
    (state as? DictionaryImport.Ready)?.let { ready ->
        ImportLanguagesDialog(
            ready,
            defaultSource = ready.info.sourceLanguage ?: learningCode ?: "en",
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
                // The revision and licence on one line, under the title they belong to.
                val about = listOfNotNull(ready.info.revision.takeIf { it.isNotEmpty() }?.let { tr("Revision {0}", it) }, ready.info.attribution).joinToString(" · ")
                if (about.isNotEmpty()) Text(about, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Dropdown(options, source, { source = it }, tr("Words of"), { tr(it.name) }, Modifier.fillMaxWidth().testTag("import-source"))
                Dropdown(options, target, { target = it }, tr("Explained in"), { tr(it.name) }, Modifier.fillMaxWidth().testTag("import-target"))
            }
        },
        confirmButton = {
            PrimaryButton(tr("Import · {0}", formatSize(ready.file.size.toLong())), onClick = { onImport(source!!.code, target!!.code) }, modifier = Modifier.testTag("import-confirm"), enabled = source != null && target != null)
        },
        dismissButton = { QuietButton(tr("Cancel"), onClick = onDismiss) },
    )
}
