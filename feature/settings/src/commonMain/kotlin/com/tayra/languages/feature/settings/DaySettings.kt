package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.components.SettingRow
import com.tayra.languages.core.ui.i18n.tr

private fun hourLabel(hour: Int) = "${hour.toString().padStart(2, '0')}:00"

/** When a new study day starts, and the longest a review may wait. */
@Composable
internal fun DaySettings(settings: UserSettings, viewModel: SettingsViewModel, dense: Boolean = false) {
    SettingRow(
        tr("Next day starts at"),
        if (dense) null else tr("Reviews done before this hour count for the day before."),
        divider = !dense,
        stackOnCompact = !dense,
        dense = dense,
    ) {
        SelectMenu((0..23).toList(), settings.flashcardDayStartHour, { hour -> viewModel.update { it.copy(flashcardDayStartHour = hour) } }, optionLabel = ::hourLabel, buttonModifier = Modifier.testTag("day-start"))
    }
    if (dense) AppDivider()
    val saved = settings.flashcardMaxInterval
    var text by remember { mutableStateOf(saved.toString()) }
    // Typing is left alone; a value saved from elsewhere, such as a reset, replaces what is typed.
    LaunchedEffect(saved) { if (text.toIntOrNull() != saved) text = saved.toString() }
    val days = text.trim().toIntOrNull()?.takeIf { it in 1..UserSettings.MAX_FLASHCARD_INTERVAL }
    val name = tr("Maximum interval")
    SettingRow(
        name,
        if (dense) null else tr("The longest a review waits, in days. Words not yet Known still come back within a week."),
        divider = !dense,
        stackOnCompact = !dense,
        dense = dense,
    ) {
        AppTextField(
            value = text,
            onValueChange = {
                text = it
                it.trim().toIntOrNull()?.takeIf { n -> n in 1..UserSettings.MAX_FLASHCARD_INTERVAL && n != saved }?.let { n -> viewModel.update { s -> s.copy(flashcardMaxInterval = n) } }
            },
            singleLine = true,
            isError = days == null,
            suffix = { Text(tr("days")) },
            supportingText = if (days == null) ({ Text(tr("From 1 to {0}", UserSettings.MAX_FLASHCARD_INTERVAL)) }) else null,
            modifier = Modifier.width(DAYS_FIELD).semantics { contentDescription = name },
        )
    }
}

private val DAYS_FIELD = 170.dp
