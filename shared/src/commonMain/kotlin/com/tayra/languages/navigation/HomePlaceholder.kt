package com.tayra.languages.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.navigation.Route

@Composable
internal fun HomePlaceholder(onNavigate: (Route) -> Unit) {
    Scaffold(topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate) }) { padding ->
        Text("Books coming soon.", Modifier.padding(padding).padding(16.dp))
    }
}
