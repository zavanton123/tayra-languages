package com.tayra.languages.feature.terms.import

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.service.BadImportFileException
import com.tayra.languages.core.domain.service.TermImportOptions
import com.tayra.languages.core.domain.service.TermImportResult
import com.tayra.languages.core.domain.service.TermImportService
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.navigation.Route
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class TermImportUiState(
    val fileName: String? = null,
    val csv: String? = null,
    val options: TermImportOptions = TermImportOptions(),
    val busy: Boolean = false,
    val error: String? = null,
    val result: TermImportResult? = null,
)

class TermImportViewModel(private val importService: TermImportService) : ViewModel() {
    private val _state = MutableStateFlow(TermImportUiState())
    val state: StateFlow<TermImportUiState> = _state.asStateFlow()

    fun setFile(name: String, bytes: ByteArray) {
        _state.update { it.copy(fileName = name, csv = bytes.decodeToString(), error = null, result = null) }
    }

    fun setOptions(options: TermImportOptions) = _state.update { it.copy(options = options) }

    fun import() {
        val csv = _state.value.csv ?: return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val result = importService.import(csv, _state.value.options)
                _state.update { it.copy(busy = false, result = result) }
            } catch (e: BadImportFileException) {
                _state.update { it.copy(busy = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.message ?: "Import failed") }
            }
        }
    }
}

@Composable
fun TermImportScreen(onNavigate: (Route) -> Unit, viewModel: TermImportViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val picker = rememberFilePickerLauncher(type = FileKitType.File(listOf("csv", "txt"))) { file ->
        if (file != null) scope.launch { viewModel.setFile(file.name, file.readBytes()) }
    }
    Scaffold(topBar = { AppTopBar(title = "Import terms", onNavigate = onNavigate) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Import a CSV file with the columns language, term and optionally translation, parent, status, tags, pronunciation and link_status.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch() }) { Text("Choose file") }
                Text(state.fileName ?: "No file selected", style = MaterialTheme.typography.bodySmall)
            }
            OptionRow("Create new terms", state.options.createTerms) { viewModel.setOptions(state.options.copy(createTerms = it)) }
            OptionRow("Update existing terms", state.options.updateTerms) { viewModel.setOptions(state.options.copy(updateTerms = it)) }
            OptionRow("Import new terms as unknown", state.options.newAsUnknown) { viewModel.setOptions(state.options.copy(newAsUnknown = it)) }
            Button(onClick = viewModel::import, enabled = state.csv != null && !state.busy) { Text("Import") }
            ErrorMessage(state.error)
            state.result?.let {
                Text("Created ${it.created}, updated ${it.updated}, skipped ${it.skipped}.", color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun OptionRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}
