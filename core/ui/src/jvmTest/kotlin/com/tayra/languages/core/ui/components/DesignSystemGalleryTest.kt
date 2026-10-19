package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppTheme
import com.tayra.languages.core.ui.theme.AppThemes
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * Every control of the design system in the light and the dark theme, for review: run with
 * DESIGN_GALLERY=<folder> to get a picture of each theme there.
 */
@OptIn(ExperimentalTestApi::class)
class DesignSystemGalleryTest {

    @Test
    fun theControlsDrawInEveryTheme() {
        for (theme in listOf(AppThemes.default, AppThemes.night)) gallery(theme)
    }

    private fun gallery(theme: AppTheme) = runDesktopComposeUiTest(width = 1100, height = 1180) {
        setContent { TayraTheme(theme) { Gallery() } }
        onAllNodesWithText("Primary").onFirst().assertExists()
        System.getenv("DESIGN_GALLERY")?.let { folder ->
            waitForIdle()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(folder, "design-system-${theme.id}.png"))
        }
    }

    @Composable
    private fun Gallery() {
        // On a surface, as the app's screens are, for the text colours they get from it.
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) { Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            for (size in ButtonSize.entries) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Label(size.name)
                    PrimaryButton("Primary", onClick = {}, icon = AppIcons.Download, size = size)
                    SecondaryButton("Secondary", onClick = {}, size = size)
                    QuietButton("Quiet", onClick = {}, size = size)
                    PrimaryButton("Delete", onClick = {}, tone = ButtonTone.Danger, size = size)
                    SecondaryButton("Remove", onClick = {}, tone = ButtonTone.Danger, size = size)
                    QuietButton("Discard", onClick = {}, tone = ButtonTone.Danger, size = size)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Label("States")
                PrimaryButton("Disabled", onClick = {}, enabled = false)
                SecondaryButton("Disabled", onClick = {}, enabled = false)
                PrimaryButton("Saving", onClick = {}, loading = true)
                SecondaryButton("Checking", onClick = {}, loading = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Label("Inputs")
                AppTextField("casa", {}, Modifier.width(240.dp), label = { Text("Word") }, singleLine = true)
                AppTextField("", {}, Modifier.width(240.dp), placeholder = { Text("Search books") }, singleLine = true)
                AppTextField("x", {}, Modifier.width(200.dp), label = { Text("Error") }, isError = true, singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Label("Chips")
                AppFilterChip(selected = true, onClick = {}, label = { Text("Easy") })
                AppFilterChip(selected = false, onClick = {}, label = { Text("Medium") })
                AppFilterChip(selected = false, onClick = {}, label = { Text("Hard") })
                StatusPill("Installed", StatusTints.ok)
                Tag("A2", MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Label("Patterns")
                SearchField("", {}, "Search books", Modifier.width(260.dp))
                SegmentedToggle(listOf(Segment("list", AppIcons.ViewList, description = "List"), Segment("grid", AppIcons.GridView, description = "Grid")), selected = "list", onSelect = {})
                SegmentedToggle(listOf(Segment(1, label = "Installed"), Segment(2, label = "Available")), selected = 2, onSelect = {})
                AppIconButton(AppIcons.Download, "Download", onClick = {})
                AppIconButton(AppIcons.Download, "Download", onClick = {}, style = IconButtonStyle.Outlined)
                AppIconButton(AppIcons.Download, "Download", onClick = {}, style = IconButtonStyle.Filled, size = IconButtonSize.Large)
                OverflowMenu("More") {}
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Label("Progress")
                ProgressTrack(0.6f, MaterialTheme.colorScheme.primary, Modifier.width(160.dp))
                ProgressTrack(0.3f, AppAccents.green, Modifier.width(160.dp))
                AppProgressBar(0.45f, Modifier.width(160.dp))
                AppSpinner(size = SpinnerSize.Small)
                AppSpinner()
                AppCheckbox(checked = true, onCheckedChange = {})
            }
            SelectionBar("2 of 5 selected", leading = { AppCheckbox(checked = false, onCheckedChange = {}) }) {
                QuietButton("Archive", onClick = {})
                QuietButton("Delete", onClick = {}, tone = ButtonTone.Danger)
            }
            Column {
                TableHeader {
                    TableHeaderCell("Book", Modifier.weight(1f), active = true, onClick = {})
                    TableHeaderCell("Reading", Modifier.weight(1f))
                    TableHeaderCell("Last opened", Modifier.weight(1f))
                }
                AppDivider()
            }
            EmptyState(AppIcons.Book, "No books match these filters.", message = "Try other filters.", action = { QuietButton("Create a book", onClick = {}) })
            ContentCard("Card", "A card with a setting, a banner and a header button.", icon = AppIcons.Download, modifier = Modifier.fillMaxWidth()) {
                SwitchSetting("A switch", "Its description.", checked = true) {}
                InfoBanner("An information banner.")
                HeaderButton("Header button", AppIcons.Download, onClick = {})
            }
        } }
    }

    @Composable
    private fun Label(text: String) {
        Text(text, Modifier.width(80.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
