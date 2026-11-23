package com.idiomiko.feature.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.news.NewsSource
import com.idiomiko.core.domain.news.NewsTopic
import com.idiomiko.core.domain.news.NewsTopics
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppSpinner
import com.idiomiko.core.ui.components.CoverageCheck
import com.idiomiko.core.ui.components.EmptyStateIllustration
import com.idiomiko.core.ui.components.JourneyIllustration
import com.idiomiko.core.ui.components.PlaceholderLine
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SpinnerSize
import com.idiomiko.core.ui.components.WebPicture
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.color
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.components.label
import com.idiomiko.core.ui.components.placeholder
import com.idiomiko.core.ui.components.relativeTo
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlin.time.Instant

/** What a card needs besides its article: how to name its source and topic, whether it was read, and what it does. */
internal class CardContext(
    val sources: List<NewsSource>,
    val languageCode: String?,
    val opened: Set<String>,
    val now: Instant,
    val onOpen: (NewsItem) -> Unit,
    val onSeen: (NewsItem) -> Unit,
) {
    fun isRead(item: NewsItem) = item.article.url in opened

    fun colorOf(source: NewsSource): Color = sourceColor(sources, source)

    fun topicLabel(item: NewsItem): String? = item.topics.firstOrNull()?.let { topic -> languageCode?.let { NewsTopics.label(topic, it) } }
}

/** Each source keeps its colour, by its place among the language's sources. */
internal fun sourceColor(sources: List<NewsSource>, source: NewsSource): Color = AppAccents.sequence[sources.indexOf(source).coerceAtLeast(0) % AppAccents.sequence.size]

/** An article on screen is judged, if it was not yet: the cards ask for it as they come into view. */
@Composable
private fun JudgedWhenSeen(item: NewsItem, context: CardContext) {
    val waiting = item.check == CoverageCheck.NotChecked
    LaunchedEffect(item.article.url, waiting) { if (waiting) context.onSeen(item) }
}

/**
 * The lead: the picture across the top, then the source, age and topic, the headline large, the summary, and the
 * share known with the minutes and the new words.
 */
