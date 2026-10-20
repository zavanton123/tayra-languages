package com.tayra.languages.feature.terms.list

import com.tayra.languages.core.ui.components.shownName
import com.tayra.languages.core.ui.components.shownStatuses
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.BulkTermUpdate
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.CheckSetting
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
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                CheckSetting(tr("Lowercase terms"), lowercase, onChange = { lowercase = it })
                CheckSetting(tr("Remove parents"), removeParents, onChange = { removeParents = it })
                AppTextField(value = parent, onValueChange = { parent = it }, label = { Text(tr("Set parent")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                CheckSetting(tr("Change status"), changeStatus, onChange = { changeStatus = it })
                if (changeStatus) {
                    val names = shownStatuses(TermStatus.selectable).associateWith { it.shownName() }
                    Dropdown(options = names.keys.toList(), selected = status, onSelect = { status = it }, label = tr("Status"), optionLabel = { names.getValue(it) }, modifier = Modifier.fillMaxWidth())
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

