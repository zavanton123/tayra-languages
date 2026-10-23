package com.tayra.languages.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.Lesson
import com.tayra.languages.core.domain.playlists.PlaylistService
import com.tayra.languages.core.domain.playlists.PlaylistSource
import com.tayra.languages.core.domain.playlists.PlaylistText
import com.tayra.languages.core.domain.playlists.PlaylistTextKind
import com.tayra.languages.core.ui.components.AppCheckbox
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.AppVerticalDivider
import com.tayra.languages.core.ui.components.BarSurface
import com.tayra.languages.core.ui.components.Crumb
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LabeledField
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SelectableChip
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlin.random.Random
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Making a playlist: its name, description and cover beside the texts to put in it; changing its
 * details; or choosing texts to add to it, by [mode].
 */
@Composable
fun PlaylistFormScreen(
    mode: PlaylistFormMode,
    onNavigate: (Route) -> Unit,
    onSaved: (playlistId: Long) -> Unit,
    onCancel: () -> Unit,
    viewModel: PlaylistFormViewModel = koinViewModel(key = "playlist-form-$mode") { parametersOf(mode) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event -> if (event is PlaylistFormEvent.Saved) onSaved(event.playlistId) }
    val compact = LocalWindowWidth.current.isCompact
    Scaffold(
        topBar = { AppTopBar(title = mode.title, onNavigate = onNavigate, section = NavSection.PLAYLISTS, onBack = onCancel) },
        bottomBar = { if (compact && !state.loading) BarSurface { Column { AppDivider(); FormFooter(state, onCancel, viewModel::save, Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m)) } } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        PlaylistFormContent(state, viewModel, onCancel, Modifier.padding(padding))
    }
}

/**
 * Making a playlist in a dialog over the screen it is made from: its name, description and cover.
 * With [bookId] the open text goes into it, and with [source] that lesson or course does.
 * [onCreated] is told the new playlist's id and name.
 */
@Composable
fun NewPlaylistDialog(bookId: Long?, onCreated: (playlistId: Long, name: String) -> Unit, onDismiss: () -> Unit, source: PlaylistSource? = null) {
    // Each opening starts from an empty form, not from the last one's.
    val opening = remember { Random.nextLong() }
    val viewModel: PlaylistFormViewModel = koinViewModel(key = "new-playlist-$bookId-$opening") { parametersOf(PlaylistFormMode.Create(bookId, source)) }
    NewPlaylistDialog(viewModel, onCreated, onDismiss)
}

@Composable
internal fun NewPlaylistDialog(viewModel: PlaylistFormViewModel, onCreated: (playlistId: Long, name: String) -> Unit, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event -> if (event is PlaylistFormEvent.Saved) onCreated(event.playlistId, state.name.trim()) }
    AppDialogSurface(onDismissRequest = onDismiss, maxWidth = AppSizes.dialog) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(AppSpacing.xxl).testTag("new-playlist-dialog"),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(tr("Create playlist"), Modifier.weight(1f), style = AppText.headline)
                AppIconButton(Icons.Default.Close, contentDescription = tr("Close"), onClick = onDismiss, size = IconButtonSize.Small)
            }
            if (state.loading) {
                LoadingIndicator()
            } else {
                state.error?.let { InfoBanner(it, tint = MaterialTheme.colorScheme.error, icon = Icons.Default.Warning) }
                DetailsColumn(state, viewModel, Modifier.fillMaxWidth(), heading = false)
                AppDivider(Modifier.padding(vertical = AppSpacing.xs))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m, Alignment.End)) {
                    SecondaryButton(tr("Cancel"), onClick = onDismiss)
                    PrimaryButton(tr("Create playlist"), onClick = viewModel::save, enabled = state.canSave, loading = state.saving, modifier = Modifier.testTag("form-save"))
                }
            }
        }
    }
}

private val PlaylistFormMode.title: String
    get() = when (this) {
        is PlaylistFormMode.Create -> tr("Create playlist")
        is PlaylistFormMode.Edit -> tr("Edit playlist")
        is PlaylistFormMode.AddTexts -> tr("Add texts")
    }

