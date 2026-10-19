package com.tayra.languages.feature.terms.list

import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import com.tayra.languages.core.ui.components.AppCheckbox
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.QuietButton

/** Bulk changes for the selected terms; the ids are filled in by the caller. */
@Composable
fun BulkEditDialog(count: Int, onApply: (BulkTermUpdate) -> Unit, onDismiss: () -> Unit) {
    var lowercase by remember { mutableStateOf(false) }
    var removeParents by remember { mutableStateOf(false) }
    var parent by remember { mutableStateOf("") }
    var changeStatus by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(TermStatus.NEW_1) }

    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(trPlural(count, "Updating {0} term", "Updating {0} terms")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CheckRow(tr("Lowercase terms"), lowercase) { lowercase = it }
                CheckRow(tr("Remove parents"), removeParents) { removeParents = it }
                AppTextField(value = parent, onValueChange = { parent = it }, label = { Text(tr("Set parent")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                CheckRow(tr("Change status"), changeStatus) { changeStatus = it }
                if (changeStatus) {
                    Dropdown(options = TermStatus.selectable, selected = status, onSelect = { status = it }, label = tr("Status"), optionLabel = { tr(it.label) }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            QuietButton(onClick = {
                onApply(
                    BulkTermUpdate(
                        termIds = emptyList(),
                        lowercaseTerms = lowercase,
                        removeParents = removeParents,
                        parentText = parent.trim().ifEmpty { null },
                        status = if (changeStatus) status else null,
                    ),
                )
            }) { Text(tr("Apply")) }
        },
        dismissButton = { QuietButton(tr("Cancel"), onClick = onDismiss) },
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppCheckbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}
