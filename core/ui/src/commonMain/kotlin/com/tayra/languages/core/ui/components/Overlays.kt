package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupPositionProvider
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppElevation
import com.tayra.languages.core.ui.theme.AppSizes

/*
 * The design system's overlays and raised surfaces: the phone's bottom sheet, the surface of a
 * dialog with content of its own (AppDialog has a title, a text and buttons; this one is for the
 * larger ones), side panels, popups, bars and tooltips.
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
    maxWidth: Dp = AppSizes.dialogWide,
    dismissible: Boolean = true,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = dismissible)) {
        Surface(modifier.padding(AppSpacing.l).widthIn(max = maxWidth).fillMaxWidth(), shape = AppShapes.large, color = color, content = content)
    }
}

/** A panel beside a page's content, such as the term form next to the text: a card of [width]. */
@Composable
fun SidePanel(width: Dp, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(modifier.width(width).cardSurface(AppShapes.card), content = content)
    }
}

/** A card that floats over the page, such as a word's popup or a list of shortcuts: raised, in an outline. */
@Composable
fun PopupCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier, shape = AppShapes.card, color = MaterialTheme.colorScheme.surface, shadowElevation = AppElevation.high, border = AppBorders.hairline(), content = content)
}

/** A bar across the top or bottom of a page that stays while the page scrolls: a phone's reading header, a form's buttons. [raised] lifts it with a tone. */
@Composable
fun BarSurface(modifier: Modifier = Modifier, raised: Boolean = false, content: @Composable () -> Unit) {
    Surface(modifier, color = MaterialTheme.colorScheme.surface, tonalElevation = if (raised) AppElevation.low else 0.dp, content = content)
}

/** A short hint over [content] while it is hovered or pressed; [positionProvider] places it, above by default. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTooltip(text: String, modifier: Modifier = Modifier, positionProvider: PopupPositionProvider? = null, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = positionProvider ?: TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
        modifier = modifier,
        content = content,
    )
}

/**
 * A larger hint with a [title] and a [body] over [content]; [persistent] keeps it while the pointer
 * stays (on touch, until a tap elsewhere) rather than hiding it after a moment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRichTooltip(
    title: String,
    modifier: Modifier = Modifier,
    persistent: Boolean = false,
    positionProvider: PopupPositionProvider? = null,
    body: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = positionProvider ?: TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { RichTooltip(title = { Text(title) }) { body() } },
        state = rememberTooltipState(isPersistent = persistent),
        modifier = modifier,
        content = content,
    )
}
