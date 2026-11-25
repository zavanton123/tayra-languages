package com.idiomiko.feature.courses

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.courses.Course
import com.idiomiko.core.domain.courses.CourseLevel
import com.idiomiko.core.domain.courses.CourseProgress
import com.idiomiko.core.domain.courses.CourseTheme
import com.idiomiko.core.domain.courses.LessonStatus
import com.idiomiko.core.domain.courses.LevelProgress
import com.idiomiko.core.domain.playlists.PlaylistSource
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.IconTile
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.OverflowMenu
import com.idiomiko.core.ui.components.ProgressTrack
import com.idiomiko.core.ui.components.Tag
import com.idiomiko.core.ui.components.TagSize
import com.idiomiko.core.ui.components.TagStyle
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.components.pressScale
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import com.idiomiko.core.ui.theme.tint

/** The picture of what a course is about. */
internal val CourseTheme.icon: ImageVector
    get() = when (this) {
        CourseTheme.TRANSPORT -> TopicIcons.DirectionsBus
        CourseTheme.HOME -> TopicIcons.House
        CourseTheme.FAMILY -> TopicIcons.FamilyRestroom
        CourseTheme.FOOD -> TopicIcons.Restaurant
        CourseTheme.SHOPPING -> Icons.Default.ShoppingCart
        CourseTheme.CLOTHES -> TopicIcons.Checkroom
        CourseTheme.MONEY -> TopicIcons.Payments
        CourseTheme.WORK -> TopicIcons.Work
        CourseTheme.SCHOOL -> AppIcons.School
        CourseTheme.LEISURE -> TopicIcons.BeachAccess
        CourseTheme.SPORT -> TopicIcons.SportsSoccer
        CourseTheme.HEALTH -> TopicIcons.LocalHospital
        CourseTheme.SAFETY -> TopicIcons.Shield
        CourseTheme.TRAVEL -> TopicIcons.Flight
        CourseTheme.MAPS -> TopicIcons.Map
        CourseTheme.CITY -> TopicIcons.LocationCity
        CourseTheme.MIND -> TopicIcons.Psychology
        CourseTheme.ART -> AppIcons.Palette
        CourseTheme.MUSIC -> TopicIcons.MusicNote
        CourseTheme.FILM -> AppIcons.Movie
        CourseTheme.THEATRE -> TopicIcons.TheaterComedy
        CourseTheme.BOOKS -> TopicIcons.AutoStories
        CourseTheme.LANGUAGE -> AppIcons.Translate
        CourseTheme.NEWS -> AppIcons.Newspaper
        CourseTheme.CELEBRATION -> TopicIcons.Celebration
        CourseTheme.HISTORY -> TopicIcons.Museum
        CourseTheme.ANIMALS -> TopicIcons.Pets
        CourseTheme.NATURE -> TopicIcons.Eco
        CourseTheme.SEA -> TopicIcons.Sailing
        CourseTheme.WEATHER -> TopicIcons.WbSunny
        CourseTheme.SPACE -> AppIcons.RocketLaunch
        CourseTheme.TECHNOLOGY -> TopicIcons.Devices
        CourseTheme.SCIENCE -> TopicIcons.Science
        CourseTheme.BUILDING -> TopicIcons.Construction
        CourseTheme.LAW -> TopicIcons.Gavel
        CourseTheme.SOCIETY -> TopicIcons.Groups
        CourseTheme.TIME -> AppIcons.Schedule
        CourseTheme.DAILY_LIFE -> TopicIcons.LocalCafe
        CourseTheme.GENERAL -> AppIcons.MenuBook
    }

