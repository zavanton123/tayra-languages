package com.tayra.languages.feature.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.data.files.FileTextExtractor
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
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

@Composable
fun BookFormScreen(
    bookId: Long?,
    importUrl: String?,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    onSaved: (bookId: Long, isNew: Boolean) -> Unit,
    viewModel: BookFormViewModel = koinViewModel(key = "book-form-$bookId") { parametersOf(bookId, importUrl) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { if (it is BookFormEvent.Saved) onSaved(it.bookId, it.isNew) }
    val scope = rememberCoroutineScope()
    val filePicker = rememberFilePickerLauncher(type = FileKitType.File(FileTextExtractor.supportedExtensions)) { file ->
        if (file != null) scope.launch { viewModel.importFile(file.name, file.readBytes()) }
    }

    Scaffold(topBar = { AppTopBar(title = if (state.isNew) "New book" else "Edit book", onNavigate = onNavigate, onBack = onBack) }) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        val draft = state.draft
        val rtl = state.language?.rightToLeft == true
        val direction = if (rtl) TextDirection.Rtl else TextDirection.Ltr
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 900.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ErrorMessage(state.error)
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodyMedium) }

            if (state.isNew) {
                Dropdown(
                    options = state.languages,
                    selected = state.language,
                    onSelect = { language -> viewModel.update { it.copy(languageId = language.id) } },
                    label = "Language",
                    optionLabel = { it.name },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = draft.title,
                onValueChange = { title -> viewModel.update { it.copy(title = title) } },
                label = { Text("Title") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = direction),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.isNew) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.importUrl,
                        onValueChange = viewModel::setImportUrl,
                        label = { Text("Import web page (URL)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = viewModel::importWebPage, enabled = !state.busy && state.importUrl.isNotBlank()) { Text("Fetch") }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { filePicker.launch() }, enabled = !state.busy) { Text("Import text file") }
                    Text(
                        state.importedFileName ?: FileTextExtractor.supportedExtensions.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = draft.text,
                    onValueChange = { text -> viewModel.update { it.copy(text = text) } },
                    label = { Text("Text") },
                    supportingText = { Text("Paste the text, or import a file or web page above. Lines containing only --- force a page break.") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textDirection = direction),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 420.dp),
                )
            }
            OutlinedTextField(
                value = draft.sourceUri,
                onValueChange = { v -> viewModel.update { it.copy(sourceUri = v) } },
                label = { Text("Text source (URL)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TagInput(
                values = draft.tags,
                onValuesChange = { tags -> viewModel.update { it.copy(tags = tags) } },
                label = "Tags",
                suggestions = state.tagSuggestions,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.isNew) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Dropdown(
                        options = BookFormViewModel.splitModes,
                        selected = draft.splitBy,
                        onSelect = { mode -> viewModel.update { it.copy(splitBy = mode) } },
                        label = "Split by",
                        optionLabel = { it.label },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = draft.wordsPerPage.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { n -> viewModel.update { it.copy(wordsPerPage = n) } } },
                        label = { Text("Words per page (max ${BookDraft.MAX_WORDS_PER_PAGE})") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::save, enabled = !state.busy) { Text("Save") }
                OutlinedButton(onClick = onBack) { Text("Cancel") }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
