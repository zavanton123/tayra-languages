package com.tayra.languages.core.ui.files

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import io.github.vinceglb.filekit.PlatformFile
import java.awt.datatransfer.DataFlavor
import java.io.File

actual val acceptsDroppedFiles: Boolean = true

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.acceptsDroppedFiles(enabled: Boolean, onHover: (Boolean) -> Unit, onFiles: (List<PlatformFile>) -> Unit): Modifier {
    if (!enabled) return this
    return dragAndDropTarget(
        shouldStartDragAndDrop = { event -> event.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) },
        target = object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) = onHover(true)
            override fun onExited(event: DragAndDropEvent) = onHover(false)
            override fun onEnded(event: DragAndDropEvent) = onHover(false)
            override fun onDrop(event: DragAndDropEvent): Boolean {
                onHover(false)
                val files = (event.awtTransferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>).orEmpty()
                    .filterIsInstance<File>().filter { it.isFile }.map { PlatformFile(it) }
                if (files.isEmpty()) return false
                onFiles(files)
                return true
            }
        },
    )
}