/** The colour of a course's picture: warm for home and food, green for nature, blue for places, and so on. */
internal val CourseTheme.tint: Color
    get() = when (this) {
        CourseTheme.HOME, CourseTheme.FOOD, CourseTheme.BUILDING, CourseTheme.CELEBRATION -> AppAccents.orange
        CourseTheme.FAMILY, CourseTheme.CLOTHES, CourseTheme.ART -> AppAccents.pink
        CourseTheme.SPORT, CourseTheme.MONEY, CourseTheme.NATURE -> AppAccents.green
        CourseTheme.HEALTH, CourseTheme.SAFETY, CourseTheme.FILM -> AppAccents.red
        CourseTheme.TRAVEL, CourseTheme.CITY, CourseTheme.SCHOOL, CourseTheme.LANGUAGE, CourseTheme.TECHNOLOGY, CourseTheme.SEA -> AppAccents.blue
        CourseTheme.TRANSPORT, CourseTheme.MAPS, CourseTheme.SCIENCE, CourseTheme.SHOPPING, CourseTheme.TIME -> AppAccents.teal
        CourseTheme.MIND, CourseTheme.MUSIC, CourseTheme.THEATRE, CourseTheme.SPACE, CourseTheme.SOCIETY -> AppAccents.purple
        CourseTheme.WORK, CourseTheme.BOOKS, CourseTheme.LAW, CourseTheme.HISTORY, CourseTheme.DAILY_LIFE -> AppAccents.brown
        CourseTheme.WEATHER, CourseTheme.ANIMALS, CourseTheme.LEISURE -> AppAccents.amber
        CourseTheme.NEWS, CourseTheme.GENERAL -> AppAccents.gray
    }

/** The course's theme, worked out once for each topic and title. */
@Composable
internal fun rememberTheme(course: Course): CourseTheme = remember(course.topic, course.title) { CourseTheme.of(course) }

/**
 * The playlist menu for a course tile's "Add to playlist", open while asked for and placed by the tile's ⋮ button.
 * The screens provide it; their content alone has none, and the tile then offers no playlist.
 */
internal val LocalPlaylistPopup = staticCompositionLocalOf<(@Composable (source: PlaylistSource, open: Boolean, onDismiss: () -> Unit) -> Unit)?> { null }

/** What marks a tile apart from its neighbours: the course being read, or the one to read next. */
internal enum class TilePin { IN_PROGRESS, UP_NEXT }

/** What a course tile or row can do besides opening its course. */
internal class TileActions(val onOpen: (courseId: String) -> Unit, val onEdit: ((courseId: String) -> Unit)? = null)

/**
 * A course as a tile: its topic's picture, title and topic, what sets it apart, and how far the reader has got or,
 * before it is started, how much of it they know already. The whole tile opens the course; its ⋮ adds it to a playlist.
 */
