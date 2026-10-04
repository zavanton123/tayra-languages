package com.tayra.languages.feature.books

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.data.files.FileTextExtractor
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.TagInput
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class ContentSource(val label: String) { PASTE("Paste text"), FILE("Import file") }

/**
 * Creates a book from pasted text or a file, or edits a book: the same form, with the book's
 * whole text, title and tags filled in and no file import. Changing the text or the page setup
 * rebuilds the pages on save.
 */
@Composable
fun BookFormScreen(
    bookId: Long?,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    onSaved: (bookId: Long, isNew: Boolean) -> Unit,
    viewModel: BookFormViewModel = koinViewModel(key = "book-form-$bookId") { parametersOf(bookId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { if (it is BookFormEvent.Saved) onSaved(it.bookId, it.isNew) }
    var source by remember { mutableStateOf(ContentSource.PASTE) }
    val scope = rememberCoroutineScope()
    val filePicker = rememberFilePickerLauncher(type = FileKitType.File(FileTextExtractor.supportedExtensions)) { file ->
        if (file != null) scope.launch {
            viewModel.importFile(file.name, file.readBytes())
            // The imported text is shown where it can be checked and edited.
            source = ContentSource.PASTE
        }
    }
    val wide = LocalWindowWidth.current.isExpanded

    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, section = NavSection.BOOKS, onBack = onBack) },
        bottomBar = { if (!state.loading) ActionBar(state, onCancel = onBack, onSave = viewModel::save) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        val languageName = state.language?.name
        PageColumn(padding) {
            ScreenHeader(
                if (state.isNew) "Create new book" else "Edit book",
                when {
                    !state.isNew -> "Change the text, title, tags and pages of “${state.loaded?.title?.ifBlank { null } ?: "this book"}”."
                    languageName != null -> "Add a $languageName text to your library."
                    else -> "Add a text to your library."
                },
            )
            state.error?.let { InfoBanner(it, tint = MaterialTheme.colorScheme.error, icon = Icons.Default.Warning) }
            state.notice?.let { InfoBanner(it, tint = StatusTints.ok) }
            when {
                wide -> Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    BookContentCard(state, source, onSource = { source = it }, onChooseFile = { filePicker.launch() }, viewModel, Modifier.weight(1.65f))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        DetailsCard(state, viewModel)
                        PageSetupCard(state, viewModel)
                    }
                }
                else -> {
                    BookContentCard(state, source, onSource = { source = it }, onChooseFile = { filePicker.launch() }, viewModel)
                    DetailsCard(state, viewModel)
                    PageSetupCard(state, viewModel)
                }
            }
        }
    }
}

