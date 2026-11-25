package com.idiomiko.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.idiomiko.core.domain.fonts.FontFiles
import com.idiomiko.core.domain.fonts.OwnFont
import com.idiomiko.core.domain.fonts.OwnFontRepository
import com.idiomiko.core.domain.settings.UserSettings
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.RemovableChip
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SettingRow
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.FontChoice
import com.idiomiko.core.ui.theme.LocalOwnFonts
import com.idiomiko.core.ui.theme.fontFamily
import com.idiomiko.core.ui.theme.ownFonts
import com.idiomiko.core.ui.theme.AppSpacing
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

/** Adding and removing the reader's own fonts, and keeping the reading font in step with them. */
internal class OwnFontActions(private val repository: OwnFontRepository, private val viewModel: SettingsViewModel) {
    /** Keeps the font file and reads in it; false when it is not a font the app can use. */
    suspend fun add(fileName: String, bytes: ByteArray): Boolean {
        val font = repository.add(fileName, bytes) ?: return false
        viewModel.update { it.copy(readingFont = FontChoice.Own(font).id) }
        return true
    }

    /** Removes the font; text read in it goes back to the default font. */
    suspend fun remove(font: OwnFont) {
        repository.remove(font.id)
        val id = FontChoice.Own(font).id
        viewModel.update { if (it.readingFont == id) it.copy(readingFont = UserSettings().readingFont) else it }
    }
}

/**
 * The reader's own font files under the font setting: each drawn in itself with a × that removes it,
 * and a button that adds a TrueType or OpenType file, which becomes the reading font.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OwnFontsSetting(viewModel: SettingsViewModel, dense: Boolean = false) {
    val repository = LocalOwnFonts.current ?: return
    val actions = remember(repository, viewModel) { OwnFontActions(repository, viewModel) }
    val fonts = ownFonts()
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    var notAFont by remember { mutableStateOf(false) }
    val picker = rememberFilePickerLauncher(type = FileKitType.File(FontFiles.pickerExtensions)) { file ->
        if (file != null) scope.launch {
            adding = true
            notAFont = !runCatching { actions.add(file.name, file.readBytes()) }.getOrDefault(false)
            adding = false
        }
    }
    Column(Modifier.fillMaxWidth().testTag("own-fonts"), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        SettingRow(tr("Your fonts"), null, divider = !dense, dense = dense) {
            SecondaryButton(
                tr("Add a font file"),
                onClick = { notAFont = false; picker.launch() },
                icon = Icons.Default.Add,
                loading = adding,
                size = ButtonSize.Compact,
                modifier = Modifier.testTag("add-font"),
            )
        }
        if (fonts.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s), itemVerticalAlignment = Alignment.CenterVertically) {
                fonts.forEach { font ->
                    RemovableChip(
                        onRemove = { scope.launch { actions.remove(font) } },
                        removeDescription = tr("Remove {0}", font.name),
                        modifier = Modifier.testTag("own-font-${font.id}"),
                    ) {
                        Text(font.name, fontFamily = FontChoice.Own(font).fontFamily(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (notAFont) {
            Text(tr("That file is not a TrueType or OpenType font."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
