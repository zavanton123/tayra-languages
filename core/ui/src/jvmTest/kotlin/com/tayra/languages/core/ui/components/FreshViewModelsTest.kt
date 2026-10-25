package com.tayra.languages.core.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/** A dialog's view models are its own: shown again, it starts afresh, and the ones it had are cleared. */
@OptIn(ExperimentalTestApi::class)
class FreshViewModelsTest {
    private class Session : ViewModel() {
        var cleared = false
        override fun onCleared() {
            cleared = true
        }
    }

    @Test
    fun shownAgainItStartsAfresh() = runDesktopComposeUiTest {
        val seen = mutableListOf<Session>()
        var shown by mutableStateOf(true)
        setContent {
            if (shown) FreshViewModels {
                val session = viewModel { Session() }
                if (seen.lastOrNull() !== session) seen += session
            }
        }
        waitForIdle()
        shown = false
        waitForIdle()
        assertTrue(seen.single().cleared, "cleared when it goes")
        shown = true
        waitForIdle()
        assertEquals(2, seen.size)
        assertNotSame(seen[0], seen[1])
    }
}