/** The text of the new book: typed or pasted, or read from a file. */
@Composable
private fun BookContentCard(
    state: BookFormUiState,
    source: ContentSource,
    onSource: (ContentSource) -> Unit,
    onChooseFile: () -> Unit,
    viewModel: BookFormViewModel,
    modifier: Modifier = Modifier,
) {
    val formats = FileTextExtractor.supportedExtensions.joinToString(" · ") { it.uppercase() }
    val rtl = state.language?.rightToLeft == true
    val subtitle = if (state.isNew) "Add the text for your book." else "Edit the text of your book."
    ContentCard("Book content", subtitle, icon = AppIcons.FileOutline, modifier = modifier) {
        // A file can only start a book; an existing book's text is edited in place.
        if (state.isNew) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceToggle(source, onSource)
                Spacer(Modifier.width(20.dp))
                Text(formats, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
        }
        when (if (state.isNew) source else ContentSource.PASTE) {
            ContentSource.PASTE -> {
                FieldLabel("Text")
                OutlinedTextField(
                    value = state.draft.text,
                    onValueChange = { text -> viewModel.update { it.copy(text = text) } },
                    placeholder = { Text(state.language?.name?.let { "Paste your $it text here…" } ?: "Paste your text here…") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Use a line containing only --- to force a page break.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    val words = wordCount(state.draft.text)
                    Text("$words word${if (words == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ContentSource.FILE -> FileDrop(state, formats, onChooseFile)
        }
    }
}

/** The number of words in [text], counted at spaces; a rough count for scripts written without them. */
private fun wordCount(text: String): Int = text.split(Regex("\\s+")).count { word -> word.any { it.isLetterOrDigit() } }

@Composable
private fun SourceToggle(selected: ContentSource, onSelect: (ContentSource) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(4.dp)) {
        ContentSource.entries.forEach { option ->
            val active = option == selected
            val icon: ImageVector = if (option == ContentSource.PASTE) AppIcons.FileOutline else AppIcons.UploadFile
            Row(
                Modifier.clip(RoundedCornerShape(9.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(option.label, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun FileDrop(state: BookFormUiState, formats: String, onChooseFile: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(enabled = !state.busy, onClick = onChooseFile)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(AppIcons.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(14.dp))
        Text("Choose a file to read its text", style = MaterialTheme.typography.titleMedium)
        Text("$formats — the text appears under Paste text, ready to check.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onChooseFile, enabled = !state.busy, shape = RoundedCornerShape(50)) { Text(if (state.busy) "Reading…" else "Choose file") }
        state.importedFileName?.let {
            Spacer(Modifier.height(10.dp))
            Text("Last imported: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailsCard(state: BookFormUiState, viewModel: BookFormViewModel, modifier: Modifier = Modifier) {
    val draft = state.draft
    val rtl = state.language?.rightToLeft == true
    ContentCard("Book details", "Give your book a title and optional tags.", icon = AppIcons.MenuBook, modifier = modifier) {
        // A new book is in the language being learned; the field is only for the case that none is chosen.
        if (state.isNew && draft.languageId == 0L) {
            FieldLabel("Language")
            Dropdown(
                options = state.languages,
                selected = state.language,
                onSelect = { language -> viewModel.update { it.copy(languageId = language.id) } },
                label = null,
                optionLabel = { it.name },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }
        FieldLabel("Title")
        OutlinedTextField(
            value = draft.title,
            onValueChange = { title -> viewModel.update { it.copy(title = title) } },
            placeholder = { Text("Enter a title") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        FieldLabel("Tags")
        TagInput(
            values = draft.tags,
            onValuesChange = { tags -> viewModel.update { it.copy(tags = tags) } },
            label = "Add tags",
            suggestions = state.tagSuggestions,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Press Enter or a comma after each tag.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun PageSetupCard(state: BookFormUiState, viewModel: BookFormViewModel) {
    val draft = state.draft
    ContentCard("Page setup", "Control how your book is split into pages.", icon = Icons.Default.Settings) {
        FieldLabel("Split by")
        Dropdown(
            options = BookFormViewModel.splitModes,
            selected = draft.splitBy,
            onSelect = { mode -> viewModel.update { it.copy(splitBy = mode) } },
            label = null,
            optionLabel = { it.label },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        FieldLabel("Words per page")
        OutlinedTextField(
            value = draft.wordsPerPage.toString(),
            onValueChange = { v -> v.filter { it.isDigit() }.take(4).toIntOrNull()?.let { n -> viewModel.update { it.copy(wordsPerPage = n) } } },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Maximum ${BookDraft.MAX_WORDS_PER_PAGE}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(16.dp))
        val pages = "${state.pageCount} page${if (state.pageCount == 1) "" else "s"}"
        InfoBanner(
            when {
                state.isNew -> "Pages will be created automatically when you save."
                state.rebuildsPages -> "Saving rebuilds the pages. Your place, bookmarks and read pages carry over."
                else -> "The book has $pages. Change the text or these settings to rebuild them."
            },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 6.dp))
}

/** Cancel and save, kept in view at the bottom of the window. */
@Composable
private fun ActionBar(state: BookFormUiState, onCancel: () -> Unit, onSave: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    Modifier.widthIn(max = 1480.dp).fillMaxWidth()
                        .padding(horizontal = if (LocalWindowWidth.current.isCompact) 16.dp else 48.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(10.dp)) { Text("Cancel", Modifier.padding(horizontal = 12.dp)) }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = onSave, enabled = !state.busy, shape = RoundedCornerShape(10.dp)) {
                        Text(if (state.isNew) "Create book" else "Save changes", Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }
    }
}