@Composable
internal fun LeadCard(item: NewsItem, context: CardContext, pictureHeight: Dp, modifier: Modifier = Modifier, compact: Boolean = false) {
    JudgedWhenSeen(item, context)
    val colors = MaterialTheme.colorScheme
    val read = context.isRead(item)
    Column(
        modifier.cardSurface(AppShapes.large).clickable(enabled = !item.opening) { context.onOpen(item) }.alpha(if (read) READ_ALPHA else 1f).testTag("lead-${item.article.url}"),
    ) {
        ArticlePicture(item, Modifier.fillMaxWidth().height(pictureHeight), iconSize = if (compact) 48.dp else 72.dp)
        Column(Modifier.padding(horizontal = if (compact) AppSpacing.l else 22.dp, vertical = if (compact) AppSpacing.l else AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Meta(item, context, withTopic = true)
            Text(
                item.article.title.inLearnedScript(),
                style = if (compact) AppText.sectionTitle else AppText.screenTitle,
                maxLines = if (compact) 4 else 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.article.summary.isNotEmpty()) {
                Text(item.article.summary.inLearnedScript(), style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Footer(item, long = true, modifier = Modifier.padding(top = AppSpacing.xs))
            item.error?.let { ErrorLine(it) }
        }
    }
}

/** One of the four cards beside the lead: a strip of the picture, the source and age, the headline, the share known and the minutes. */
@Composable
internal fun ArticleCard(item: NewsItem, context: CardContext, modifier: Modifier = Modifier) {
    JudgedWhenSeen(item, context)
    val read = context.isRead(item)
    Column(modifier.cardSurface(AppShapes.large).clickable(enabled = !item.opening) { context.onOpen(item) }.alpha(if (read) READ_ALPHA else 1f).testTag("card-${item.article.url}")) {
        ArticlePicture(item, Modifier.fillMaxWidth().height(CARD_PICTURE), iconSize = 40.dp)
        Column(Modifier.weight(1f, fill = false).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Meta(item, context, withTopic = false)
            Text(item.article.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Footer(item, long = false)
            item.error?.let { ErrorLine(it) }
        }
    }
}

/**
 * A row of "More from today": the source, age and topic over the headline, then the share known and the way in.
 * The rows join into one card: [first] and [last] round its corners, and each row's own edge draws the line under it.
 */
@Composable
internal fun ArticleRow(item: NewsItem, context: CardContext, first: Boolean, last: Boolean) {
    JudgedWhenSeen(item, context)
    val colors = MaterialTheme.colorScheme
    val read = context.isRead(item)
    val radius = ROW_RADIUS
    val outer = RoundedCornerShape(topStart = if (first) radius else 0.dp, topEnd = if (first) radius else 0.dp, bottomStart = if (last) radius else 0.dp, bottomEnd = if (last) radius else 0.dp)
    val inner = RoundedCornerShape(topStart = if (first) radius - ROW_LINE else 0.dp, topEnd = if (first) radius - ROW_LINE else 0.dp, bottomStart = if (last) radius - ROW_LINE else 0.dp, bottomEnd = if (last) radius - ROW_LINE else 0.dp)
    Box(Modifier.fillMaxWidth().clip(outer).background(colors.outlineVariant).padding(start = ROW_LINE, end = ROW_LINE, top = if (first) ROW_LINE else 0.dp, bottom = ROW_LINE)) {
        Row(
            Modifier.fillMaxWidth().clip(inner).background(colors.surface).clickable(enabled = !item.opening) { context.onOpen(item) }
                .padding(horizontal = AppSpacing.l, vertical = AppSpacing.m).alpha(if (read) READ_ALPHA else 1f).testTag("row-${item.article.url}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Meta(item, context, withTopic = true)
                Text(item.article.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                item.error?.let { ErrorLine(it) }
            }
            KnownBadge(item)
            QuietButton(
                if (read) tr("Continue") else tr("Read"),
                onClick = { context.onOpen(item) },
                modifier = Modifier.testTag("read-${item.article.url}"),
                loading = item.opening,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            )
        }
    }
}

/** A phone's card after the lead: a square of the picture at the left, the source and age, the headline and the share known. */
@Composable
internal fun ThumbCard(item: NewsItem, context: CardContext, modifier: Modifier = Modifier) {
    JudgedWhenSeen(item, context)
    val read = context.isRead(item)
    Row(
        modifier.fillMaxWidth().cardSurface(AppShapes.large).clickable(enabled = !item.opening) { context.onOpen(item) }.alpha(if (read) READ_ALPHA else 1f)
            .padding(AppSpacing.m).testTag("card-${item.article.url}"),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        ArticlePicture(item, Modifier.size(THUMB).clip(AppShapes.panel), iconSize = 28.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Meta(item, context, withTopic = false)
            Text(item.article.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Footer(item, long = false)
            item.error?.let { ErrorLine(it) }
        }
    }
}

/** The article's picture, or while it loads and where there is none, a soft tile of its topic's colours with the topic's icon. */
@Composable
internal fun ArticlePicture(item: NewsItem, modifier: Modifier, iconSize: Dp) {
    val topic = item.topics.firstOrNull()
    WebPicture(item.picture, modifier) { TopicTile(topic, iconSize, Modifier.fillMaxSize()) }
}

/** A soft gradient in the topic's colours, its icon in the middle; toned down on a dark theme. */
@Composable
internal fun TopicTile(topic: NewsTopic?, iconSize: Dp, modifier: Modifier, icon: ImageVector = topicIcon(topic)) {
    val surface = MaterialTheme.colorScheme.surface
    val accent = topicAccent(topic)
    // A wash of the accent deepening across the tile, laid on the surface, so it is pale on a light theme and dim on a dark one.
    val start = accent.copy(alpha = AppAlpha.tint).compositeOver(surface)
    val end = accent.copy(alpha = AppAlpha.disabled).compositeOver(surface)
    Box(modifier.background(Brush.linearGradient(listOf(start, end))), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = AppAccents.onWash(accent).copy(alpha = AppAlpha.strong), modifier = Modifier.size(iconSize))
    }
}

private fun topicAccent(topic: NewsTopic?): Color = when (topic) {
    NewsTopic.HOME -> AppAccents.red
    NewsTopic.WORLD -> AppAccents.blue
    NewsTopic.POLITICS -> AppAccents.purple
    NewsTopic.ECONOMY -> AppAccents.teal
    NewsTopic.SPORT -> AppAccents.green
    NewsTopic.CULTURE -> AppAccents.orange
    NewsTopic.SCIENCE -> AppAccents.pink
    NewsTopic.LIFESTYLE -> AppAccents.amber
    null -> AppAccents.gray
}

internal fun topicIcon(topic: NewsTopic?): ImageVector = when (topic) {
    NewsTopic.HOME -> Icons.Default.Home
    NewsTopic.WORLD -> AppIcons.Globe
    NewsTopic.POLITICS -> AppIcons.AccountBalance
    NewsTopic.ECONOMY -> AppIcons.TrendingUp
    NewsTopic.SPORT -> AppIcons.Trophy
    NewsTopic.CULTURE -> AppIcons.Palette
    NewsTopic.SCIENCE -> AppIcons.Science
    NewsTopic.LIFESTYLE -> AppIcons.LocalCafe
    null -> AppIcons.Newspaper
}

/** The source with its colour, how long ago, the topic where asked, and a tick once read. */
@Composable
private fun Meta(item: NewsItem, context: CardContext, withTopic: Boolean) {
    val colors = MaterialTheme.colorScheme
    val article = item.article
    val parts = listOfNotNull(article.published?.relativeTo(context.now), context.topicLabel(item)?.takeIf { withTopic }?.inLearnedScript())
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        SourceDot(context.colorOf(article.source))
        Text(article.source.name, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (parts.isNotEmpty()) Text(parts.joinToString(" · ", prefix = "· "), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, softWrap = false)
        if (context.isRead(item)) {
            Text("·", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Icon(Icons.Default.Check, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconTiny))
            Text(tr("read"), Modifier.testTag("read-mark-${article.url}"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
internal fun SourceDot(color: Color) {
    Box(Modifier.size(AppSizes.dot).clip(RoundedCornerShape(DOT_CORNER)).background(color))
}

/** The share known, then the minutes it takes and, on the lead, the words new to the reader. */
@Composable
private fun Footer(item: NewsItem, long: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val coverage = item.coverage
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        KnownBadge(item)
        val parts = listOfNotNull(
            coverage?.minutes?.let { if (long) tr("~{0} min read", it) else tr("~{0} min", it) },
            coverage?.newWords?.takeIf { long }?.let { trPlural(it, "{0} new word", "{0} new words") },
        )
        if (parts.isNotEmpty()) Text(parts.joinToString(" · ", prefix = "· "), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (item.opening) AppSpinner(Modifier.size(AppSizes.iconSmall), size = SpinnerSize.Small)
    }
}

/**
 * The share of the article's words the reader knows, on a wash of its level's colour; the level's name is heard, not
 * shown, as the colour already tells it. "…" while the article is judged; nothing where it could not be.
 */
@Composable
internal fun KnownBadge(item: NewsItem) {
    val colors = MaterialTheme.colorScheme
    val coverage = item.coverage
    val tag = Modifier.testTag("known-${item.article.url}")
    when {
        coverage != null -> {
            val difficulty = coverage.difficulty
            val text = tr("{0}% known", coverage.percent)
            Box(
                tag.clip(AppShapes.compact).background(difficulty.color.copy(alpha = AppAlpha.soft)).padding(horizontal = AppSpacing.s, vertical = BADGE_PADDING)
                    .semantics(mergeDescendants = true) { contentDescription = "$text · ${difficulty.label}" },
            ) {
                Text(text, style = AppText.badge, color = AppAccents.onWash(difficulty.color), maxLines = 1, softWrap = false)
            }
        }
        item.check is CoverageCheck.Failed -> Unit
        else -> Box(
            tag.clip(AppShapes.compact).background(colors.onSurface.copy(alpha = AppAlpha.wash)).padding(horizontal = AppSpacing.s, vertical = BADGE_PADDING)
                .semantics(mergeDescendants = true) { contentDescription = tr("Checking the words…") },
        ) {
            Text("…", style = AppText.badge, color = colors.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun ErrorLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** The front page still to come: the lead's and the cards' shapes in the placeholder grey, then a few rows. */
@Composable
internal fun FrontSkeleton(layout: FrontLayout) {
    val loading = tr("Loading...")
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = loading }.testTag("news-skeleton"), verticalArrangement = Arrangement.spacedBy(GRID_GAP)) {
        when (layout) {
            FrontLayout.WIDE -> Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(GRID_GAP)) {
                SkeletonCard(LEAD_PICTURE, lines = 4, Modifier.weight(LEAD_WEIGHT).fillMaxHeight())
                Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(GRID_GAP)) {
                    repeat(2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(GRID_GAP)) { repeat(2) { SkeletonCard(CARD_PICTURE, lines = 2, Modifier.weight(1f)) } }
                    }
                }
            }
            FrontLayout.MEDIUM -> {
                SkeletonCard(MEDIUM_LEAD_PICTURE, lines = 3, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(GRID_GAP)) { repeat(2) { SkeletonCard(CARD_PICTURE, lines = 2, Modifier.weight(1f)) } }
            }
            FrontLayout.PHONE -> {
                SkeletonCard(PHONE_LEAD_PICTURE, lines = 3, Modifier.fillMaxWidth())
                repeat(3) {
                    Row(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.m), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                        Box(Modifier.size(THUMB).placeholder(AppShapes.panel))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) { PlaceholderLine(0.5f); PlaceholderLine(0.9f, height = AppSizes.titleLine); PlaceholderLine(0.3f) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonCard(picture: Dp, lines: Int, modifier: Modifier) {
    Column(modifier.cardSurface(AppShapes.large)) {
        Box(Modifier.fillMaxWidth().height(picture).placeholder(RectangleShape))
        Column(Modifier.padding(AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            PlaceholderLine(0.4f)
            repeat(lines) { PlaceholderLine(if (it == lines - 1) 0.6f else 0.95f, height = AppSizes.titleLine) }
            PlaceholderLine(0.3f)
        }
    }
}

/** Some sources could not be read: a quiet line naming them above the news the others gave, with a way to try again. */
@Composable
internal fun FailedSourcesNotice(failed: List<NewsSource>, onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().cardSurface(AppShapes.panel, color = lerp(colors.surface, AppAccents.orange, AppAlpha.wash)).padding(start = AppSpacing.l, end = AppSpacing.xs, top = AppSpacing.xs, bottom = AppSpacing.xs)
            .testTag("failed-sources"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Icon(AppIcons.CloudOff, contentDescription = null, tint = AppAccents.onWash(AppAccents.orange), modifier = Modifier.size(AppSizes.icon))
        Text(
            trPlural(failed.size, "{1} could not be reached; the other sources' news is here.", "{1} could not be reached; the other sources' news is here.", failed.joinToString(", ") { it.name }),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
        )
        QuietButton(tr("Try again"), onClick = onRetry, modifier = Modifier.testTag("retry-failed"))
    }
}

/** No source could be read: the reader is offline, or every site is down. */
@Composable
internal fun OutageCard(onRetry: () -> Unit, compact: Boolean) {
    MessageCard(
        title = tr("The news can't be reached right now"),
        message = tr("Check your connection and try again."),
        compact = compact,
        tag = "news-outage",
    ) {
        PrimaryButton(tr("Try again"), onClick = onRetry, modifier = Modifier.testTag("retry-all"))
    }
}

/** The filters leave no article: the ways out are clearing them and, where the time is narrowed, any time. */
@Composable
internal fun NoMatchCard(state: NewsUiState, actions: NewsActions, compact: Boolean) {
    MessageCard(
        title = tr("No articles match these filters."),
        message = tr("Try another topic or source, or clear the filters."),
        compact = compact,
        tag = "news-no-match",
        art = { EmptyStateIllustration(Modifier.width(if (compact) AppSizes.emptyArtCompact else AppSizes.emptyArt)) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            SecondaryButton(tr("Clear filters"), onClick = actions.onClear, modifier = Modifier.testTag("no-match-clear"))
            if (state.shownPeriod != NewsPeriod.ANY) SecondaryButton(tr("Any time"), onClick = { actions.onPeriod(NewsPeriod.ANY) }, modifier = Modifier.testTag("no-match-any-time"))
        }
    }
}

/** The mascot ([art]), a title and a message centred on a card, and what to do about it. */
@Composable
internal fun MessageCard(
    title: String,
    message: String,
    compact: Boolean,
    tag: String,
    art: @Composable () -> Unit = { JourneyIllustration(Modifier.width(if (compact) 140.dp else 180.dp)) },
    actions: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(if (compact) AppSpacing.xl else AppSpacing.xxxl).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        art()
        Text(title, style = if (compact) AppText.sectionTitle else AppText.headline, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(AppSpacing.xs))
        actions()
    }
}

/** How the front page is laid out: the lead beside a 2×2 block, the lead over two columns, or one column. */
internal enum class FrontLayout { WIDE, MEDIUM, PHONE }

internal val GRID_GAP = 20.dp
internal const val LEAD_WEIGHT = 1.6f
internal val LEAD_PICTURE = 300.dp
internal val MEDIUM_LEAD_PICTURE = 240.dp
internal val PHONE_LEAD_PICTURE = 190.dp
internal val CARD_PICTURE = 120.dp
private val THUMB = 84.dp
private const val READ_ALPHA = 0.55f
private val ROW_RADIUS = 18.dp
/** The line around the rows' card and between its rows. */
private val ROW_LINE = 1.dp
/** The source's mark: a square with its corners just softened. */
private val DOT_CORNER = 2.dp
private val BADGE_PADDING = 3.dp
