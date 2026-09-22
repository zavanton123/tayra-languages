package com.tayra.languages.feature.terms.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.model.TermTag
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.EmptyMessage
import com.tayra.languages.core.ui.navigation.Route
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class TermTagsViewModel(private val terms: TermRepository) : ViewModel() {
    val tags: StateFlow<List<TermTag>> = terms.observeTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun save(tag: TermTag) = viewModelScope.launch { terms.saveTag(tag) }
    fun delete(id: Long) = viewModelScope.launch { terms.deleteTag(id) }
}

@Composable
fun TermTagsScreen(onNavigate: (Route) -> Unit, viewModel: TermTagsViewModel = koinViewModel()) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<TermTag?>(null) }
    var deleting by remember { mutableStateOf<TermTag?>(null) }

    Scaffold(topBar = { AppTopBar(title = "Term tags", onNavigate = onNavigate) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Button(onClick = { editing = TermTag(text = "") }, modifier = Modifier.padding(16.dp)) { Text("New tag") }
            if (tags.isEmpty()) {
                EmptyMessage("No tags yet.")
            } else {
                LazyColumn {
                    items(tags, key = { it.id }) { tag ->
                        Row(Modifier.fillMaxWidth().clickable { editing = tag }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tag.text, style = MaterialTheme.typography.titleMedium)
                                if (tag.comment.isNotBlank()) Text(tag.comment, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { deleting = tag }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    editing?.let { tag ->
        var text by remember(tag) { mutableStateOf(tag.text) }
        var comment by remember(tag) { mutableStateOf(tag.comment) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (tag.id == 0L) "New tag" else "Edit tag") },
            text = {
                Column {
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Tag") }, singleLine = true)
                    OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Comment") })
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.save(tag.copy(text = text.trim(), comment = comment.trim())); editing = null }, enabled = text.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
    deleting?.let { tag ->
        ConfirmDialog(
            title = "Delete tag \"${tag.text}\"?",
            text = "The tag will be removed from all terms.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { viewModel.delete(tag.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}