@Composable
internal fun PlaylistFormContent(state: PlaylistFormUiState, viewModel: PlaylistFormViewModel, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    ScrollColumn(
        modifier.fillMaxSize(),
        contentModifier = Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
    ) {
        ScreenHeader(
            state.mode.title,
            when (state.mode) {
                is PlaylistFormMode.Create -> tr("Collect texts to listen to one after another.")
                is PlaylistFormMode.Edit -> tr("Change what the playlist is called, what it is for and its cover.")
                is PlaylistFormMode.AddTexts -> tr("Choose books, courses and lessons to add after the playlist's texts.")
            },
            crumbs = if (compact) emptyList() else listOf(Crumb(tr("Playlists"), onCancel)),
        )
        state.error?.let { InfoBanner(it, tint = MaterialTheme.colorScheme.error, icon = Icons.Default.Warning) }
        val details: @Composable (Modifier) -> Unit = { m -> DetailsColumn(state, viewModel, m) }
        val texts: @Composable (Modifier) -> Unit = { m -> TextsColumn(state, viewModel, m) }
        if (compact) {
            if (state.showsDetails) Box(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.l)) { details(Modifier.fillMaxWidth()) }
            if (state.showsTexts) Box(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.l)) { texts(Modifier.fillMaxWidth()) }
        } else {
            val narrow = !state.showsDetails || !state.showsTexts
            Column(Modifier.then(if (narrow) Modifier.widthIn(max = AppSizes.readableWidth) else Modifier).fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.xxl)) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl)) {
                    if (state.showsDetails) details(Modifier.weight(1f))
                    if (state.showsDetails && state.showsTexts) AppVerticalDivider(Modifier.fillMaxHeight())
                    if (state.showsTexts) texts(Modifier.weight(1.15f))
                }
                AppDivider(Modifier.padding(vertical = AppSpacing.xl))
                FormFooter(state, onCancel, viewModel::save)
            }
        }
    }
}

