package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tayra.languages.core.ui.theme.AppShapes

/*
 * The design system's overlays: the phone's bottom sheet, and the surface of a dialog with content
 * of its own (AppDialog has a title, a text and buttons; this one is for the larger ones).
 */

/**
 * A sheet from the bottom of a phone's screen, opened all the way and as wide as the screen, on
 * the surface colour. [skipPartiallyExpanded] false lets it stop half-way, for long content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
        sheetMaxWidth = Dp.Unspecified,
        containerColor = MaterialTheme.colorScheme.surface,
        content = content,
    )
}

/**
 * A dialog whose content is its own: a card of [maxWidth] at most on the large containers' shape.
 * [dismissible] false keeps it open on a tap outside, for a choice that must be made.
 */
@Composable
fun AppDialogSurface(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 760.dp,
    dismissible: Boolean = true,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = dismissible)) {
        Surface(modifier.padding(16.dp).widthIn(max = maxWidth).fillMaxWidth(), shape = AppShapes.large, color = color, content = content)
    }
}
