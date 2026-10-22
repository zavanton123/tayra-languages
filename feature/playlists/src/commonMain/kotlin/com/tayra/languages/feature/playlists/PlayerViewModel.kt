package com.tayra.languages.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.playlists.ListeningTime
import com.tayra.languages.core.domain.playlists.PlayerState
import com.tayra.languages.core.domain.playlists.PlaylistPlayer
import com.tayra.languages.core.domain.playlists.PlaylistService
import com.tayra.languages.core.domain.playlists.RepeatMode
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration

/** What the player screen and bar show: the player's state with the listening times and the sentence's translation. */
data class PlayerUiState(
    val player: PlayerState = PlayerState(),
    /** The translation of the sentence being heard, into the native language; null while there is none. */
    val translation: String? = null,
    val duration: Duration = Duration.ZERO,
    val durations: Map<Long, Duration> = emptyMap(),
    /** How far into the current text, and how long it is. */
    val elapsed: Duration = Duration.ZERO,
    val total: Duration = Duration.ZERO,
    val speed: Float = 1f,
)

/** The app's playlist player, for its screen and its bar: the commands go straight to the player. */
class PlayerViewModel(
    val player: PlaylistPlayer,
    private val service: PlaylistService,
    private val translator: SentenceTranslator,
    private val languages: LanguageRepository,
    settings: SettingsRepository,
    time: ListeningTime,
) : ViewModel() {
    /** Translations of the sentences heard, by text, so going back to one shows it at once. */
    private val translations = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        viewModelScope.launch {
            player.state.map { it.currentSentence to it.playlist?.languageId }.distinctUntilChanged().collect { (sentence, languageId) ->
                if (sentence == null || languageId == null || sentence in translations.value) return@collect
                val language = languages.getById(languageId) ?: return@collect
                translator.translate(sentence, language)?.let { text -> translations.update { it + (sentence to text) } }
            }
        }
    }

    val state: StateFlow<PlayerUiState> = combine(
        player.state,
        translations,
        combine(time.changes, settings.settings.map { it.speechSpeed }.distinctUntilChanged()) { _, speed -> speed },
    ) { p, known, speed ->
        val code = p.languageCode
        val playlist = p.playlist
        PlayerUiState(
            player = p,
            translation = p.currentSentence?.let { known[it] },
            duration = playlist?.let { service.duration(it, code) } ?: Duration.ZERO,
            durations = playlist?.items.orEmpty().associate { it.id to service.duration(it.length, code) },
            elapsed = service.duration(p.heard, code),
            total = service.duration(p.textLength, code),
            speed = speed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUiState(player.state.value))

    fun cycleRepeat() {
        player.setRepeat(
            when (player.state.value.repeat) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            },
        )
    }

    fun remove(itemId: Long) {
        viewModelScope.launch { service.remove(itemId) }
    }

    fun move(itemId: Long, toIndex: Int) {
        val playlistId = player.state.value.playlist?.id ?: return
        viewModelScope.launch { service.move(playlistId, itemId, toIndex) }
    }

    companion object {
        /** The speeds offered, normal among them. */
        val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
    }
}