/** The hint and the form's buttons. */
@Composable
private fun FormFooter(state: PlaylistFormUiState, onCancel: () -> Unit, onSave: () -> Unit, modifier: Modifier = Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        if (!compact) {
            Text(
                if (state.mode is PlaylistFormMode.Edit) tr("The texts are changed on the playlist's page.") else tr("You can add, remove and reorder texts later."),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SecondaryButton(tr("Cancel"), onClick = onCancel, modifier = if (compact) Modifier.weight(1f) else Modifier)
        PrimaryButton(
            when (state.mode) {
                is PlaylistFormMode.Create -> tr("Create playlist")
                is PlaylistFormMode.Edit -> tr("Save changes")
                is PlaylistFormMode.AddTexts -> if (state.selectedCount > 0) trPlural(state.selectedCount, "Add {0} text", "Add {0} texts") else tr("Add texts")
            },
            onClick = onSave,
            enabled = state.canSave,
            loading = state.saving,
            modifier = (if (compact) Modifier.weight(1f) else Modifier).testTag("form-save"),
        )
    }
}

/** The playlist's name, description and cover colour, with how its card will look. */
@Composable
private fun DetailsColumn(state: PlaylistFormUiState, viewModel: PlaylistFormViewModel, modifier: Modifier, heading: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        if (heading) Text(tr("Playlist details"), style = AppText.sectionTitle)
        LabeledField(tr("Name")) {
            AppTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                placeholder = { Text(tr("For example: Morning practice")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("playlist-name"),
            )
            Text(
                "${state.name.length} / ${PlaylistService.NAME_MAX_LENGTH}",
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
        LabeledField(tr("Description (optional)")) {
            AppTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                placeholder = { Text(tr("What the playlist is for")) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("playlist-description"),
            )
        }
        LabeledField(tr("Cover colour")) {
            val compact = LocalWindowWidth.current.isCompact
            Row(horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.s else AppSpacing.m)) {
                coverColors.forEachIndexed { index, color ->
                    val chosen = index == state.cover
                    val name = tr("Cover colour {0}", index + 1)
                    Box(
                        // A phone shares its width out among the colours.
                        (if (compact) Modifier.weight(1f).height(SWATCH_HEIGHT) else Modifier.size(SWATCH_WIDTH, SWATCH_HEIGHT)).clip(AppShapes.control).background(color.copy(alpha = AppAlpha.line))
                            .then(if (chosen) Modifier.border(AppBorders.selected(color), AppShapes.control) else Modifier)
                            .clickable { viewModel.setCover(index) }
                            .semantics {
                                contentDescription = name
                                selected = chosen
                            }
                            .testTag("cover-$index"),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (chosen) {
                            Box(Modifier.size(AppSizes.iconLarge).clip(AppShapes.circle).background(color), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AppAccents.onAccent, modifier = Modifier.size(AppSizes.iconSmall))
                            }
                        }
                    }
                }
            }
        }
        // How the playlist's card will read.
        val tint = coverColor(state.cover)
        Row(
            Modifier.fillMaxWidth().clip(AppShapes.card).background(tint.copy(alpha = AppAlpha.wash)).padding(AppSpacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            CoverTile(state.cover, PREVIEW_TILE)
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(state.name.ifBlank { tr("New playlist") }, style = AppText.sectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (state.showsTexts) {
                    Text(
                        if (state.selectedCount == 0) textCount(0) else "${textCount(state.selectedCount)} · ${listeningTime(viewModel.duration(state.selectedLength))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The library's texts to tick: books and news articles, and courses that open to show their lessons. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextsColumn(state: PlaylistFormUiState, viewModel: PlaylistFormViewModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        if (state.showsDetails) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(tr("Add texts"), style = AppText.sectionTitle)
                Text(tr("Choose now, or create an empty playlist."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
        }
        SearchField(state.search, viewModel::setSearch, tr("Search books, courses and lessons"), Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            TextFilter.entries.forEach { filter ->
                SelectableChip(filter.label, selected = state.filter == filter, onClick = { viewModel.setFilter(filter) }, modifier = Modifier.testTag("filter-${filter.name}"))
            }
        }
        val texts = state.texts
        val courses = state.courses
        Column(Modifier.fillMaxWidth().clip(AppShapes.card).border(AppBorders.hairline(), AppShapes.card)) {
            if (texts.isEmpty() && courses.isEmpty()) {
                Text(
                    if (state.library.texts.isEmpty() && state.library.courses.isEmpty()) tr("The library has no texts in this language yet.") else tr("Nothing matches the search."),
                    Modifier.padding(AppSpacing.l),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            var first = true
            texts.forEach { text ->
                if (!first) AppDivider()
                first = false
                TextRow(text, PlaylistSource.Text(text.bookId) in state.selected, listeningTime(viewModel.duration(text.length))) { viewModel.toggle(PlaylistSource.Text(text.bookId)) }
            }
            courses.forEach { course ->
                if (!first) AppDivider()
                first = false
                CourseRow(state, course, viewModel)
            }
        }
        Text(
            if (state.selectedCount == 0) tr("Nothing selected") else "${tr("{0} selected", state.selectedCount)} · ${listeningTime(viewModel.duration(state.selectedLength))}",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

internal val TextFilter.label: String
    get() = when (this) {
        TextFilter.ALL -> tr("All")
        TextFilter.BOOKS -> tr("Books")
        TextFilter.COURSES -> tr("Courses")
        TextFilter.NEWS -> tr("News")
    }

@Composable
private fun TextRow(text: PlaylistText, selected: Boolean, time: String, onToggle: () -> Unit) {
    PickRow(
        title = text.title,
        detail = "${text.kind.label} · $time",
        icon = text.kind.icon,
        selected = selected,
        onToggle = onToggle,
        modifier = Modifier.testTag("pick-text-${text.bookId}"),
    )
}

/** A whole course to tick, with its lessons below it once opened. */
@Composable
private fun CourseRow(state: PlaylistFormUiState, course: Course, viewModel: PlaylistFormViewModel) {
    val whole = state.isSelected(course)
    val open = state.showsLessons(course)
    PickRow(
        title = course.title,
        detail = "${tr("Course")} · ${trPlural(course.lessons.size, "{0} lesson", "{0} lessons")} · ${listeningTime(viewModel.duration(course.lessons.sumOf { it.text.length }))}",
        icon = AppIcons.School,
        selected = whole,
        onToggle = { viewModel.toggle(PlaylistSource.WholeCourse(course.id)) },
        modifier = Modifier.testTag("pick-course-${course.id}"),
        trailing = {
            val description = if (open) tr("Hide lessons") else tr("Show lessons")
            Icon(
                if (open) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(AppShapes.small).clickable { viewModel.toggleExpanded(course.id) }.padding(AppSpacing.xs).testTag("expand-${course.id}"),
            )
        },
    )
    if (open) {
        state.lessonsOf(course).forEach { lesson ->
            AppDivider(Modifier.padding(start = AppSpacing.giant))
            LessonRow(course, lesson, whole || PlaylistSource.Lesson(course.id, lesson.id) in state.selected, enabled = !whole, time = listeningTime(viewModel.duration(lesson.text.length))) {
                viewModel.toggle(PlaylistSource.Lesson(course.id, lesson.id))
            }
        }
    }
}

@Composable
private fun LessonRow(course: Course, lesson: Lesson, selected: Boolean, enabled: Boolean, time: String, onToggle: () -> Unit) {
    PickRow(
        title = lesson.title,
        detail = "${PlaylistTextKind.LESSON.label} · $time",
        icon = PlaylistTextKind.LESSON.icon,
        selected = selected,
        enabled = enabled,
        onToggle = onToggle,
        modifier = Modifier.padding(start = AppSpacing.xxxl).testTag("pick-lesson-${course.id}-${lesson.id}"),
    )
}

@Composable
private fun PickRow(
    title: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    Row(
        modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onToggle).padding(horizontal = AppSpacing.m, vertical = AppSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        AppCheckbox(selected, onCheckedChange = null, enabled = enabled)
        IconTile(icon, size = TILE_SIZE)
        if (compact) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
        }
        trailing?.invoke() ?: Spacer(Modifier.width(AppSpacing.xxs))
    }
}

private const val TILE_SIZE = 40
private val SWATCH_WIDTH = 64.dp
private val SWATCH_HEIGHT = 56.dp
private val PREVIEW_TILE = 72.dp
