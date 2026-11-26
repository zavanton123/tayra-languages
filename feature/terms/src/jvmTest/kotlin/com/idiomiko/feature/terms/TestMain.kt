package com.idiomiko.feature.terms

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain

/**
 * The view model tests' Dispatchers.Main: Dispatchers.Default, set once for the whole run and never reset. The view
 * models a test leaves behind keep running on Main into the next test, and setting or resetting Main while they use it
 * fails ("Dispatchers.Main is used concurrently with setting it").
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal object TestMain {
    init {
        Dispatchers.setMain(Dispatchers.Default)
    }

    /** Makes sure Main is set; the first call sets it. */
    fun use() = Unit
}
