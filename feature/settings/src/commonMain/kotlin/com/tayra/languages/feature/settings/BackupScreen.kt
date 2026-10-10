package com.tayra.languages.feature.settings

import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.i18n.tr
import androidx.compose.foundation.background
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.files.acceptsDroppedFiles
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.formatSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.backup.Backup
import com.tayra.languages.core.domain.backup.BackupException
import com.tayra.languages.core.domain.backup.BackupRepository
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LocalWindowWidth
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
    /** Set once a file was imported, until the reader has said whether to restore it now. */
    val imported: Backup? = null,
)

class BackupViewModel(private val backups: BackupRepository) : ViewModel() {
    private val _state = MutableStateFlow(BackupState())
    val state: StateFlow<BackupState> = _state.asStateFlow()

    init {
        viewModelScope.launch { refresh() }
    }

    fun create() = run(tr("Creating a backup"), { tr("Could not create the backup: {0}", it) }) {
        val backup = backups.create()
        tr("Backup of {0} created", formatBackupTime(backup))
    }

    fun delete(backup: Backup) = run(tr("Deleting the backup"), { tr("Could not delete the backup: {0}", it) }) {
        backups.delete(backup.name)
        null
    }

    fun export(backup: Backup) = run(tr("Exporting the backup"), { tr("Could not export the backup: {0}", it) }) {
        val bytes = backups.read(backup.name)
        if (saveBinaryFile(backup.name.removeSuffix(".sqlite"), "sqlite", bytes)) tr("Backup exported") else null
    }

    fun import(bytes: ByteArray) = run(tr("Importing the backup"), { tr("Could not import the backup: {0}", it) }) {
        val backup = backups.import(bytes)
        _state.update { it.copy(imported = backup) }
        tr("Backup of {0} added to the list", formatBackupTime(backup))
    }

    fun importSeen() = _state.update { it.copy(imported = null) }

    fun restore(backup: Backup) = run(tr("Restoring the backup"), { tr("Could not restore the backup: {0}", it) }) {
        val undo = backups.restore(backup.name)
        _state.update { it.copy(restored = undo) }
        null
    }

    fun restoreSeen() = _state.update { it.copy(restored = null) }

