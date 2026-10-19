package com.tayra.languages.core.ui.components

import com.tayra.languages.core.ui.i18n.tr
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = tr("OK"),
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { QuietButton(confirmLabel, onClick = onConfirm, tone = if (destructive) ButtonTone.Danger else ButtonTone.Normal) },
        dismissButton = { QuietButton(tr("Cancel"), onClick = onDismiss) },
    )
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String = "",
    confirmLabel: String = tr("Save"),
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { AppTextField(value = value, onValueChange = { value = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { QuietButton(confirmLabel, onClick = { onConfirm(value) }, enabled = value.isNotBlank()) },
        dismissButton = { QuietButton(tr("Cancel"), onClick = onDismiss) },
    )
}
