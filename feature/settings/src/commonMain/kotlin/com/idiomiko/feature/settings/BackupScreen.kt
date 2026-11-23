package com.idiomiko.feature.settings

import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.saveable.rememberSaveable
import com.idiomiko.core.ui.components.ToastHost
import com.idiomiko.core.ui.components.rememberToastState
import androidx.compose.runtime.LaunchedEffect
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.i18n.tr
import androidx.compose.foundation.background
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.files.acceptsDroppedFiles
import com.idiomiko.core.ui.components.AppDialog
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppProgressBar
import com.idiomiko.core.ui.components.IconTile
import com.idiomiko.core.ui.components.InfoBanner
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.StatusDot
import com.idiomiko.core.ui.components.StatusPill
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.Tag
import com.idiomiko.core.ui.components.formatSize
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.shortDateWithYear
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.idiomiko.core.domain.backup.Backup
import com.idiomiko.core.domain.backup.BackupException
import com.idiomiko.core.domain.backup.BackupRepository
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppMenu
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.SettingsSection
import com.idiomiko.core.ui.components.SettingsPane
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.files.saveBinaryFile
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
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
) {
    /** Nothing is under way, so another action may start. */
    val idle: Boolean get() = working == null
}

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

    fun messageSeen() = _state.update { it.copy(message = null) }

    /** Does [action] while showing [working]; a failure is shown by [failed] with what went wrong. */
    private fun run(working: String, failed: (String) -> String, action: suspend () -> String?) {
        if (!_state.value.idle) return
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
    // How an action went shows for a moment over the screen, not as a block in it.
    val toast = rememberToastState()
    LaunchedEffect(state.message) {
        state.message?.let { toast.show(it); viewModel.messageSeen() }
    }
    var restoring by remember { mutableStateOf<Backup?>(null) }
    var deleting by remember { mutableStateOf<Backup?>(null) }
    val picker = rememberFilePickerLauncher(type = FileKitType.File()) { file ->
        if (file != null) scope.launch { viewModel.import(file.readBytes()) }
    }
    val idle = state.idle
    // A file dragged in from the desktop lands anywhere on the screen; the import card lights up meanwhile.
    var dropping by remember { mutableStateOf(false) }
    val dropTarget = Modifier.acceptsDroppedFiles(enabled = idle, onHover = { dropping = it }) { files ->
        scope.launch { viewModel.import(files.first().readBytes()) }
    }
    val compact = LocalWindowWidth.current.isCompact
    if (compact) {
        PhoneBackupsScreen(state, idle, viewModel, onImport = { picker.launch() }, onRestore = { restoring = it }, onDelete = { deleting = it }, onNavigate = onNavigate, onBack = onBack)
    } else Scaffold(
        topBar = { AppTopBar(title = "Idiomiko", onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        SettingsPane(SettingsSection.BACKUPS, padding, onNavigate) { inner ->
            PageColumn(inner, modifier = dropTarget) {
                ScreenHeader(tr("Backups"), null, onBackToSettings = onBack)
                val create: @Composable (Modifier) -> Unit = { m ->
                    ActionCard(
                        tr("Create a backup"),
                        tr("Save your languages, books, vocabulary, reading history, and settings."),
                        AppIcons.Storage,
                        modifier = m,
                    ) { PrimaryButton(onClick = viewModel::create, enabled = idle) { Text(tr("Create backup"), Modifier.padding(horizontal = AppSpacing.xxl)) } }
                }
                val import: @Composable (Modifier) -> Unit = { m ->
                    ActionCard(
                        tr("Import a backup"),
                        tr("Restore Idiomiko data from a previously exported backup file."),
                        AppIcons.UploadFile,
                        note = if (dropping) tr("Drop the file to import it.") else null,
                        modifier = m,
                        highlighted = dropping,
                    ) { SecondaryButton(onClick = { picker.launch() }, enabled = idle) { Text(tr("Choose file"), Modifier.padding(horizontal = AppSpacing.xxl)) } }
                }
                if (compact) {
                    create(Modifier)
                    import(Modifier)
                } else {
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        create(Modifier.weight(1f).fillMaxHeight())
                        import(Modifier.weight(1f).fillMaxHeight())
                    }
                }
                Progress(state)
                HistoryCard(state, idle, viewModel, onRestore = { restoring = it }, onDelete = { deleting = it })
            }
        }
    }

    restoring?.let { backup ->
        AppDialog(
            onDismissRequest = { restoring = null },
            title = { Text(tr("Restore this backup?")) },
            text = {
                Text(
                    tr("All languages, books, vocabulary, reading history and settings will be replaced by those of the backup of {0}. Your current data is backed up first, so you can go back to it.", formatBackupTime(backup)),
                )
            },
            confirmButton = { PrimaryButton(onClick = { restoring = null; viewModel.restore(backup) }) { Text(tr("Restore")) } },
            dismissButton = { QuietButton(onClick = { restoring = null }) { Text(tr("Cancel")) } },
        )
    }
    deleting?.let { backup ->
        AppDialog(
            onDismissRequest = { deleting = null },
            title = { Text(tr("Delete this backup?")) },
            text = { Text(tr("The backup of {0} will be deleted from this device. Exported copies are kept.", formatBackupTime(backup))) },
            confirmButton = { PrimaryButton(onClick = { deleting = null; viewModel.delete(backup) }) { Text(tr("Delete")) } },
            dismissButton = { QuietButton(onClick = { deleting = null }) { Text(tr("Cancel")) } },
        )
    }
    // An imported file is most often meant to be restored: offer that at once, with the same warning as the Restore button.
    state.imported?.let { backup ->
        AppDialog(
            onDismissRequest = viewModel::importSeen,
            title = { Text(tr("Restore the imported backup?")) },
            text = {
                Text(
                    tr("The backup of {0} is in the list now. Restoring it replaces all languages, books, vocabulary, reading history and settings; your current data is backed up first, so you can go back to it.", formatBackupTime(backup)),
                )
            },
            confirmButton = { PrimaryButton(onClick = { viewModel.importSeen(); viewModel.restore(backup) }) { Text(tr("Yes")) } },
            dismissButton = { QuietButton(onClick = viewModel::importSeen) { Text(tr("No")) } },
        )
    }
    ToastHost(toast)
    state.restored?.let { undo ->
        val done = { viewModel.restoreSeen(); onRestored() }
        AppDialog(
            onDismissRequest = done,
            title = { Text(tr("Backup restored")) },
            text = { Text(tr("The data you had before is kept as the backup of {0}.", formatBackupTime(undo))) },
            confirmButton = { PrimaryButton(onClick = done) { Text(tr("OK")) } },
        )
    }
}

/** What is being done right now, and how the last action went. */
@Composable
internal fun Progress(state: BackupState) {
    state.working?.let { working ->
        Column(Modifier.fillMaxWidth()) {
            Text("$working…", style = MaterialTheme.typography.bodyMedium)
            AppProgressBar(null, Modifier.padding(top = AppSpacing.s))
        }
    }
    state.error?.let { InfoBanner(it, tint = MaterialTheme.colorScheme.error, icon = Icons.Default.Warning) }
}

/** A card with one action: its icon, what it does, the button, and a note under it; [highlighted] while a drop would land in it. */
@Composable
private fun ActionCard(title: String, subtitle: String, icon: ImageVector, note: String? = null, modifier: Modifier = Modifier, highlighted: Boolean = false, button: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clip(AppShapes.card).background(if (highlighted) colors.primaryContainer else colors.surface)
            .border(if (highlighted) AppBorders.selected() else AppBorders.hairline(), AppShapes.card).padding(AppSpacing.xxl),
    ) {
        IconTile(icon, size = 64)
        Spacer(Modifier.width(AppSpacing.xl))
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.sectionTitle)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(AppSpacing.xl))
            button()
            if (note != null) {
                Spacer(Modifier.height(AppSpacing.m))
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
        null,
        icon = AppIcons.History,
        modifier = modifier,
    ) {
        if (state.loaded && state.backups.isEmpty()) {
            Text(
                tr("Create a backup to keep a copy of your data."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = AppSpacing.m),
            )
        }
        var more by rememberSaveable { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            state.backups.recent(more).forEachIndexed { i, backup ->
                BackupRow(
                    backup,
                    latest = i == 0,
                    enabled = idle,
                    onRestore = { onRestore(backup) },
                    onExport = { viewModel.export(backup) },
                    onDelete = { onDelete(backup) },
                )
            }
            MoreBackups(state.backups, more) { more = !more }
        }
    }
}

