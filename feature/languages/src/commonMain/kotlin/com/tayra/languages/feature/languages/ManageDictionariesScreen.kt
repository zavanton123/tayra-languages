package com.tayra.languages.feature.languages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.model.LanguageDictionary
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppSnackbar
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.itemMotion
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Enables or disables the online dictionaries offered for one language. */
@Composable
fun ManageDictionariesScreen(
    languageId: Long,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: ManageDictionariesViewModel = koinViewModel(key = "dictionaries-$languageId") { parametersOf(languageId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val compact = LocalWindowWidth.current.isCompact
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = { AppTopBar(title = tr("Dictionaries"), onNavigate = onNavigate, section = NavSection.SETTINGS, onBack = if (compact) onBack else null) },
        snackbarHost = {
            state.message?.let { AppSnackbar(it, actionLabel = tr("OK"), onAction = viewModel::dismissMessage) }
        },
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            ScrollList(
                Modifier.fillMaxSize().widthIn(max = AppSizes.readableWidth),
                contentPadding = PaddingValues(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = AppSpacing.l),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
            ) {
                item {
                    Row(Modifier.fillMaxWidth().padding(bottom = AppSpacing.m), verticalAlignment = Alignment.CenterVertically) {
                        if (!compact) {
                            AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, tr("Back"), onBack, style = IconButtonStyle.Outlined)
                            Spacer(Modifier.width(AppSpacing.xl))
                        }
                        Icon(AppIcons.Book, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconXLarge))
                        Spacer(Modifier.width(AppSpacing.l))
                        Column(Modifier.weight(1f)) {
                            // On a phone the bar carries the title.
                            if (!compact) Text(tr("Dictionaries"), style = AppText.headline)
                            val pair = listOfNotNull(state.source?.name, state.target?.name).joinToString(" \u2192 ") { tr(it) }
                            Text(tr("{0} \u00b7 changes apply immediately", pair), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                }
                item { SectionLabel(tr("Preferred")) }
                if (state.preferred.isEmpty()) {
                    item { Text(tr("No dictionaries enabled yet."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
                }
                itemsIndexed(state.preferred, key = { index, entry -> entry.key("on", index) }) { _, entry -> Column(itemMotion()) {
                    DictionaryRow(entry.name, entry.url, tint = AppAccents.green, icon = Icons.Default.Close, iconDescription = tr("Disable")) { viewModel.disable(entry) }
                }}
                item { SectionLabel(tr("All resources"), topPadding = AppSpacing.xl) }
                itemsIndexed(state.available, key = { index, entry -> entry.key("off", index) }) { _, entry -> Column(itemMotion()) {
                    DictionaryRow(entry.name, entry.url, tint = null, icon = Icons.Default.Add, iconDescription = tr("Enable")) { viewModel.enable(entry) }
                }}
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, topPadding: androidx.compose.ui.unit.Dp = AppSpacing.xs) {
    Text(text, Modifier.padding(top = topPadding, bottom = AppSpacing.s), style = AppText.smallTitle)
}

@Composable
private fun DictionaryRow(name: String, url: String, tint: Color?, icon: ImageVector, iconDescription: String, onAction: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val host = url.substringAfter("://").substringBefore("/").removePrefix("www.")
    // Several stored dictionaries can share a host, so the path tells them apart.
    val path = url.substringAfter("://").substringAfter("/", "").substringBefore(LanguageDictionary.LOOKUP_PLACEHOLDER).substringBefore("###").trimEnd('/', '?', '=', '&')
    val detail = if (name.equals(host, ignoreCase = true)) path.takeIf { it.isNotBlank() }?.let { "/$it" } else host
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Column(
            Modifier.weight(1f).clip(AppShapes.panel)
                .background(if (tint != null) tint.copy(alpha = AppAlpha.tint) else colors.surfaceVariant.copy(alpha = AppAlpha.muted))
                .padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
        ) {
            Text(name, style = AppText.emphasis, color = tint ?: colors.onSurface)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AppIconButton(icon, iconDescription, onAction, size = IconButtonSize.Large, style = IconButtonStyle.Tonal, tint = colors.onSurface, shape = AppShapes.large)
    }
}