    /** Does [action] while showing [working]; a failure is shown by [failed] with what went wrong. */
    private fun run(working: String, failed: (String) -> String, action: suspend () -> String?) {
        if (_state.value.working != null) return
        _state.update { it.copy(working = working, message = null, error = null) }
        viewModelScope.launch {
            try {
                val message = action()
                _state.update { it.copy(message = message) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupException) {
                // The repository's messages are fixed texts, translated like the screen's own.
                _state.update { it.copy(error = e.message?.let(::tr)) }
            } catch (e: Exception) {
                Logger.w(e) { "Backup action failed: $working" }
                _state.update { it.copy(error = failed(e.message ?: e::class.simpleName.orEmpty())) }
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

/** The backups kept by the app: make one, import one, and restore, export or delete those in the history. */
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
    // A file dragged in from the desktop lands anywhere on the screen; the import card lights up meanwhile.
    var dropping by remember { mutableStateOf(false) }
    val dropTarget = Modifier.acceptsDroppedFiles(enabled = idle, onHover = { dropping = it }) { files ->
        scope.launch { viewModel.import(files.first().readBytes()) }
    }
    val wide = LocalWindowWidth.current.isExpanded
    val compact = LocalWindowWidth.current.isCompact
    val count = state.backups.size
    val pill: @Composable () -> Unit = {
        if (state.loaded) StatusPill(if (count == 0) tr("No backups yet") else trPlural(count, "{0} local backup", "{0} local backups"), if (count == 0) StatusTints.warning else StatusTints.ok)
    }

    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding, modifier = dropTarget) {
            ScreenHeader(tr("Backups"), tr("Protect your library, vocabulary, progress, and preferences."), onBackToSettings = onBack) { if (!compact) pill() }
            if (compact) pill()
            val create: @Composable (Modifier) -> Unit = { m ->
                ActionCard(
                    tr("Create a backup"),
                    tr("Save your languages, books, vocabulary, reading history, and settings."),
                    AppIcons.Storage,
                    note = tr("Stored locally on this device."),
                    modifier = m,
                ) { Button(onClick = viewModel::create, enabled = idle, shape = RoundedCornerShape(50)) { Text(tr("Create backup"), Modifier.padding(horizontal = 24.dp)) } }
            }
            val import: @Composable (Modifier) -> Unit = { m ->
                ActionCard(
                    tr("Import a backup"),
                    tr("Restore Tayra data from a previously exported backup file."),
                    AppIcons.UploadFile,
                    note = when {
                        dropping -> tr("Drop the file to import it.")
                        acceptsDroppedFiles -> tr("Or drop a backup file anywhere on this screen. It joins the history below, and you are asked whether to restore it right away.")
                        else -> tr("It joins the history below, and you are asked whether to restore it right away.")
                    },
                    modifier = m,
                    highlighted = dropping,
                ) { OutlinedButton(onClick = { picker.launch() }, enabled = idle, shape = RoundedCornerShape(50)) { Text(tr("Choose file"), Modifier.padding(horizontal = 24.dp)) } }
            }
            if (compact) {
                create(Modifier)
                import(Modifier)
            } else {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    create(Modifier.weight(1f).fillMaxHeight())
                    import(Modifier.weight(1f).fillMaxHeight())
                }
            }
            Progress(state)
            if (wide) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    HistoryCard(state, idle, viewModel, onRestore = { restoring = it }, onDelete = { deleting = it }, modifier = Modifier.weight(1.65f).fillMaxHeight())
                    IncludedCard(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                HistoryCard(state, idle, viewModel, onRestore = { restoring = it }, onDelete = { deleting = it })
                IncludedCard()
            }
        }
    }

    restoring?.let { backup ->
        AlertDialog(
            onDismissRequest = { restoring = null },
            title = { Text(tr("Restore this backup?")) },
            text = {
                Text(
                    tr("All languages, books, vocabulary, reading history and settings will be replaced by those of the backup of {0}. Your current data is backed up first, so you can go back to it.", formatBackupTime(backup)),
                )
            },
            confirmButton = { Button(onClick = { restoring = null; viewModel.restore(backup) }) { Text(tr("Restore")) } },
            dismissButton = { TextButton(onClick = { restoring = null }) { Text(tr("Cancel")) } },
        )
    }
    deleting?.let { backup ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(tr("Delete this backup?")) },
            text = { Text(tr("The backup of {0} will be deleted from this device. Exported copies are kept.", formatBackupTime(backup))) },
            confirmButton = { Button(onClick = { deleting = null; viewModel.delete(backup) }) { Text(tr("Delete")) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(tr("Cancel")) } },
        )
    }
    // An imported file is most often meant to be restored: offer that at once, with the same warning as the Restore button.
    state.imported?.let { backup ->
        AlertDialog(
            onDismissRequest = viewModel::importSeen,
            title = { Text(tr("Restore the imported backup?")) },
            text = {
                Text(
                    tr("The backup of {0} is in the list now. Restoring it replaces all languages, books, vocabulary, reading history and settings; your current data is backed up first, so you can go back to it.", formatBackupTime(backup)),
                )
            },
            confirmButton = { Button(onClick = { viewModel.importSeen(); viewModel.restore(backup) }) { Text(tr("Yes")) } },
            dismissButton = { TextButton(onClick = viewModel::importSeen) { Text(tr("No")) } },
        )
    }
    state.restored?.let { undo ->
        val done = { viewModel.restoreSeen(); onRestored() }
        AlertDialog(
            onDismissRequest = done,
            title = { Text(tr("Backup restored")) },
            text = { Text(tr("The data you had before is kept as the backup of {0}.", formatBackupTime(undo))) },
            confirmButton = { Button(onClick = done) { Text(tr("OK")) } },
        )
    }
}

