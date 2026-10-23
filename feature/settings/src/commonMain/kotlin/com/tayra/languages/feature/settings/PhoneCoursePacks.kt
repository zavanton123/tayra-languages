package com.tayra.languages.feature.settings

import com.tayra.languages.core.ui.components.ScrollColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.courses.CoursePack
import com.tayra.languages.core.domain.courses.CoursePackStatus
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/** The course packs page on a phone: each pack a card with its figures, its size and one full-width button. */
@Composable
internal fun PhoneCoursePacksScreen(
    packs: List<CoursePackStatus>,
    languageName: String?,
    onDownload: (CoursePack) -> Unit,
    onRemove: (CoursePack) -> Unit,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var removing by remember { mutableStateOf<CoursePack?>(null) }
    Scaffold(
        topBar = { AppTopBar(title = tr("Course packs"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        ScrollColumn(Modifier.padding(padding).fillMaxWidth(), contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Text(tr("Download ready-made courses for offline use."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            InfoBanner(tr("Downloaded packs appear in Courses and can be edited."))
            if (packs.isEmpty() && languageName != null) {
                Text(tr("There are no ready-made courses for {0} yet.", tr(languageName)), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            packs.forEach { status ->
                PhonePackCard(status.pack.info(), status.state, coursePackWords(), onDownload = { onDownload(status.pack) }, onRemove = { removing = status.pack }, onOpen = { onNavigate(Route.Courses) }) {
                    // Every pack is cut the same way: 100 courses of 10 lessons, graded A1 to C2 (see CoursePack.summary).
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                        Figure("100", tr("courses"), Modifier.weight(1f))
                        Figure("1,000", tr("lessons"), Modifier.weight(1f))
                        Figure("A1–C2", tr("levels"), Modifier.weight(1f))
                    }
                    Text(tr("Graded mini stories built on the 10,000 most common words."), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
    removing?.let { pack -> RemoveCoursePackDialog(pack, onRemove = { onRemove(pack) }, onDismiss = { removing = null }) }
}

/** A figure of the pack in a tinted box: the number large, what it counts under it. */
@Composable
private fun Figure(value: String, label: String?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.clip(AppShapes.panel).background(colors.surfaceContainerLow).padding(vertical = AppSpacing.m, horizontal = AppSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = AppText.sectionTitle, textAlign = TextAlign.Center)
        if (label != null) Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        else Spacer(Modifier.height(0.dp))
    }
}
