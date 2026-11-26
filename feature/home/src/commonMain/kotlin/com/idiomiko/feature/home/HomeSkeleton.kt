package com.idiomiko.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.PhoneNavBar
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.placeholder
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSpacing

/**
 * The home page's outline while its figures load, and the app's while it starts: its cards where they will be, with
 * grey blocks for their pictures, headings and lines, so the page fills in where the eye already is.
 */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    val loading = tr("Loading…")
    Column(modifier.testTag("home-skeleton").clearAndSetSemantics { contentDescription = loading }) {
        if (LocalWindowWidth.current.isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                PhoneTodayBones()
                PhoneNextUpBones()
                ContinueBones(phone = true)
                VocabularyBones()
            }
            return@Column
        }
        WideHeaderBones()
        Spacer(Modifier.height(AppSpacing.xl))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= TWO_COLUMNS) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    Column(Modifier.weight(LEFT_SHARE), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        WideNextUpBones()
                        ContinueBones(phone = false)
                        RecommendedBones()
                    }
                    Column(Modifier.weight(1f - LEFT_SHARE), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        VocabularyBones()
                        RecentWordsBones()
                        ActivityBones()
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    WideNextUpBones()
                    ContinueBones(phone = false)
                    VocabularyBones()
                }
            }
        }
    }
}

/**
 * The home page with the app's bars, as the app shows it while it starts. Nothing in it answers a touch yet:
 * the bars only show where they will be.
 */
@Composable
fun StartupSkeleton() {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = tr("Home"), onNavigate = {}, section = NavSection.HOME)
            Box(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
                PageColumn(PaddingValues()) { HomeSkeleton() }
            }
            if (LocalWindowWidth.current.isCompact) PhoneNavBar(NavSection.HOME, onNavigate = {})
        }
        // Over it all, taking the touches and the pointer, so a tab does not light up or open its sheet.
        Box(Modifier.matchParentSize().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } })
    }
}

/** A grey block of [width] (the line's whole width when null) and [height]. */
@Composable
private fun Bone(width: Dp?, height: Dp, modifier: Modifier = Modifier, shape: Shape = AppShapes.small) {
    Box((if (width != null) modifier.width(width) else modifier.fillMaxWidth()).height(height).placeholder(shape))
}

@Composable
private fun BoneCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().cardSurface(AppShapes.card).padding(AppSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        content = content,
    )
}

/** A card's heading: its icon's tile and its title. */
@Composable
private fun CardHeadingBones(title: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Bone(ICON_TILE, ICON_TILE, shape = AppShapes.panel)
        Bone(title, HEADING)
    }
}

@Composable
private fun RowScope.TwoLines(over: Dp, main: Dp) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Bone(over, SMALL_LINE)
        Bone(main, LINE)
    }
}

@Composable
private fun PhoneTodayBones() = BoneCard {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Bone(RING_PHONE, RING_PHONE, shape = AppShapes.circle)
        Bone(GOAL_TITLE, HEADING)
    }
    AppDivider()
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Bone(FIGURE_PHONE, LINE)
        Bone(FIGURE_PHONE, LINE)
    }
}

@Composable
private fun PhoneNextUpBones() = BoneCard {
    Bone(OVERLINE, SMALL_LINE)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Bone(PICTURE_PHONE, PICTURE_PHONE, shape = AppShapes.panel)
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Bone(NEXT_TITLE_PHONE, HEADING)
            Bone(MINUTES, SMALL_LINE)
        }
    }
    Buttons()
}

@Composable
private fun WideHeaderBones() {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(end = AppSpacing.xxl), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Bone(GREETING, GREETING_HEIGHT)
            Bone(CHEER, LINE)
        }
        Row(
            Modifier.cardSurface(AppShapes.card).padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            Bone(RING_WIDE, RING_WIDE, shape = AppShapes.circle)
            Bone(GOAL_TITLE, HEADING)
            repeat(FIGURES) { Bone(FIGURE_WIDE, FIGURE_HEIGHT, shape = AppShapes.pill) }
        }
    }
}