/** What is being done right now, and how the last action went. */
@Composable
private fun Progress(state: BackupState) {
    state.working?.let { working ->
        Column(Modifier.fillMaxWidth()) {
            Text("$working…", style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
        }
    }
    state.message?.let { InfoBanner(it, tint = StatusTints.ok, icon = Icons.Default.CheckCircle) }
    state.error?.let { InfoBanner(it, tint = MaterialTheme.colorScheme.error, icon = Icons.Default.Warning) }
}

/** A card with one action: its icon, what it does, the button, and a note under it; [highlighted] while a drop would land in it. */
@Composable
private fun ActionCard(title: String, subtitle: String, icon: ImageVector, note: String, modifier: Modifier = Modifier, highlighted: Boolean = false, button: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (highlighted) colors.primaryContainer else colors.surface)
            .border(1.dp, if (highlighted) colors.primary else colors.outlineVariant, RoundedCornerShape(14.dp)).padding(24.dp),
    ) {
        IconTile(icon, size = 64)
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            button()
            Spacer(Modifier.height(12.dp))
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HistoryCard(
    state: BackupState,
    idle: Boolean,
    viewModel: BackupViewModel,
    onRestore: (Backup) -> Unit,
    onDelete: (Backup) -> Unit,
    modifier: Modifier = Modifier,
) {
    ContentCard(
        tr("Backup history"),
        tr("Newest first."),
        icon = AppIcons.History,
        modifier = modifier,
        titleExtra = { if (state.backups.isNotEmpty()) Tag(trPlural(state.backups.size, "{0} backup", "{0} backups"), MaterialTheme.colorScheme.primary) },
    ) {
        if (state.loaded && state.backups.isEmpty()) {
            Text(
                tr("No backups yet. Create one to keep a copy of your data."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.backups.forEachIndexed { i, backup ->
                BackupRow(
                    backup,
                    latest = i == 0,
                    enabled = idle,
                    onRestore = { onRestore(backup) },
                    onExport = { viewModel.export(backup) },
                    onDelete = { onDelete(backup) },
                )
            }
        }
        // Beside the other card the note sits at the bottom, level with that card's own note.
        if (LocalWindowWidth.current.isExpanded) Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(14.dp))
        InfoBanner(tr("Restoring replaces your current Tayra data after confirmation."))
    }
}

@Composable
private fun BackupRow(backup: Backup, latest: Boolean, enabled: Boolean, onRestore: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit) {
    // Too narrow for the date, status and buttons on one line, the buttons go below. The width is
    // measured rather than read from constraints, since the card sits in a row sized by intrinsics.
    val density = LocalDensity.current
    var stacked by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().onSizeChanged { stacked = with(density) { it.width.toDp() } < 700.dp }) {
        BackupRowContent(backup, latest, enabled, stacked = stacked || LocalWindowWidth.current.isCompact, onRestore, onExport, onDelete)
    }
}

@Composable
private fun BackupRowContent(
    backup: Backup,
    latest: Boolean,
    enabled: Boolean,
    stacked: Boolean,
    onRestore: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val actions: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onRestore, enabled = enabled, shape = RoundedCornerShape(10.dp)) { Text(tr("Restore")) }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onExport, enabled = enabled) {
                Icon(AppIcons.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("Export"))
            }
            Box {
                IconButton(onClick = { menu = true }, enabled = enabled) { Icon(AppIcons.MoreHoriz, contentDescription = tr("More for the backup of {0}", formatBackupTime(backup))) }
                AppMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    AppMenuItem(text = { Text(tr("Delete")) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(AppIcons.FileOutline, size = 44)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatBackupTime(backup),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (latest) {
                        Spacer(Modifier.width(10.dp))
                        Tag(tr("Latest"), MaterialTheme.colorScheme.primary)
                    }
                }
                Text(formatSize(backup.sizeBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!stacked) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(StatusTints.ok))
                Spacer(Modifier.width(8.dp))
                Text(tr("Ready"), style = MaterialTheme.typography.bodyMedium, color = StatusTints.ok)
                Spacer(Modifier.width(24.dp))
                actions()
            }
        }
        if (stacked) {
            Spacer(Modifier.height(6.dp))
            actions()
        }
    }
}

/** What a backup holds, and what has to be set up again after restoring it elsewhere. */
@Composable
private fun IncludedCard(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(AppIcons.VerifiedUser, size = 56)
            Spacer(Modifier.width(16.dp))
            Text(tr("What's included"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        listOf(tr("Languages and books"), tr("Vocabulary"), tr("Reading history"), tr("Settings")).forEach { IncludedLine(it, Icons.Default.CheckCircle, StatusTints.ok) }
        HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(tr("Not included"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
        listOf(tr("Audio files"), tr("Downloaded dictionaries"), tr("Voices and translation models"), tr("API keys"))
            .forEach { IncludedLine(it, AppIcons.RemoveCircle, MaterialTheme.colorScheme.outline) }
        if (LocalWindowWidth.current.isExpanded) Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(14.dp))
        InfoBanner(tr("These items can be downloaded or configured again after restoring."))
    }
}

@Composable
private fun IncludedLine(text: String, icon: ImageVector, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

internal fun formatBackupTime(backup: Backup): String = backup.createdAt?.let(::formatBackupTime) ?: backup.name

private fun formatBackupTime(time: LocalDateTime): String {
    fun two(n: Int) = n.toString().padStart(2, '0')
    return "${time.day} ${tr(MONTHS[time.month.ordinal])} ${time.year}, ${two(time.hour)}:${two(time.minute)}:${two(time.second)}"
}
