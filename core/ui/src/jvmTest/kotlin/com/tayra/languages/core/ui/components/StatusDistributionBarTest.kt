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
                    Text("85% new", softWrap = false)
                }
            }
        }
        val bar = rule.onNodeWithTag(STATUS_BAR_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        val label = rule.onNodeWithText("85% new").fetchSemanticsNode().boundsInWindow
        assertTrue(bar.right <= label.left + 0.5f, "the bar should end where the label starts: $bar, $label")
        assertTrue(label.width > label.height * 2, "the label should sit on one line: $label")
    }
}
