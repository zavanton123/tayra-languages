package com.idiomiko.core.domain.frequency

import kotlin.test.Test
import kotlin.test.assertEquals

class VocabularyProgressTest {
    @Test
    fun theNextMilestoneIsTheFirstPastTheWordsKnown() {
        // 1,532 known, 1,520 of them among the 2,000 most common words.
        val byRank = List(10_000) { it < 1_520 }
        val progress = VocabularyProgress.of(1_532, byRank)
        assertEquals(2_000, progress.milestone)
        assertEquals(1_520, progress.knownInMilestone)
        assertEquals(0.76f, progress.share)
    }

    @Test
    fun theWholeListIsTheLastMilestone() {
        assertEquals(1_800, VocabularyProgress.of(1_532, List(1_800) { true }).milestone)
        assertEquals(10_000, VocabularyProgress.of(12_000, List(10_000) { it % 2 == 0 }).milestone)
        assertEquals(1f, VocabularyProgress.of(1_800, List(1_800) { true }).share)
    }

    @Test
    fun withoutAListOnlyTheWordsKnownCount() {
        val progress = VocabularyProgress.of(40, emptyList())
        assertEquals(null, progress.milestone)
        assertEquals(0f, progress.share)
        assertEquals(100, VocabularyProgress.of(0, List(5_000) { false }).milestone)
    }
}