/** How many of the newest backups the history shows until More opens the rest. */
private const val RECENT_BACKUPS = 3

/** The newest backups, or all of them once [more] is open. */
internal fun List<Backup>.recent(more: Boolean): List<Backup> = if (more) this else take(RECENT_BACKUPS)

/** More under the newest backups when there are older ones, and Less to put them away again. */
@Composable
internal fun MoreBackups(backups: List<Backup>, more: Boolean, onToggle: () -> Unit) {
    if (backups.size <= RECENT_BACKUPS) return
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        QuietButton(
            if (more) tr("Less") else tr("More"),
            onClick = onToggle,
            modifier = Modifier.testTag("backups-more"),
            icon = if (more) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
        )
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
            SecondaryButton(onClick = onRestore, enabled = enabled) { Text(tr("Restore")) }
            Spacer(Modifier.width(AppSpacing.s))
            QuietButton(tr("Export"), onClick = onExport, icon = AppIcons.Share, enabled = enabled)
            Box {
                AppIconButton(AppIcons.MoreHoriz, contentDescription = tr("More for the backup of {0}", formatBackupTime(backup)), onClick = { menu = true }, enabled = enabled)
                AppMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    AppMenuItem(text = { Text(tr("Delete")) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
    Column(
        Modifier.fillMaxWidth().clip(AppShapes.control).border(AppBorders.hairline(), AppShapes.control)
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(AppIcons.FileOutline, size = 44)
            Spacer(Modifier.width(AppSpacing.l))
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
                        Spacer(Modifier.width(AppSpacing.m))
                        Tag(tr("Latest"), MaterialTheme.colorScheme.primary)
                    }
                }
                Text(formatSize(backup.sizeBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!stacked) {
                Spacer(Modifier.width(AppSpacing.xxl))
                actions()
            }
        }
        if (stacked) {
            Spacer(Modifier.height(AppSpacing.s))
            actions()
        }
    }
}

internal fun formatBackupTime(backup: Backup, seconds: Boolean = true): String = backup.createdAt?.let { formatBackupTime(it, seconds) } ?: backup.name

private fun formatBackupTime(time: LocalDateTime, seconds: Boolean): String {
    fun two(n: Int) = n.toString().padStart(2, '0')
    val clock = "${two(time.hour)}:${two(time.minute)}" + if (seconds) ":${two(time.second)}" else ""
    return "${time.date.shortDateWithYear()}, $clock"
}
