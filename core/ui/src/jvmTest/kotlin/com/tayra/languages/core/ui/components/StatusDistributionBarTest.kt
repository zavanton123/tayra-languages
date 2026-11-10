package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.model.BookStats
import com.tayra.languages.core.domain.model.TermStatus
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class StatusDistributionBarTest {

    @get:Rule
    val rule = createComposeRule()

    /** As in the Books list: the bar takes the row's spare width and the label beside it stays on one line. */
    @Test
    fun aWeightedBarLeavesRoomForTheLabelBesideIt() {
        val stats = BookStats(
            distinctTerms = 100,
            distinctUnknowns = 85,
            unknownPercent = 85,
            statusDistribution = mapOf(TermStatus.UNKNOWN to 85, TermStatus.NEW_1 to 5, TermStatus.WELL_KNOWN to 8, TermStatus.IGNORED to 2),
        )
        rule.setContent {
            MaterialTheme {
                Row(Modifier.padding(40.dp).width(400.dp)) {
                    StatusDistributionBar(stats, Modifier.weight(1f))
                    Text("85 unknown words", softWrap = false)
                }
            }
        }
        val bar = rule.onNodeWithTag(STATUS_BAR_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        val label = rule.onNodeWithText("85 unknown words").fetchSemanticsNode().boundsInWindow
        assertTrue(bar.right <= label.left + 0.5f, "the bar should end where the label starts: $bar, $label")
        assertTrue(label.width > label.height * 2, "the label should sit on one line: $label")
    }

    /** The blue is the unknown words' share of the different words; known and learning ones are the white. */
    @Test
    fun theBlueIsTheShareOfUnknownWords() {
        val stats = BookStats(
            distinctTerms = 100,
            distinctUnknowns = 25,
            unknownPercent = 25,
            statusDistribution = mapOf(TermStatus.UNKNOWN to 25, TermStatus.NEW_1 to 30, TermStatus.WELL_KNOWN to 45),
        )
        rule.setContent { MaterialTheme { Row(Modifier.width(400.dp)) { StatusDistributionBar(stats, Modifier.width(400.dp)) } } }
        val bar = rule.onNodeWithTag(STATUS_BAR_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        val blue = rule.onNodeWithTag(UNKNOWN_SHARE_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        assertTrue(kotlin.math.abs(blue.width / bar.width - 0.25f) < 0.02f, "a quarter of the bar is blue: $blue of $bar")
    }

    /** With no word unknown the bar is all white. */
    @Test
    fun noUnknownWordLeavesTheBarWhite() {
        val stats = BookStats(
            distinctTerms = 10,
            distinctUnknowns = 0,
            unknownPercent = 0,
            statusDistribution = mapOf(TermStatus.NEW_1 to 4, TermStatus.WELL_KNOWN to 6),
        )
        rule.setContent { MaterialTheme { StatusDistributionBar(stats, Modifier.width(400.dp)) } }
        rule.onNodeWithTag(STATUS_BAR_TAG, useUnmergedTree = true).assertExists()
        rule.onNodeWithTag(UNKNOWN_SHARE_TAG, useUnmergedTree = true).assertDoesNotExist()
    }
}
