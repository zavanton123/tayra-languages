package com.idiomiko.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppMenu
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.LocalVideosEnabled
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route

/**
 * One way to bring something new in to read: a text pasted or from a file, a web page where the app can
 * read one, or a YouTube video while videos are on. Each opens the import it already has.
 */
@Composable
internal fun QuickAddButton(readsWebPages: Boolean, onNavigate: (Route) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = tr("Add")
    // The menu anchors to its enclosing composable, so the button and the menu share a Box.
    Box {
        if (LocalWindowWidth.current.isCompact) {
            AppIconButton(Icons.Default.Add, contentDescription = label, onClick = { open = true }, modifier = Modifier.testTag("quick-add"))
        } else {
            QuietButton(label, onClick = { open = true }, modifier = Modifier.testTag("quick-add"), icon = Icons.Default.Add)
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            AppMenuItem(text = { Text(tr("Paste or import text")) }, onClick = { open = false; onNavigate(Route.NewBook()) }, modifier = Modifier.testTag("quick-add-text"))
            if (readsWebPages) AppMenuItem(text = { Text(tr("Web page")) }, onClick = { open = false; onNavigate(Route.NewBook(fromWeb = true)) }, modifier = Modifier.testTag("quick-add-web"))
            if (LocalVideosEnabled.current) AppMenuItem(text = { Text(tr("YouTube video")) }, onClick = { open = false; onNavigate(Route.NewVideo) }, modifier = Modifier.testTag("quick-add-video"))
        }
    }
}
