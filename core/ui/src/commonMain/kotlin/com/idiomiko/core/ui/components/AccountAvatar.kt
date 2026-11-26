package com.idiomiko.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppText
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * The account signed in to, for the bar: its [name] or [email], its [picture] (an encoded image) when it has one,
 * signing this device out, and opening the account page.
 */
class AccountBadge(val email: String, val name: String?, val picture: ByteArray?, val signOut: () -> Unit = {}, val open: () -> Unit)

/** Provided at the root of the app; null when signed out or where the build has no account service. */
val LocalAccountBadge = compositionLocalOf<AccountBadge?> { null }

/** The account's [picture] in a circle, or the first letter of its [name] or [email] where it has none or it cannot be read. */
@Composable
fun AccountAvatar(name: String?, email: String, picture: ByteArray?, size: Dp, modifier: Modifier = Modifier, letterStyle: TextStyle = AppText.labelStrong) {
    val image = remember(picture) { picture?.let { runCatching<ImageBitmap> { it.decodeToImageBitmap() }.getOrNull() } }
    Box(modifier.size(size).clip(AppShapes.circle).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        if (image != null) Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(initial(name, email), style = letterStyle, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

internal fun initial(name: String?, email: String): String =
    (name?.trim()?.takeIf { it.isNotEmpty() } ?: email.trim()).firstOrNull()?.uppercase() ?: "?"
