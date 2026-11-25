package com.idiomiko.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.home.NextUp
import com.idiomiko.core.domain.home.knownPercent
import com.idiomiko.core.domain.stats.SavedWord
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppTooltip
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.CardHeader
import com.idiomiko.core.ui.components.CardHeaderSize
import com.idiomiko.core.ui.components.IconTile
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.ProgressTrack
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.StudyActivityCard
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

/** A card of the page's progress side: a small header with an optional link at its end, and its content. */
@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    tint: Color,
    tag: String,
    link: Pair<String, () -> Unit>? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val compact = LocalWindowWidth.current.isCompact
    Column(
        modifier.fillMaxWidth().cardSurface(AppShapes.card).padding(if (compact) AppSpacing.l else AppSpacing.xl).testTag(tag),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        CardHeader(title, icon = icon, tint = tint, size = CardHeaderSize.Dense, actions = {
            link?.let { (text, onClick) -> LinkText(text, onClick = onClick, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("$tag-link")) }
        })
        content()
    }
}

/**
 * The words saved last, as chips; one opens in the sentence it was saved in, which its flashcard keeps, or as
 * the word alone when it was saved without one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecentWordsCard(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    if (state.recentWords.isEmpty()) return
    SectionCard(tr("Recently saved"), AppIcons.Abc, AppAccents.orange, "recent-words", link = tr("Vocabulary") to { actions.onNavigate(Route.Terms()) }, modifier = modifier) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            state.recentWords.forEach { word -> WordChip(word) { actions.onNavigate(if (word.sentence != null) Route.BrowseFlashcards(word.termId) else Route.EditTerm(word.termId)) } }
        }
    }
}

@Composable
private fun WordChip(word: SavedWord, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val chip = @Composable {
        Text(
            word.text.inLearnedScript(),
            Modifier.widthIn(max = CHIP_MAX_WIDTH).clip(AppShapes.pill).background(AppAccents.orange.copy(alpha = AppAlpha.tint)).border(AppBorders.hairline(AppAccents.orange.copy(alpha = AppAlpha.line)), AppShapes.pill)
                .clickable(onClick = onClick).padding(horizontal = AppSpacing.m, vertical = AppSpacing.s).testTag("recent-word-${word.termId}"),
            style = AppText.label,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    // The translation shows on hover, where there is one.
    val translation = word.translation
    if (translation != null) AppTooltip(translation) { chip() } else chip()
}

/**
 * How far the words known go through the frequency list: "76% of the 2,000 most common words", towards the
 * next milestone. The count of words known is in the day's strip above, so it is not said twice.
 */
@Composable
internal fun VocabularyCard(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val words = state.vocabulary ?: return
    val milestone = words.milestone ?: return
    val colors = MaterialTheme.colorScheme
    SectionCard(tr("Vocabulary"), Icons.Default.Check, AppAccents.green, "vocabulary", link = tr("Word frequency") to { actions.onNavigate(Route.WordFrequency) }, modifier = modifier) {
        Text(tr("{0}% of the {1} most common words", (words.share * 100).toInt(), formatCount(milestone)), style = AppText.itemTitle)
        ProgressTrack(words.share, AppAccents.green, Modifier.fillMaxWidth(), height = AppSizes.trackThick)
        if (words.known < milestone) Text(tr("Next milestone: {0}", formatCount(milestone)), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

/**
 * The vocabulary as one row on a phone, in place of the wide window's card of the most common words: the count of
 * words known is on the day's card above, and the row leads to the words themselves.
 */
@Composable
internal fun VocabularyRow(actions: HomeActions, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().cardSurface(AppShapes.card).clickable { actions.onNavigate(Route.Terms()) }.padding(AppSpacing.l).testTag("vocabulary-row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        IconTile(AppIcons.Book, size = 48, tint = AppAccents.green)
        Column(Modifier.weight(1f)) {
            Text(tr("Vocabulary"), style = AppText.sectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tr("Build and review your word knowledge"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconMedium))
    }
}

/** What [RecommendedCard] offers: a title, what makes it fit, where it leads and where the button browses for more. */
private class Recommendation(val title: String, val detail: String, val tag: String, val open: Route, val browse: String, val browseTo: Route)

/**
 * One thing that suits the reader's level: the course whose words go just past those known, or failing that a
 * book not opened yet whose words the reader mostly knows. A course already offered as the next step is not offered twice.
 */
@Composable
private fun recommendation(state: HomeUiState): Recommendation? {
    val next = state.nextUp
    val course = state.suggestedCourse?.takeIf { !(next is NextUp.Lesson && next.course.course.id == it.course.id) }
    if (course != null) {
        val rank = course.course.rankUpTo
        return Recommendation(
            course.course.title.inLearnedScript(),
            listOfNotNull(course.course.level.code, rank?.let { tr("Words up to rank {0}", formatCount(it)) }).joinToString(" · "),
            "suggested-course", Route.Course(course.course.id), tr("Browse courses"), Route.Courses,
        )
    }
    val book = state.suggestedBooks.firstOrNull() ?: return null
    return Recommendation(
        book.title.inLearnedScript(),
        listOfNotNull(book.knownPercent?.let { tr("{0}% known", it) }, pageLine(book)).joinToString(" · "),
        "suggested-book-${book.id}", Route.Read(book.id), tr("Browse books"), Route.Books,
    )
}

/** The recommendation for the reader's level, which opens on a click, with a way to browse for others beside it. */
@Composable
internal fun RecommendedCard(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val pick = recommendation(state) ?: return
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    val browse = @Composable { m: Modifier ->
        SecondaryButton(pick.browse, onClick = { actions.onNavigate(pick.browseTo) }, modifier = m.testTag("suggestions-browse"), size = ButtonSize.Large, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward)
    }
    val about = @Composable { m: Modifier ->
        Row(
            m.clip(AppShapes.control).clickable { actions.onNavigate(pick.open) }.testTag(pick.tag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            IconTile(AppIcons.RocketLaunch, size = if (compact) 48 else 72, tint = AppAccents.purple)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(tr("Recommended for you").uppercase(), style = AppText.overline, color = AppAccents.onWash(AppAccents.purple))
                Text(pick.title, style = AppText.sectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pick.detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    val card = modifier.testTag("suggestions").fillMaxWidth().cardSurface(AppShapes.card).padding(if (compact) AppSpacing.l else AppSpacing.xl)
    // A phone has no room for the button beside the words: it goes under them.
    if (compact) {
        Column(card, verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            about(Modifier.fillMaxWidth())
            browse(Modifier.fillMaxWidth())
        }
    } else {
        Row(card, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            about(Modifier.weight(1f))
            browse(Modifier)
        }
    }
}

/** The last 16 weeks of study; the whole year is on Statistics. */
@Composable
internal fun RecentActivityCard(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val activity = state.activity ?: return
    val today = activity.today
    val start = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY).minus((RECENT_WEEKS - 1) * 7, DateTimeUnit.DAY)
    StudyActivityCard(
        activity,
        range = start..today,
        action = { LinkText(tr("See all"), onClick = { actions.onNavigate(Route.Stats) }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("see-all-stats")) },
        modifier = modifier.testTag("activity"),
    )
}

private const val RECENT_WEEKS = 16
private val CHIP_MAX_WIDTH = 220.dp
