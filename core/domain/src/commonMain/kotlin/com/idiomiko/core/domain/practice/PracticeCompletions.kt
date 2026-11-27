package com.idiomiko.core.domain.practice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * The pages whose practice was taken to its end while the app runs: a lesson's Quiz stage can be
 * checked once its page's practice is, wherever that practice was shown.
 */
class PracticeCompletions {
    private val pages = MutableStateFlow<Set<PracticeSource.Page>>(emptySet())

    /** Notes that a practice of [page] reached its result. */
    fun finished(page: PracticeSource.Page) = pages.update { it + page }

    /** Whether a practice of [page] has reached its result. */
    fun observe(page: PracticeSource.Page): Flow<Boolean> = pages.map { page in it }.distinctUntilChanged()
}
