package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.flashcards.PersonalParameters
import com.tayra.languages.core.domain.flashcards.PersonalScheduling
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.formatDate
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** A language with answers to train on: how many, the parameters it schedules with, and what the last training came to here. */
data class LanguageScheduling(
    val languageId: Long,
    val name: String,
    val reviews: Int,
    val current: PersonalParameters?,
    val outcome: PersonalScheduling.Outcome? = null,
)

class PersonalSchedulingViewModel(private val personal: PersonalScheduling, private val languages: LanguageRepository) : ViewModel() {
    private val _state = MutableStateFlow<List<LanguageScheduling>>(emptyList())
    val state: StateFlow<List<LanguageScheduling>> = _state.asStateFlow()
    val progress: StateFlow<Map<Long, Double>> = personal.progress

    init {
        load()
        // A training finished, whether asked for here or begun of its own accord, shows what it came to.
        viewModelScope.launch { personal.progress.map { it.keys }.distinctUntilChanged().collect { load() } }
    }

    private fun load() {
        viewModelScope.launch {
            val outcomes = _state.value.associate { it.languageId to it.outcome }
            _state.value = languages.getAll().mapNotNull { language ->
                val reviews = personal.reviewCount(language.id).takeIf { it > 0 } ?: return@mapNotNull null
                LanguageScheduling(language.id, language.name, reviews, personal.current(language.id), outcomes[language.id])
            }
        }
    }

    fun optimize(languageId: Long) {
        viewModelScope.launch {
            val outcome = personal.optimize(languageId)
            _state.update { list -> list.map { if (it.languageId == languageId) it.copy(outcome = outcome) else it } }
            load()
        }
    }

    fun useDefaults(languageId: Long) {
        viewModelScope.launch {
            personal.useDefaults(languageId)
            _state.update { list -> list.map { if (it.languageId == languageId) it.copy(outcome = null) else it } }
            load()
        }
    }
}

/**
 * Each language's own scheduling: whether it schedules with parameters trained on its answers or
 * with the defaults, the way to train it now, and the way back to the defaults.
 */
@Composable
internal fun PersonalSchedulingCard(viewModel: PersonalSchedulingViewModel, dense: Boolean = false) {
    val languages by viewModel.state.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    ContentCard(
        tr("Personal scheduling"),
        if (dense) null else tr("Fits the scheduling to how you remember, trained on your own answers in each language; trained again by itself after every 500 new answers."),
        icon = AppIcons.Tune,
        dense = dense,
        modifier = Modifier.testTag("personal-scheduling"),
    ) {
        if (languages.isEmpty()) {
            Text(tr("Your answers to flashcards train it once there are some."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        languages.forEachIndexed { index, language ->
            if (index > 0) AppDivider()
            LanguageRow(language, progress[language.languageId], onOptimize = { viewModel.optimize(language.languageId) }, onDefaults = { viewModel.useDefaults(language.languageId) })
        }
    }
}

@Composable
private fun LanguageRow(language: LanguageScheduling, progress: Double?, onOptimize: () -> Unit, onDefaults: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val current = language.current
    val trained = current?.parameters != null
    Column(Modifier.fillMaxWidth().padding(vertical = AppSpacing.s).testTag("personal-${language.languageId}"), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(language.name, style = AppText.itemTitle)
        val status = if (current != null && trained) {
            tr("Your own: trained on {0} answers on {1}; it predicts your recall {2}% better than the defaults.", formatCount(current.reviews), current.trainedAt.formatDate(), (current.gain * 100).roundToInt().coerceAtLeast(1))
        } else {
            tr("The defaults, with {0} answers to learn from.", formatCount(language.reviews))
        }
        Text(status, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.testTag("personal-status-${language.languageId}"))
        when (language.outcome) {
            PersonalScheduling.Outcome.Kept -> tr("Training found nothing that predicts your answers better than what is in use.")
            PersonalScheduling.Outcome.TooFew -> tr("Too few answers to learn from yet.")
            else -> null
        }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.testTag("personal-outcome-${language.languageId}")) }
        Row(Modifier.padding(top = AppSpacing.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            SecondaryButton(
                if (progress != null) tr("Optimising… {0}%", (progress * 100).roundToInt()) else tr("Optimise"),
                onClick = onOptimize,
                enabled = progress == null,
                size = ButtonSize.Compact,
                modifier = Modifier.testTag("personal-optimise-${language.languageId}"),
            )
            if (trained && progress == null) QuietButton(tr("Use defaults"), onClick = onDefaults, size = ButtonSize.Compact, modifier = Modifier.testTag("personal-defaults-${language.languageId}"))
        }
    }
}
