package com.idiomiko.feature.frequency

import com.idiomiko.core.domain.frequency.FrequencyListDownloads
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** A list the build bundles is read from its resources; one it leaves out is fetched, and tried again after a failure. */
class ResourceFrequencyListsTest {
    @Test
    fun aBundledListNeedsNoDownload() = runTest {
        var fetched = 0
        val lists = ResourceFrequencyLists { fetched++; null }
        assertNotNull(lists.list("pt"))
        assertEquals(0, fetched)
    }

    @Test
    fun aListLeftOutIsFetchedAndTriedAgainAfterAFailure() = runTest {
        var offline = true
        var fetched = 0
        val lists = ResourceFrequencyLists(FrequencyListDownloads {
            fetched++
            if (offline) error("offline") else "# source\nwordo\nvordo\n".encodeToByteArray()
        })
        assertNull(lists.list("zz"))
        offline = false
        assertEquals(listOf("wordo", "vordo"), lists.list("zz")?.words?.map { it.word })
        lists.list("zz")
        assertEquals(2, fetched, "kept once fetched")
    }
}
