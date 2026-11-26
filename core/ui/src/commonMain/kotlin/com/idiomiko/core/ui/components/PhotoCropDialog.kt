package com.idiomiko.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.materialIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.media.PhotoCrop
import com.idiomiko.core.ui.media.encodeJpeg
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.roundToInt

/** The side, in pixels, of the picture [PhotoCropDialog] makes. */
const val CROPPED_PHOTO_SIZE = 512

/**
 * Fits [photo] into a circle: dragged, pinched, or zoomed with the wheel or the slider. Saving hands [onSave] the square
 * behind the circle as a JPEG [CROPPED_PHOTO_SIZE] pixels a side. While [saving], the buttons wait; [problem] says why
 * the last save failed.
 */
@Composable
fun PhotoCropDialog(photo: ImageBitmap, saving: Boolean, problem: String?, onCancel: () -> Unit, onSave: (ByteArray) -> Unit) {
    var crop by remember(photo) { mutableStateOf(PhotoCrop()) }
    val scope = rememberCoroutineScope()
    AppDialog(
        onDismissRequest = { if (!saving) onCancel() },
        modifier = Modifier.testTag("photo-crop"),
        title = { Text(tr("Your photo")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(tr("Drag to move it, and zoom to fit it in the circle."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                CropArea(photo, crop, onCrop = { crop = it.within(photo) }, Modifier.align(Alignment.CenterHorizontally))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    Text(tr("Zoom"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AppSlider(crop.zoom, { crop = crop.copy(zoom = it).within(photo) }, 1f..PhotoCrop.MAX_ZOOM, Modifier.weight(1f).testTag("photo-zoom"))
                }
                if (problem != null) Text(problem, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            PrimaryButton(
                tr("Save"), loading = saving, modifier = Modifier.testTag("photo-save"),
                onClick = { scope.launch { onSave(withContext(Dispatchers.Default) { crop.cropped(photo, CROPPED_PHOTO_SIZE).encodeJpeg(JPEG_QUALITY) }) } },
            )
        },
        dismissButton = { QuietButton(tr("Cancel"), onClick = onCancel, enabled = !saving) },
    )
}

/** The photo behind a square, dimmed outside the circle it is cut to. */
@Composable
private fun CropArea(photo: ImageBitmap, crop: PhotoCrop, onCrop: (PhotoCrop) -> Unit, modifier: Modifier) {
    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM)
    // The gestures run on for as long as the area is shown: they read the crop as it is now.
    var current by remember { mutableStateOf(crop) }
    current = crop
    Canvas(
        modifier.widthIn(max = CROP_AREA).fillMaxWidth().aspectRatio(1f).clip(AppShapes.panel).testTag("photo-crop-area")
            .pointerInput(photo) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onCrop(current.copy(zoom = current.zoom * zoom, offset = current.offset + pan / size.width.toFloat()))
                }
            }
            .pointerInput(photo) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Scroll) continue
                        val delta = event.changes.sumOf { it.scrollDelta.y.toDouble() }.toFloat()
                        onCrop(current.copy(zoom = current.zoom * exp(-delta * WHEEL_ZOOM)))
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        val side = size.width
        val scale = crop.scale(photo, side)
        val width = photo.width * scale
        val height = photo.height * scale
        drawImage(
            photo, IntOffset.Zero, IntSize(photo.width, photo.height),
            IntOffset((side / 2 + crop.offset.x * side - width / 2).roundToInt(), (side / 2 + crop.offset.y * side - height / 2).roundToInt()),
            IntSize(width.roundToInt(), height.roundToInt()), filterQuality = FilterQuality.High,
        )
        val outside = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addOval(Rect(Offset.Zero, size))
        }
        drawPath(outside, scrim)
    }
}

/** A camera, for changing a picture. */
val CameraIcon: ImageVector by lazy {
    materialIcon(name = "Camera") {
        addPath(
            pathData = addPathNodes("M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4zM9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"),
            fill = SolidColor(AppAccents.ink),
        )
    }
}

private val CROP_AREA = 320.dp
/** How dark the photo is outside the circle. */
private const val SCRIM = 0.55f
private const val JPEG_QUALITY = 92
/** How far one notch of a mouse wheel zooms. */
private const val WHEEL_ZOOM = 0.1f