@Composable
private fun WideNextUpBones() = BoneCard {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl)) {
        Bone(PICTURE_WIDE, PICTURE_WIDE, shape = AppShapes.large)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Bone(NEXT_TITLE_WIDE, TITLE)
            Bone(MINUTES, LINE)
            Spacer(Modifier.height(AppSpacing.s))
            Buttons()
        }
    }
}

@Composable
private fun Buttons() {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Bone(null, BUTTON, Modifier.weight(BUTTON_SHARE), shape = AppShapes.control)
        Bone(null, BUTTON, Modifier.weight(1f - BUTTON_SHARE), shape = AppShapes.control)
    }
}

@Composable
private fun ContinueBones(phone: Boolean) = BoneCard {
    if (phone) CardHeadingBones(CONTINUE_TITLE) else Bone(CONTINUE_TITLE, HEADING)
    repeat(CONTINUE_ROWS) { i ->
        if (i > 0) AppDivider()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Bone(ICON_TILE, ICON_TILE, shape = AppShapes.panel)
            TwoLines(ROW_OVER, if (phone) ROW_TITLE_PHONE else ROW_TITLE)
            if (!phone) Bone(PROGRESS, BAR, shape = AppShapes.pill)
        }
    }
}

@Composable
private fun RecommendedBones() = BoneCard {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Bone(ICON_TILE_LARGE, ICON_TILE_LARGE, shape = AppShapes.panel)
        TwoLines(ROW_OVER, ROW_TITLE)
    }
}

@Composable
private fun VocabularyBones() = BoneCard {
    CardHeadingBones(VOCABULARY_TITLE)
    Bone(VOCABULARY_LINE, LINE)
    Bone(null, BAR, shape = AppShapes.pill)
}

@Composable
private fun RecentWordsBones() = BoneCard {
    CardHeadingBones(RECENT_TITLE)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        WORD_CHIPS.forEach { Bone(it, CHIP, shape = AppShapes.pill) }
    }
}

@Composable
private fun ActivityBones() = BoneCard {
    CardHeadingBones(ACTIVITY_TITLE)
    Bone(null, ACTIVITY_GRID, shape = AppShapes.compact)
}

/** As the page's own: wide enough for two columns, the left one's share. */
private val TWO_COLUMNS = 1040.dp
private const val LEFT_SHARE = 11f / 18f
private const val BUTTON_SHARE = 0.72f
private const val FIGURES = 3
private const val CONTINUE_ROWS = 3

private val SMALL_LINE = 12.dp
private val LINE = 18.dp
private val HEADING = 24.dp
private val TITLE = 32.dp
private val GREETING_HEIGHT = 30.dp
private val BUTTON = 52.dp
private val BAR = 6.dp
private val CHIP = 34.dp
private val FIGURE_HEIGHT = 46.dp
private val ACTIVITY_GRID = 190.dp

private val ICON_TILE = 44.dp
private val ICON_TILE_LARGE = 64.dp
private val RING_PHONE = 56.dp
private val RING_WIDE = 64.dp
private val PICTURE_PHONE = 52.dp
private val PICTURE_WIDE = 164.dp

private val GREETING = 170.dp
private val CHEER = 340.dp
private val GOAL_TITLE = 170.dp
private val FIGURE_PHONE = 120.dp
private val FIGURE_WIDE = 150.dp
private val OVERLINE = 70.dp
private val NEXT_TITLE_PHONE = 200.dp
private val NEXT_TITLE_WIDE = 320.dp
private val MINUTES = 70.dp
private val CONTINUE_TITLE = 180.dp
private val ROW_OVER = 110.dp
private val ROW_TITLE = 210.dp
private val ROW_TITLE_PHONE = 170.dp
private val PROGRESS = 160.dp
private val VOCABULARY_TITLE = 130.dp
private val VOCABULARY_LINE = 280.dp
private val RECENT_TITLE = 160.dp
private val ACTIVITY_TITLE = 150.dp
private val WORD_CHIPS = listOf(110.dp, 90.dp, 100.dp, 85.dp, 80.dp, 75.dp, 90.dp, 70.dp)
