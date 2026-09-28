package com.tayra.languages.feature.languages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route
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
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, section = NavSection.SETTINGS, onBack = if (compact) onBack else null) },
        snackbarHost = {
            state.message?.let { Snackbar(action = { TextButton(onClick = viewModel::dismissMessage) { Text("OK") } }) { Text(it) } }
        },
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.fillMaxSize().widthIn(max = 760.dp),
                contentPadding = PaddingValues(horizontal = if (compact) 16.dp else 32.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!compact) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).clickable(onClick = onBack),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp)) }
                            Spacer(Modifier.width(20.dp))
                        }
                        Icon(AppIcons.Book, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Dictionaries", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            val pair = listOfNotNull(state.source?.name, state.target?.name).joinToString(" \u2192 ")
                            Text("$pair \u00b7 changes apply immediately", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                }
                item { SectionLabel("Preferred") }
                if (state.preferred.isEmpty()) {
                    item { Text("No dictionaries enabled yet.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
                }
                itemsIndexed(state.preferred, key = { index, entry -> entry.key("on", index) }) { _, entry ->
                    DictionaryRow(entry.name, entry.url, tint = Color(0xFF1FA463), icon = Icons.Default.Close, iconDescription = "Disable") { viewModel.disable(entry) }
                }
                item { SectionLabel("All resources", topPadding = 20.dp) }
                itemsIndexed(state.available, key = { index, entry -> entry.key("off", index) }) { _, entry ->
                    DictionaryRow(entry.name, entry.url, tint = null, icon = Icons.Default.Add, iconDescription = "Enable") { viewModel.enable(entry) }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, topPadding: androidx.compose.ui.unit.Dp = 4.dp) {
    Text(text, Modifier.padding(top = topPadding, bottom = 6.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun DictionaryRow(name: String, url: String, tint: Color?, icon: ImageVector, iconDescription: String, onAction: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val host = url.substringAfter("://").substringBefore("/").removePrefix("www.")
    // Several stored dictionaries can share a host, so the path tells them apart.
    val path = url.substringAfter("://").substringAfter("/", "").substringBefore("[LUTE]").substringBefore("###").trimEnd('/', '?', '=', '&')
    val detail = if (name.equals(host, ignoreCase = true)) path.takeIf { it.isNotBlank() }?.let { "/$it" } else host
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                .background(if (tint != null) tint.copy(alpha = 0.1f) else colors.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(name, style = MaterialTheme.typography.bodyLarge, color = tint ?: colors.onSurface, fontWeight = FontWeight.Medium)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(colors.surfaceVariant).clickable(onClick = onAction),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = iconDescription, modifier = Modifier.size(20.dp)) }
    }
}
