package com.idiomiko.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.idiomiko.core.ui.components.AccountAvatar
import com.idiomiko.core.ui.components.AppBottomSheet
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.CameraIcon
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.PhotoCropDialog
import com.idiomiko.core.ui.components.PhotoLibraryIcon
import com.idiomiko.core.ui.files.rememberCameraCapture
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.media.decodePhoto
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The profile's picture, with a camera on it: it, or Change photo under it, opens the platform's photo picker (on a
 * phone, after asking whether to take a photo instead) and the photo is cropped into the circle before it is saved.
 * Remove photo shows when the picture is an uploaded one.
 */
@Composable
internal fun ProfilePicture(state: AccountUiState, actions: AccountViewModel?, name: String?, email: String, picture: ByteArray?, size: Dp, letterStyle: TextStyle) {
    val scope = rememberCoroutineScope()
    var chosen by remember { mutableStateOf<ImageBitmap?>(null) }
    var unreadable by remember { mutableStateOf(false) }
    var choosingSource by remember { mutableStateOf(false) }
    val busy = state.busy != null
    val crop: suspend (ByteArray) -> Unit = { bytes ->
        val photo = withContext(Dispatchers.Default) { runCatching { decodePhoto(bytes) }.getOrNull() }
        unreadable = photo == null
        chosen = photo
    }
    val picker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        if (file != null) scope.launch { crop(file.readBytes()) }
    }
    val camera = rememberCameraCapture { _, bytes -> scope.launch { crop(bytes) } }
    // Where there is a camera the photo can be taken as well as chosen, so the choice comes first.
    val choose = { unreadable = false; actions?.problemSeen(); if (camera != null) choosingSource = true else picker.launch() }
    // Saved, the dialog goes; a failed save keeps it open, saying why.
    LaunchedEffect(state.done) { if (state.done == AccountDone.PICTURE_SAVED) chosen = null }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Box {
            Box(Modifier.clip(AppShapes.circle).clickable(enabled = !busy, onClickLabel = tr("Change photo"), role = Role.Button, onClick = choose).testTag("account-change-picture")) {
                AccountAvatar(name, email, picture, size, Modifier.testTag("account-card-avatar"), letterStyle = letterStyle)
            }
            Box(
                Modifier.align(Alignment.BottomEnd).size(AppSizes.iconXLarge).clip(AppShapes.circle).background(MaterialTheme.colorScheme.surface)
                    .border(AppBorders.hairline(), AppShapes.circle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(CameraIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppSizes.iconSmall))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
            LinkText(tr("Change photo"), onClick = choose, icon = CameraIcon, style = MaterialTheme.typography.bodyMedium, enabled = !busy, modifier = Modifier.testTag("account-change-photo"))
            if (state.account?.ownPicture == true) {
                LinkText(tr("Remove photo"), onClick = { actions?.removePicture() }, style = MaterialTheme.typography.bodyMedium, enabled = !busy, modifier = Modifier.testTag("account-remove-photo"))
            }
        }
        if (unreadable) Text(tr("That picture could not be opened. Choose a PNG or JPEG photo."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
    if (choosingSource) {
        PhotoSourceSheet(
            onGallery = { choosingSource = false; picker.launch() },
            onCamera = { choosingSource = false; camera?.invoke() },
            onDismiss = { choosingSource = false },
        )
    }
    chosen?.let { photo ->
        PhotoCropDialog(
            photo,
            saving = state.busy == AccountTask.PICTURE,
            problem = state.problem?.let { problemTitle(it, state.problemText) },
            onCancel = { chosen = null; actions?.problemSeen() },
            onSave = { actions?.uploadPicture(it) },
        )
    }
}

/** Where a new photo comes from on a phone: the gallery, or the camera. */
@Composable
internal fun PhotoSourceSheet(onGallery: () -> Unit, onCamera: () -> Unit, onDismiss: () -> Unit) {
    AppBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("photo-source-sheet")) {
        Column(Modifier.fillMaxWidth().padding(bottom = AppSpacing.l)) {
            Text(tr("Change photo"), Modifier.padding(horizontal = AppSpacing.xl, vertical = AppSpacing.s), style = AppText.headline)
            PhotoSourceRow(tr("Choose from gallery"), PhotoLibraryIcon, onGallery, Modifier.testTag("photo-source-gallery"))
            AppDivider(Modifier.padding(horizontal = AppSpacing.xl))
            PhotoSourceRow(tr("Take photo"), CameraIcon, onCamera, Modifier.testTag("photo-source-camera"))
        }
    }
}

@Composable
private fun PhotoSourceRow(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = AppSpacing.xl, vertical = AppSpacing.l),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppSizes.iconMedium))
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}
