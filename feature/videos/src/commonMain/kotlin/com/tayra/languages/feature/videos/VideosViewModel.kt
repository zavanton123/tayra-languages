package com.tayra.languages.feature.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.media.VideoListItem
import com.tayra.languages.core.domain.media.VideoRepository
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VideosUiState(
    val loading: Boolean = true,
    val videos: List<VideoListItem> = emptyList(),
    val search: String = "",
) {
    val shown: List<VideoListItem>
        get() = if (search.isBlank()) videos
        else videos.filter { it.video.title.contains(search.trim(), ignoreCase = true) || it.video.channel?.contains(search.trim(), ignoreCase = true) == true }
}

class VideosViewModel(
    videos: VideoRepository,
    private val books: BookRepository,
    settings: SettingsRepository,
    private val bookStats: BookStatsService,
) : ViewModel() {
    /** Videos whose share of known words is being counted, so each is counted once. */
    private val counting = mutableSetOf<Long>()

    private val _state = MutableStateFlow(VideosUiState())
    val state: StateFlow<VideosUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(videos.observeVideos(), settings.settings.map { it.currentLanguageId }) { all, language ->
                all.filter { it.video.languageId == language }
            }.collect { list ->
                _state.update { it.copy(loading = false, videos = list) }
                // A video's statistics are counted once and kept; until then the card has no "% known".
                list.filter { it.unknownPercent == null && counting.add(it.video.bookId) }.forEach { item ->
                    launch { runCatching { bookStats.stats(item.video.bookId) } }
                }
            }
        }
    }

    fun setSearch(text: String) = _state.update { it.copy(search = text) }

    fun delete(bookId: Long) {
        viewModelScope.launch { books.deleteBook(bookId) }
    }
}
