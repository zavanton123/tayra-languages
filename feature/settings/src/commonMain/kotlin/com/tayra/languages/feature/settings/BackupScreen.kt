package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.backup.Backup
import com.tayra.languages.core.domain.backup.BackupException
import com.tayra.languages.core.domain.backup.BackupRepository
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.files.saveBinaryFile
import com.tayra.languages.core.ui.navigation.Route
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.koin.compose.viewmodel.koinViewModel

data class BackupState(
    val backups: List<Backup> = emptyList(),
    val loaded: Boolean = false,
    /** What is being done, such as "Creating a backup", or null when idle. */
    val working: String? = null,
    val message: String? = null,
    val error: String? = null,
    /** Set once a restore finished, with the backup made of the data it replaced. */
    val restored: Backup? = null,
)

class BackupViewModel(private val backups: BackupRepository) : ViewModel() {
    private val _state = MutableStateFlow(BackupState())
    val state: StateFlow<BackupState> = _state.asStateFlow()

    init {
        viewModelScope.launch { refresh() }
    }

    fun create() = run("Creating a backup") {
        val backup = backups.create()
        "Backup of ${formatBackupTime(backup)} created"
    }

    fun delete(backup: Backup) = run("Deleting the backup") {
        backups.delete(backup.name)
        null
    }

    fun export(backup: Backup) = run("Exporting the backup") {
        val bytes = backups.read(backup.name)
        if (saveBinaryFile(backup.name.removeSuffix(".sqlite"), "sqlite", bytes)) "Backup exported" else null
    }

    fun import(bytes: ByteArray) = run("Importing the backup") {
        val backup = backups.import(bytes)
        "Backup of ${formatBackupTime(backup)} added to the list"
    }

    fun restore(backup: Backup) = run("Restoring the backup") {
        val undo = backups.restore(backup.name)
        _state.update { it.copy(restored = undo) }
        null
    }

    fun restoreSeen() = _state.update { it.copy(restored = null) }

    private fun run(working: String, action: suspend () -> String?) {
        if (_state.value.working != null) return
        _state.update { it.copy(working = working, message = null, error = null) }
        viewModelScope.launch {
            try {
                val message = action()
                _state.update { it.copy(message = message) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupException) {
                _state.update { it.copy(error = e.message) }
            } catch (e: Exception) {
                Logger.w(e) { "$working failed" }
                _state.update { it.copy(error = "$working failed: ${e.message ?: e::class.simpleName}") }
            } finally {
                _state.update { it.copy(working = null) }
                refresh()
            }
        }
    }

    private suspend fun refresh() {
        val list = try {
            backups.list()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Could not list the backups" }
            emptyList()
        }
        _state.update { it.copy(backups = list, loaded = true) }
    }
}

/** The backups kept by the app, with buttons to make, import, restore, export and delete them. */
@Composable
fun BackupScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, onRestored: () -> Unit, viewModel: BackupViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var restoring by remember { mutableStateOf<Backup?>(null) }
    var deleting by remember { mutableStateOf<Backup?>(null) }
    val picker = rememberFilePickerLauncher(type = FileKitType.File()) { file ->
        if (file != null) scope.launch { viewModel.import(file.readBytes()) }
    }
    val idle = state.working == null

    Scaffold(topBar = { AppTopBar(title = "Backups", onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 800.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "A backup is one file with your languages, books, vocabulary, reading history and settings. " +
                    "Audio files, downloaded dictionaries, voices and translation models are not included, nor are API keys.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = viewModel::create, enabled = idle) { Text("Create backup") }
                OutlinedButton(onClick = { picker.launch() }, enabled = idle) { Text("Import file") }
            }
            state.working?.let { working ->
                Text("$working…", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
            state.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }

            if (state.loaded && state.backups.isEmpty()) {
                Text("No backups yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.backups.forEachIndexed { i, backup ->
                if (i > 0) HorizontalDivider()
                BackupRow(
                    backup,
                    enabled = idle,
                    onRestore = { restoring = backup },
                    onExport = { viewModel.export(backup) },
                    onDelete = { deleting = backup },
                )
            }
        }
    }

    restoring?.let { backup ->
        AlertDialog(
            onDismissRequest = { restoring = null },
            title = { Text("Restore this backup?") },
            text = {
                Text(
                    "All languages, books, vocabulary, reading history and settings will be replaced by those of the backup of ${formatBackupTime(backup)}. " +
                        "Your current data is backed up first, so you can go back to it.",
                )
            },
            confirmButton = { Button(onClick = { restoring = null; viewModel.restore(backup) }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = { restoring = null }) { Text("Cancel") } },
        )
    }
    deleting?.let { backup ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this backup?") },
            text = { Text("The backup of ${formatBackupTime(backup)} will be deleted from this device. Exported copies are kept.") },
            confirmButton = { Button(onClick = { deleting = null; viewModel.delete(backup) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
    state.restored?.let { undo ->
        val done = { viewModel.restoreSeen(); onRestored() }
        AlertDialog(
            onDismissRequest = done,
            title = { Text("Backup restored") },
            text = { Text("The data you had before is kept as the backup of ${formatBackupTime(undo)}.") },
            confirmButton = { Button(onClick = done) { Text("OK") } },
        )
    }
}

@Composable
private fun BackupRow(backup: Backup, enabled: Boolean, onRestore: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit) {
    val details: @Composable () -> Unit = {
        Column {
            Text(formatBackupTime(backup), style = MaterialTheme.typography.bodyLarge)
            Text(formatSize(backup.sizeBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    val actions: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onRestore, enabled = enabled) { Text("Restore") }
            TextButton(onClick = onExport, enabled = enabled) { Text("Export") }
            IconButton(onClick = onDelete, enabled = enabled) { Icon(Icons.Default.Delete, contentDescription = "Delete backup") }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 480.dp) {
            Column { details(); actions() }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { details() }
                actions()
            }
        }
    }
}

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

internal fun formatBackupTime(backup: Backup): String = backup.createdAt?.let(::formatBackupTime) ?: backup.name

private fun formatBackupTime(time: LocalDateTime): String {
    fun two(n: Int) = n.toString().padStart(2, '0')
    return "${time.day} ${MONTHS[time.month.ordinal]} ${time.year}, ${two(time.hour)}:${two(time.minute)}:${two(time.second)}"
}