@Composable
internal fun CourseTile(progress: CourseProgress, knownShare: Int?, pin: TilePin?, actions: TileActions, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    val theme = rememberTheme(course)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        modifier.pressScale(interaction).clip(AppShapes.large).background(colors.surface)
            .border(AppBorders.hairline(if (hovered) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), AppShapes.large)
            .hoverable(interaction).clickable(interaction, LocalIndication.current, onClick = { actions.onOpen(course.id) })
            .padding(start = AppSpacing.l, end = AppSpacing.s, top = AppSpacing.l, bottom = AppSpacing.s).testTag("course-${course.id}"),
    ) {
        Row(Modifier.padding(end = AppSpacing.s), verticalAlignment = Alignment.Top) {
            IconTile(theme.icon, size = TOPIC_TILE, tint = theme.tint)
            Spacer(Modifier.weight(1f))
            pin?.let { PinTag(it) }
        }
        Spacer(Modifier.height(AppSpacing.s))
        Text(course.title.inLearnedScript(), Modifier.padding(end = AppSpacing.s), style = AppText.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        course.cardSubtitle.takeIf { it.isNotBlank() }?.let {
            Text(it, Modifier.padding(top = AppSpacing.xxs, end = AppSpacing.s), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        // Tiles in a row share the tallest one's height; how far the course has got keeps to the bottom.
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(AppSpacing.xs))
        Row(Modifier.fillMaxWidth().height(TILE_FOOT), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { TileStatus(progress, knownShare) }
            TileMenu(course, actions)
        }
    }
}

@Composable
private fun PinTag(pin: TilePin) = when (pin) {
    TilePin.IN_PROGRESS -> Tag(tr("In progress").uppercase(), AppAccents.orange, Modifier.testTag("pin-in-progress"), size = TagSize.Small, shape = AppShapes.small)
    TilePin.UP_NEXT -> Tag(tr("Next up").uppercase(), AppAccents.blue, Modifier.testTag("pin-up-next"), size = TagSize.Small, shape = AppShapes.small)
}

/** A started course's bar of lessons read, a finished one's tick, or else the share of its words known. */
@Composable
private fun TileStatus(progress: CourseProgress, knownShare: Int?) {
    val colors = MaterialTheme.colorScheme
    when {
        progress.lessons.isEmpty() -> Text(tr("No lessons yet"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        progress.status == LessonStatus.COMPLETED -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AppAccents.green, modifier = Modifier.size(AppSizes.iconSmall))
            Text(tr("Completed"), style = AppText.labelStrong, color = AppAccents.green)
        }
        progress.status == LessonStatus.IN_PROGRESS -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            // At least a sliver, so a course begun never looks untouched.
            ProgressTrack(progress.fraction.coerceAtLeast(MIN_BAR), AppAccents.orange, Modifier.weight(1f), height = AppSizes.track)
            Text("${progress.completed}/${progress.lessons.size}", style = AppText.label, color = colors.onSurfaceVariant, softWrap = false)
        }
        knownShare != null && knownShare > 0 -> Text(tr("{0}% known", knownShare), Modifier.testTag("known-share"), style = AppText.labelStrong, color = if (knownShare >= COMFORTABLE_SHARE) AppAccents.green else colors.onSurfaceVariant)
        else -> Text(lessonCount(progress.lessons.size), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}

/** The tile's ⋮: adding the course to a playlist, and editing it when it is the reader's own; none when it offers nothing. */
@Composable
private fun TileMenu(course: Course, actions: TileActions) {
    val popup = LocalPlaylistPopup.current?.takeIf { course.lessons.isNotEmpty() }
    val edit = actions.onEdit?.takeIf { !course.builtIn }
    if (popup == null && edit == null) return
    var picking by remember { mutableStateOf(false) }
    // The playlist menu is made the first time it is asked for, not for every tile, and kept for the dialog it may open.
    var asked by remember { mutableStateOf(false) }
    Box {
        OverflowMenu(tr("Course actions"), buttonModifier = Modifier.testTag("course-menu-${course.id}")) { close ->
            if (popup != null) AppMenuItem(text = { Text(tr("Add to playlist")) }, onClick = { close(); asked = true; picking = true }, modifier = Modifier.testTag("menu-add-to-playlist"))
            if (edit != null) AppMenuItem(text = { Text(tr("Edit course")) }, onClick = { close(); edit(course.id) })
        }
        if (asked) popup?.invoke(PlaylistSource.WholeCourse(course.id), picking) { picking = false }
    }
}

/** Courses as tiles in [columns], the tiles of a row as tall as each other. */
@Composable
internal fun CourseGrid(list: List<CourseProgress>, columns: Int, knownShares: Map<String, Int>, pinOf: (CourseProgress) -> TilePin?, actions: TileActions) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        list.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                row.forEach { CourseTile(it, knownShares[it.course.id], pinOf(it), actions, Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Courses as rows of a list: picture, title and topic, lessons, and progress or the share known. */
@Composable
internal fun CourseList(list: List<CourseProgress>, knownShares: Map<String, Int>, pinOf: (CourseProgress) -> TilePin?, actions: TileActions) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) { list.forEach { CourseRow(it, knownShares[it.course.id], pinOf(it), actions) } }
}

@Composable
private fun CourseRow(progress: CourseProgress, knownShare: Int?, pin: TilePin?, actions: TileActions) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    val theme = rememberTheme(course)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth().pressScale(interaction).clip(AppShapes.card).background(colors.surface)
            .border(AppBorders.hairline(if (hovered) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), AppShapes.card)
            .hoverable(interaction).clickable(interaction, LocalIndication.current, onClick = { actions.onOpen(course.id) })
            .padding(horizontal = AppSpacing.l, vertical = AppSpacing.m).testTag("course-${course.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        IconTile(theme.icon, size = ROW_TILE, tint = theme.tint)
        Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                Text(course.title.inLearnedScript(), Modifier.weight(1f, fill = false), style = AppText.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                pin?.let { PinTag(it) }
            }
            Text(course.cardSubtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(courseFacts(course), Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.weight(0.8f)) { TileStatus(progress, knownShare) }
        TileMenu(course, actions)
    }
}

/**
 * A level's heading: its code on its colour, its name, and [detail], such as why it is picked, on the right or, where
 * room is short, under the name; then [trailing], such as the toggle that opens it.
 */
@Composable
internal fun LevelHeading(level: CourseLevel, detail: String?, modifier: Modifier = Modifier, compact: Boolean = LocalWindowWidth.current.isCompact, trailing: @Composable () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().testTag("level-header-${level.name}"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Tag(level.code, level.tint, style = TagStyle.Solid, shape = AppShapes.control, size = TagSize.Large, textColor = AppAccents.onAccent)
        Column(Modifier.weight(1f)) {
            Text(tr(level.label), style = AppText.sectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (compact && detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!compact && detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
        trailing()
    }
}

/** A level's heading with [detail], by default how many courses it has, and a toggle that shows or hides them. */
@Composable
internal fun FoldedLevelHeading(level: LevelProgress, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, detail: String? = null) {
    val colors = MaterialTheme.colorScheme
    LevelHeading(
        level.level,
        detail ?: if (level.completed > 0) trPlural(level.courses, "{1} of {0} course", "{1} of {0} courses", level.completed) else trPlural(level.courses, "{0} course", "{0} courses"),
        modifier.clip(AppShapes.control).clickable(onClick = onToggle).padding(vertical = AppSpacing.m),
    ) {
        Row(Modifier.testTag("level-toggle-${level.level.name}"), verticalAlignment = Alignment.CenterVertically) {
            Text(if (expanded) tr("Hide") else tr("Show"), style = AppText.labelStrong, color = colors.primary)
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.icon))
        }
    }
}

/**
 * The levels finished, folded into one line: which they are, the courses and lessons read in them, and a way to
 * look back at them.
 */
@Composable
internal fun FinishedLevelsBar(levels: List<LevelProgress>, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val codes = levels.map { it.level.code }
    val title = if (codes.size == 1) tr("{0} complete", codes.single()) else tr("{0} and {1} complete", codes.dropLast(1).joinToString(", "), codes.last())
    val courses = levels.sumOf { it.courses }
    val lessons = levels.sumOf { it.lessonsRead }
    val detail = trPlural(courses, "{0} course", "{0} courses") + ", " + trPlural(lessons, "{1} lesson read", "{1} lessons read", formatCount(lessons))
    val green = AppAccents.onWash(AppAccents.green)
    Row(
        modifier.fillMaxWidth().clip(AppShapes.card).background(AppAccents.green.copy(alpha = AppAlpha.tint)).clickable(onClick = onToggle)
            .padding(horizontal = AppSpacing.l, vertical = AppSpacing.m).testTag("finished-levels"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Icon(TopicIcons.MilitaryTech, contentDescription = null, tint = AppAccents.amber, modifier = Modifier.size(AppSizes.iconLarge))
        Text(
            buildAnnotatedString {
                withStyle(AppText.labelStrong.toSpanStyle().merge(SpanStyle(color = green))) { append(title) }
                append(" · ")
                append(detail)
            },
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
        )
        LinkText(if (expanded) tr("Hide") else tr("Review them"), onToggle, style = MaterialTheme.typography.bodyMedium)
    }
}

private const val TOPIC_TILE = 44
private const val ROW_TILE = 44
private val TILE_FOOT = 36.dp

/** A known share from which a course reads comfortably, shown in green. */
private const val COMFORTABLE_SHARE = 85

/** The least of the bar a started course shows. */
private const val MIN_BAR = 0.04f
