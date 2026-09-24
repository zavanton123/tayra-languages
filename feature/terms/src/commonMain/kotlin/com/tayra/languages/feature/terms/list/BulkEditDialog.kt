package com.tayra.languages.feature.terms.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
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
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.BulkTermUpdate
import com.tayra.languages.core.ui.components.Dropdown

/** Bulk changes for the selected terms; the ids are filled in by the caller. */
@Composable
fun BulkEditDialog(count: Int, onApply: (BulkTermUpdate) -> Unit, onDismiss: () -> Unit) {
    var lowercase by remember { mutableStateOf(false) }
    var removeParents by remember { mutableStateOf(false) }
    var parent by remember { mutableStateOf("") }
    var changeStatus by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(TermStatus.NEW_1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Updating $count term(s)") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CheckRow("Lowercase terms", lowercase) { lowercase = it }
                CheckRow("Remove parents", removeParents) { removeParents = it }
                OutlinedTextField(value = parent, onValueChange = { parent = it }, label = { Text("Set parent") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                CheckRow("Change status", changeStatus) { changeStatus = it }
                if (changeStatus) {
                    Dropdown(options = TermStatus.selectable, selected = status, onSelect = { status = it }, label = "Status", optionLabel = { it.label }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(
                    BulkTermUpdate(
                        termIds = emptyList(),
                        lowercaseTerms = lowercase,
                        removeParents = removeParents,
                        parentText = parent.trim().ifEmpty { null },
                        status = if (changeStatus) status else null,
                    ),
                )
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}
