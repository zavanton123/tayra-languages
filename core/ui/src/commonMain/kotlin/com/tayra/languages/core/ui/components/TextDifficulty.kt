package com.tayra.languages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.books.discover.Difficulty
import com.tayra.languages.core.domain.books.discover.TextCoverage
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppShapes

/** What is known of how hard a text found online is for the reader. */
sealed interface CoverageCheck {
    data object NotChecked : CoverageCheck
    data object Checking : CoverageCheck
    data class Checked(val coverage: TextCoverage) : CoverageCheck
    data class Failed(val message: String) : CoverageCheck
}

/**
 * A text's difficulty and how it was found: the tag, then "You know N% of the words", the check
 * under way, why it failed, or, with [onCheck], a button to check it; [tag] names it in tests.
 */
@Composable
fun CoverageLine(difficulty: Difficulty?, check: CoverageCheck, tag: String, onCheck: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        difficulty?.let { DifficultyTag(it) }
        when (check) {
            CoverageCheck.Checking -> {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(tr("Checking the words…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            is CoverageCheck.Checked -> Text(
                tr("You know {0}% of the words", check.coverage.percent),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.testTag("coverage-$tag"),
            )
            is CoverageCheck.Failed -> Text(check.message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            CoverageCheck.NotChecked -> if (onCheck != null) {
                QuietButton(tr("Check the words"), onClick = onCheck, modifier = Modifier.height(32.dp).testTag("check-$tag"), size = ButtonSize.Compact)
            }
        }
    }
}

@Composable
fun DifficultyTag(difficulty: Difficulty) {
    Text(
        difficulty.label,
        Modifier.clip(AppShapes.pill).background(difficulty.color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelLarge,
        color = difficulty.color,
        fontWeight = FontWeight.SemiBold,
    )
}

/** Chips to show texts of one difficulty, or of all of them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DifficultyFilter(selected: Difficulty?, onSelect: (Difficulty?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (listOf<Difficulty?>(null) + Difficulty.entries).forEach { difficulty ->
            AppFilterChip(
                selected = selected == difficulty,
                onClick = { onSelect(difficulty) },
                label = { Text(difficulty?.label ?: tr("All levels")) },
                leadingIcon = difficulty?.let { { Box(Modifier.size(10.dp).clip(AppShapes.tiny).background(it.color)) } },
                modifier = Modifier.testTag("difficulty-${difficulty?.name ?: "ALL"}"),
            )
        }
    }
}

val Difficulty.label: String
    get() = when (this) {
        Difficulty.EASY -> tr("Easy")
        Difficulty.MEDIUM -> tr("Medium")
        Difficulty.HARD -> tr("Hard")
    }

val Difficulty.color: Color
    get() = when (this) {
        Difficulty.EASY -> Color(0xFF2E9D57)
        Difficulty.MEDIUM -> Color(0xFFE08A1E)
        Difficulty.HARD -> Color(0xFFD64545)
    }
