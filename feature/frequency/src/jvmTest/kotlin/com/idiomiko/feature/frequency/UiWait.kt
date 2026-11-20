package com.idiomiko.feature.frequency

/**
 * How long the vocabulary level tests wait for the view model. Loading the frequency list and saving a level's
 * thousands of terms take seconds on their own, and several times longer while the commit build compiles beside them.
 */
internal const val UI_WAIT_MILLIS = 60_000L
