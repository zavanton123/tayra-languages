package com.tayra.languages.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

/** Buttons in a scrolling column take a mouse click wherever they stand, also half out of view. */
class ScrollColumnClickTest {

    @get:Rule
    val rule = createComposeRule()

    /** A click focuses the button, which scrolls it fully into view; the click must survive that scroll. */
    @Test
    fun aButtonHalfOutOfViewTakesTheFirstClick() {
        var clicks = 0
        rule.setContent {
            Box(Modifier.size(300.dp, 200.dp)) {
                ScrollColumn(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(170.dp))
                    // The lower half of the button is below the column's edge.
                    Box(Modifier.fillMaxWidth().height(60.dp).testTag("button").clickable { clicks++ })
                    Spacer(Modifier.height(400.dp))
                }
            }
        }
        rule.onNodeWithTag("button").performMouseInput {
            moveTo(Offset(center.x, 10.dp.toPx()))
            press()
            advanceEventTime(150)
            release()
        }
        rule.waitForIdle()
        assertEquals(1, clicks, "the first click acts")
    }
}
