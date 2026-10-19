package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import com.tayra.languages.core.domain.books.BookPack
import com.tayra.languages.core.domain.books.BookPackStatus
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppSpacing

/** The book packs page on a phone: each bundle a card with its summary, its size and one full-width button. */
@Composable
internal fun PhoneBookPacksScreen(
    packs: List<BookPackStatus>,
    languageName: String?,
    onDownload: (BookPack) -> Unit,
    onRemove: (BookPack) -> Unit,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var removing by remember { mutableStateOf<BookPack?>(null) }
    Scaffold(
        topBar = { AppTopBar(title = tr("Book packs"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Text(tr("Download a bundle of free books, graded by level."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            InfoBanner(tr("Downloaded bundles appear in Books and can be edited."))
            if (packs.isEmpty() && languageName != null) {
                Text(tr("There is no book bundle for {0} yet.", tr(languageName)), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            packs.forEach { status ->
                PhonePackCard(status.pack.info(), status.state, bookPackWords(), onDownload = { onDownload(status.pack) }, onRemove = { removing = status.pack }) {
                    Text(tr(status.pack.summary), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
    removing?.let { pack -> RemoveBookPackDialog(pack, onRemove = { onRemove(pack) }, onDismiss = { removing = null }) }
}
