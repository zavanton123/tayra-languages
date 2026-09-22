package com.tayra.languages.core.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** One-off events from a view model to the UI (navigation, messages). */
class UiEvents<T> {
    private val channel = Channel<T>(Channel.BUFFERED)
    val flow: Flow<T> = channel.receiveAsFlow()
    suspend fun send(event: T) = channel.send(event)
    fun trySend(event: T) {
        channel.trySend(event)
    }
}

@Composable
fun <T> CollectEvents(events: UiEvents<T>, handler: suspend (T) -> Unit) {
    LaunchedEffect(events) {
        events.flow.collect { handler(it) }
    }
}
