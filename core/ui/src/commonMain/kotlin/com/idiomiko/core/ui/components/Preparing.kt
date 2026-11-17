package com.idiomiko.core.ui.components

import androidx.compose.runtime.compositionLocalOf
import com.idiomiko.core.domain.service.SetupKind

/** What the language being learned is still getting in the background (LanguageAssets), for a feature to say it is getting ready; provided at the root of the app. */
val LocalPreparing = compositionLocalOf<Set<SetupKind>> { emptySet() }
