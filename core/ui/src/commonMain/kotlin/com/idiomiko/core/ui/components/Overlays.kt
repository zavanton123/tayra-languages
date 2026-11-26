package com.idiomiko.core.ui.components

import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupPositionProvider
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppElevation
import com.idiomiko.core.ui.theme.AppSizes

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
    /** How much the page behind is dimmed; a light veil leaves it readable, for settings seen taking effect. */
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
        sheetMaxWidth = Dp.Unspecified,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = scrimColor,
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
        DialogCard(modifier, maxWidth, color, content)
    }
}

/**
 * A dialog that takes the whole screen, as a phone shows a choice that must be made before anything
 * else; neither Back nor a tap closes it.
 */
@Composable
fun AppFullScreenDialog(content: @Composable () -> Unit) {
    Dialog(onDismissRequest = {}, properties = screenDialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        ScreenDialogBars()
        content()
    }
}

/** [AppDialogSurface]'s card for a dialog window made elsewhere, such as a navigation destination's. */
@Composable
fun DialogCard(
    modifier: Modifier = Modifier,
    maxWidth: Dp = AppSizes.dialogWide,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    // The same margin on every side: wide enough at the top to clear a title bar the window's content runs under.
    Surface(modifier.padding(LocalTitleBarInset.current + AppSpacing.l).widthIn(max = maxWidth).fillMaxWidth(), shape = AppShapes.large, color = color, content = content)
}

/**
 * A dialog that fills the screen, as a page of its own over the one under it: a phone's settings. Its content
 * keeps clear of the system bars, and of a title bar the window's content runs under (a narrow macOS window).
 */
@Composable
fun AppScreenDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    val titleBar = LocalTitleBarInset.current
    Dialog(onDismissRequest = onDismissRequest, properties = screenDialogProperties()) {
        ScreenDialogBars()
        Surface(modifier.fillMaxSize(), color = color) {
            Box(Modifier.padding(top = titleBar).windowInsetsPadding(WindowInsets.safeDrawing)) { content() }
        }
    }
}

/**
 * The height of a transparent native title bar the window's content runs under (macOS desktop),
 * which a dialog stays below so its top is not under the window's buttons; its margin on the other
 * sides grows to match.
 */
val LocalTitleBarInset = staticCompositionLocalOf { 0.dp }

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

/** Whether a page's descriptions show as tooltips over their titles rather than as lines under them. */
val LocalHintsAsTooltips = staticCompositionLocalOf { false }

/** A title, [content], with its [hint] in a tooltip over it on a page that wants [LocalHintsAsTooltips]. */
@Composable
fun HintedTitle(hint: String?, content: @Composable () -> Unit) {
    if (hint != null && LocalHintsAsTooltips.current) AppTooltip(hint, content = content) else content()
}

/** The [hint] to write under a title: none where it is the title's tooltip. */
@Composable
fun hintBelow(hint: String?): String? = hint.takeUnless { LocalHintsAsTooltips.current }
